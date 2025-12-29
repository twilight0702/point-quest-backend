package com.twilight.pointquestbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.twilight.pointquestbackend.common.ServiceException;
import com.twilight.pointquestbackend.domain.OrderItem;
import com.twilight.pointquestbackend.domain.Orders;
import com.twilight.pointquestbackend.domain.PointAccount;
import com.twilight.pointquestbackend.domain.Reward;
import com.twilight.pointquestbackend.domain.RewardInventory;
import com.twilight.pointquestbackend.dto.CheckoutSubmitRequest;
import com.twilight.pointquestbackend.mapper.OrderItemMapper;
import com.twilight.pointquestbackend.mapper.OrdersMapper;
import com.twilight.pointquestbackend.mapper.PointAccountMapper;
import com.twilight.pointquestbackend.mapper.RewardInventoryMapper;
import com.twilight.pointquestbackend.mapper.RewardMapper;
import com.twilight.pointquestbackend.security.UserPrincipal;
import com.twilight.pointquestbackend.vo.CheckoutResultVO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
public class CheckoutService {

    private final CartService cartService;
    private final RewardMapper rewardMapper;
    private final RewardInventoryMapper rewardInventoryMapper;
    private final PointAccountMapper pointAccountMapper;
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;

    public CheckoutService(CartService cartService,
                           RewardMapper rewardMapper,
                           RewardInventoryMapper rewardInventoryMapper,
                           PointAccountMapper pointAccountMapper,
                           OrdersMapper ordersMapper,
                           OrderItemMapper orderItemMapper) {
        this.cartService = cartService;
        this.rewardMapper = rewardMapper;
        this.rewardInventoryMapper = rewardInventoryMapper;
        this.pointAccountMapper = pointAccountMapper;
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public CheckoutResultVO submit(CheckoutSubmitRequest request, UserPrincipal principal) {
        Map<Long, Integer> cartEntries = cartService.getCartEntries(principal.getId());
        if (CollectionUtils.isEmpty(cartEntries)) {
            throw new ServiceException(400, "cart_empty");
        }
        List<Reward> rewards = rewardMapper.selectBatchIds(cartEntries.keySet());
        Map<Long, Reward> rewardMap = new HashMap<>();
        for (Reward reward : rewards) {
            rewardMap.put(reward.getId(), reward);
        }
        if (rewardMap.size() != cartEntries.size()) {
            throw new ServiceException(404, "reward_not_found");
        }

        long totalPoints = 0L;
        for (Map.Entry<Long, Integer> entry : cartEntries.entrySet()) {
            Reward reward = rewardMap.get(entry.getKey());
            int qty = entry.getValue();
            if (qty < 1) {
                throw new ServiceException(400, "invalid_quantity");
            }
            if (!"ON".equalsIgnoreCase(reward.getStatus())) {
                throw new ServiceException(409, "reward_unavailable");
            }
            if (reward.getPointCost() == null) {
                throw new ServiceException(500, "reward_cost_missing");
            }
            totalPoints += reward.getPointCost() * (long) qty;
        }

        Map<Long, RewardInventory> inventoryMap = rewardInventoryMapper.selectList(
                        new LambdaQueryWrapper<RewardInventory>().in(RewardInventory::getRewardId, cartEntries.keySet()))
                .stream()
                .collect(HashMap::new, (m, inv) -> m.put(inv.getRewardId(), inv), Map::putAll);
        for (Map.Entry<Long, Integer> entry : cartEntries.entrySet()) {
            RewardInventory inv = inventoryMap.get(entry.getKey());
            int stock = inv == null ? 0 : inv.getStock();
            if (stock < entry.getValue()) {
                throw new ServiceException(409, "stock_not_enough");
            }
        }

        PointAccount account = pointAccountMapper.selectOne(
                new LambdaQueryWrapper<PointAccount>().eq(PointAccount::getUserId, principal.getId()));
        if (account == null) {
            throw new ServiceException(500, "point_account_missing");
        }
        if (account.getBalance() == null || account.getBalance() < totalPoints) {
            throw new ServiceException(409, "point_not_enough");
        }

        // optimistic stock deduction
        for (Map.Entry<Long, Integer> entry : cartEntries.entrySet()) {
            RewardInventory inv = inventoryMap.get(entry.getKey());
            int newStock = (inv == null ? 0 : inv.getStock()) - entry.getValue();
            RewardInventory update = new RewardInventory();
            update.setStock(newStock);
            long currentVersion = inv == null || inv.getVersion() == null ? 0L : inv.getVersion();
            update.setVersion(currentVersion + 1);
            int updated = rewardInventoryMapper.update(update,
                    new LambdaQueryWrapper<RewardInventory>()
                            .eq(RewardInventory::getRewardId, entry.getKey())
                            .eq(RewardInventory::getVersion, currentVersion));
            if (updated != 1) {
                throw new ServiceException(409, "stock_conflict");
            }
        }

        // deduct points
        PointAccount updateAccount = new PointAccount();
        long newBalance = account.getBalance() - totalPoints;
        updateAccount.setBalance(newBalance);
        int accountUpdated = pointAccountMapper.update(updateAccount,
                new LambdaQueryWrapper<PointAccount>()
                        .eq(PointAccount::getId, account.getId())
                        .ge(PointAccount::getBalance, totalPoints));
        if (accountUpdated != 1) {
            throw new ServiceException(409, "point_conflict");
        }

        Orders order = new Orders();
        order.setOrderNo(generateOrderNo());
        order.setUserId(principal.getId());
        order.setTotalPoints(totalPoints);
        order.setAddress(request.getAddress());
        order.setStatus("CREATED");
        ordersMapper.insert(order);
        if (order.getId() == null) {
            throw new ServiceException(500, "order_create_failed");
        }

        for (Map.Entry<Long, Integer> entry : cartEntries.entrySet()) {
            Reward reward = rewardMap.get(entry.getKey());
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setRewardId(reward.getId());
            item.setRewardNameSnapshot(reward.getName());
            item.setPointCostSnapshot(reward.getPointCost());
            item.setQty(entry.getValue());
            orderItemMapper.insert(item);
        }

        cartService.clearCart(principal.getId());

        CheckoutResultVO result = new CheckoutResultVO();
        result.setOrderNo(order.getOrderNo());
        result.setTotalPoints(totalPoints);
        return result;
    }

    private String generateOrderNo() {
        return UUID.randomUUID().toString();
    }
}
