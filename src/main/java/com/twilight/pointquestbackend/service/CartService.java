package com.twilight.pointquestbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.twilight.pointquestbackend.common.ServiceException;
import com.twilight.pointquestbackend.domain.Reward;
import com.twilight.pointquestbackend.domain.RewardInventory;
import com.twilight.pointquestbackend.dto.AddCartItemRequest;
import com.twilight.pointquestbackend.security.UserPrincipal;
import com.twilight.pointquestbackend.vo.CartItemVO;
import com.twilight.pointquestbackend.vo.CartVO;
import com.twilight.pointquestbackend.mapper.RewardInventoryMapper;
import com.twilight.pointquestbackend.mapper.RewardMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class CartService {

    private static final Duration CART_TTL = Duration.ofHours(6);

    private final StringRedisTemplate redisTemplate;
    private final RewardMapper rewardMapper;
    private final RewardInventoryMapper rewardInventoryMapper;

    public CartService(StringRedisTemplate redisTemplate,
                       RewardMapper rewardMapper,
                       RewardInventoryMapper rewardInventoryMapper) {
        this.redisTemplate = redisTemplate;
        this.rewardMapper = rewardMapper;
        this.rewardInventoryMapper = rewardInventoryMapper;
    }

    public CartVO getCart(UserPrincipal principal) {
        return buildCart(principal.getId());
    }

    public Map<Long, Integer> getCartEntries(Long userId) {
        return loadCartEntries(cartKey(userId));
    }

    public CartVO addItem(AddCartItemRequest request, UserPrincipal principal) {
        int qty = validateQuantity(request.getQuantity());
        Reward reward = ensureRewardAvailable(request.getRewardId());
        int stock = currentStock(reward.getId());

        String key = cartKey(principal.getId());
        Map<Long, Integer> cartEntries = loadCartEntries(key);
        int newQty = qty + cartEntries.getOrDefault(reward.getId(), 0);
        assertStockEnough(stock, newQty);

        persistQuantity(key, reward.getId(), newQty);
        return buildCart(principal.getId());
    }

    public CartVO updateItem(Long rewardId, Integer quantity, UserPrincipal principal) {
        int qty = validateQuantity(quantity);
        Reward reward = ensureRewardAvailable(rewardId);
        int stock = currentStock(reward.getId());
        assertStockEnough(stock, qty);

        String key = cartKey(principal.getId());
        persistQuantity(key, rewardId, qty);
        return buildCart(principal.getId());
    }

    public void removeItem(Long rewardId, UserPrincipal principal) {
        String key = cartKey(principal.getId());
        redisTemplate.opsForHash().delete(key, rewardId.toString());
    }

    public void clearCart(UserPrincipal principal) {
        redisTemplate.delete(cartKey(principal.getId()));
    }

    public void clearCart(Long userId) {
        redisTemplate.delete(cartKey(userId));
    }

    private CartVO buildCart(Long userId) {
        String key = cartKey(userId);
        Map<Long, Integer> cartEntries = loadCartEntries(key);
        if (cartEntries.isEmpty()) {
            CartVO empty = new CartVO();
            empty.setItems(List.of());
            empty.setTotalPoints(0L);
            empty.setTotalQuantity(0);
            return empty;
        }
        Set<Long> rewardIds = new HashSet<>(cartEntries.keySet());
        List<Reward> rewards = rewardMapper.selectBatchIds(rewardIds);
        Map<Long, Reward> rewardMap = rewards.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Reward::getId, r -> r));
        // Clean up missing rewards from cart
        for (Long rewardId : rewardIds) {
            if (!rewardMap.containsKey(rewardId)) {
                redisTemplate.opsForHash().delete(key, rewardId.toString());
            }
        }

        Map<Long, RewardInventory> inventoryMap = CollectionUtils.isEmpty(rewardMap)
                ? Map.of()
                : rewardInventoryMapper.selectList(
                                new LambdaQueryWrapper<RewardInventory>().in(RewardInventory::getRewardId, rewardMap.keySet()))
                        .stream()
                        .collect(Collectors.toMap(RewardInventory::getRewardId, inv -> inv));

        long totalPoints = 0;
        int totalQuantity = 0;
        List<CartItemVO> items = new java.util.ArrayList<>(rewardMap.size());
        for (Map.Entry<Long, Integer> entry : cartEntries.entrySet()) {
            Reward reward = rewardMap.get(entry.getKey());
            if (reward == null) {
                continue;
            }
            int qty = entry.getValue();
            RewardInventory inv = inventoryMap.get(reward.getId());
            int stock = inv == null ? 0 : inv.getStock();
            long linePoints = (reward.getPointCost() == null ? 0 : reward.getPointCost()) * qty;
            boolean available = "ON".equalsIgnoreCase(reward.getStatus()) && stock >= qty && stock > 0;

            CartItemVO itemVO = new CartItemVO();
            itemVO.setRewardId(reward.getId());
            itemVO.setRewardNo(reward.getRewardNo());
            itemVO.setName(reward.getName());
            itemVO.setPointCost(reward.getPointCost());
            itemVO.setQuantity(qty);
            itemVO.setStock(stock);
            itemVO.setStatus(reward.getStatus());
            itemVO.setAvailable(available);
            itemVO.setLinePoints(linePoints);
            items.add(itemVO);

            totalPoints += linePoints;
            totalQuantity += qty;
        }

        CartVO cartVO = new CartVO();
        cartVO.setItems(items);
        cartVO.setTotalPoints(totalPoints);
        cartVO.setTotalQuantity(totalQuantity);
        return cartVO;
    }

    private Map<Long, Integer> loadCartEntries(String key) {
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(key);
        Map<Long, Integer> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : raw.entrySet()) {
            try {
                Long rewardId = Long.parseLong(String.valueOf(entry.getKey()));
                Integer qty = Integer.parseInt(String.valueOf(entry.getValue()));
                result.put(rewardId, qty);
            } catch (NumberFormatException ignored) {
                // skip malformed data
            }
        }
        return result;
    }

    private Reward ensureRewardAvailable(Long rewardId) {
        if (rewardId == null) {
            throw new ServiceException(400, "reward_id_required");
        }
        Reward reward = rewardMapper.selectById(rewardId);
        if (reward == null) {
            throw new ServiceException(404, "reward_not_found");
        }
        if (!"ON".equalsIgnoreCase(reward.getStatus())) {
            throw new ServiceException(409, "reward_unavailable");
        }
        return reward;
    }

    private int currentStock(Long rewardId) {
        RewardInventory inventory = rewardInventoryMapper.selectById(rewardId);
        return inventory == null ? 0 : inventory.getStock();
    }

    private int validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > 99) {
            throw new ServiceException(400, "invalid_quantity");
        }
        return quantity;
    }

    private void assertStockEnough(int stock, int requestedQty) {
        if (stock <= 0 || requestedQty > stock) {
            throw new ServiceException(409, "stock_not_enough");
        }
    }

    private void persistQuantity(String key, Long rewardId, int quantity) {
        redisTemplate.opsForHash().put(key, rewardId.toString(), String.valueOf(quantity));
        redisTemplate.expire(key, CART_TTL);
    }

    private String cartKey(Long userId) {
        return "cart:" + userId;
    }
}
