package com.twilight.pointquestbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.twilight.pointquestbackend.common.ServiceException;
import com.twilight.pointquestbackend.domain.OrderItem;
import com.twilight.pointquestbackend.domain.Orders;
import com.twilight.pointquestbackend.domain.PointAccount;
import com.twilight.pointquestbackend.domain.PointLedger;
import com.twilight.pointquestbackend.domain.Pool;
import com.twilight.pointquestbackend.domain.PoolDraw;
import com.twilight.pointquestbackend.mapper.OrderItemMapper;
import com.twilight.pointquestbackend.mapper.OrdersMapper;
import com.twilight.pointquestbackend.mapper.PointAccountMapper;
import com.twilight.pointquestbackend.mapper.PointLedgerMapper;
import com.twilight.pointquestbackend.mapper.PoolDrawMapper;
import com.twilight.pointquestbackend.mapper.PoolMapper;
import com.twilight.pointquestbackend.security.UserPrincipal;
import com.twilight.pointquestbackend.vo.MyPointsVO;
import com.twilight.pointquestbackend.vo.OrderDetailVO;
import com.twilight.pointquestbackend.vo.OrderItemVO;
import com.twilight.pointquestbackend.vo.OrderSummaryVO;
import com.twilight.pointquestbackend.vo.PointLedgerVO;
import com.twilight.pointquestbackend.vo.PoolDrawRecordVO;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

    private final PointAccountMapper pointAccountMapper;
    private final PointLedgerMapper pointLedgerMapper;
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final PoolMapper poolMapper;
    private final PoolDrawMapper poolDrawMapper;

    public UserProfileService(PointAccountMapper pointAccountMapper,
                              PointLedgerMapper pointLedgerMapper,
                              OrdersMapper ordersMapper,
                              OrderItemMapper orderItemMapper,
                              PoolMapper poolMapper,
                              PoolDrawMapper poolDrawMapper) {
        this.pointAccountMapper = pointAccountMapper;
        this.pointLedgerMapper = pointLedgerMapper;
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
        this.poolMapper = poolMapper;
        this.poolDrawMapper = poolDrawMapper;
    }

    public MyPointsVO getMyPoints(UserPrincipal principal) {
        UserPrincipal user = requirePrincipal(principal);
        PointAccount account = pointAccountMapper.selectOne(
                new LambdaQueryWrapper<PointAccount>().eq(PointAccount::getUserId, user.getId()));
        if (account == null) {
            throw new ServiceException(500, "point_account_missing");
        }
        MyPointsVO vo = new MyPointsVO();
        vo.setBalance(account.getBalance());
        vo.setUpdatedAt(account.getUpdatedAt());
        return vo;
    }

    public Page<PointLedgerVO> pageMyPointLedger(long page, long size, UserPrincipal principal) {
        UserPrincipal user = requirePrincipal(principal);
        long current = Math.max(1, page);
        long pageSize = Math.min(100, Math.max(1, size));
        Page<PointLedger> ledgerPage = pointLedgerMapper.selectPage(
                new Page<>(current, pageSize),
                new LambdaQueryWrapper<PointLedger>()
                        .eq(PointLedger::getUserId, user.getId())
                        .orderByDesc(PointLedger::getCreatedAt));
        Page<PointLedgerVO> voPage = new Page<>(ledgerPage.getCurrent(), ledgerPage.getSize(), ledgerPage.getTotal());
        voPage.setRecords(ledgerPage.getRecords()
                .stream()
                .map(this::toPointLedgerVO)
                .toList());
        return voPage;
    }

    public Page<OrderSummaryVO> pageMyOrders(long page, long size, UserPrincipal principal) {
        UserPrincipal user = requirePrincipal(principal);
        long current = Math.max(1, page);
        long pageSize = Math.min(100, Math.max(1, size));

        Page<Orders> orderPage = ordersMapper.selectPage(
                new Page<>(current, pageSize),
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getUserId, user.getId())
                        .orderByDesc(Orders::getCreatedAt));

        Map<Long, List<OrderItem>> itemsByOrder = Collections.emptyMap();
        if (!orderPage.getRecords().isEmpty()) {
            List<Long> orderIds = orderPage.getRecords().stream()
                    .map(Orders::getId)
                    .filter(Objects::nonNull)
                    .toList();
            if (!orderIds.isEmpty()) {
                List<OrderItem> items = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));
                itemsByOrder = items.stream()
                        .collect(Collectors.groupingBy(OrderItem::getOrderId, HashMap::new, Collectors.toList()));
            }
        }
        Map<Long, List<OrderItem>> finalItemsByOrder = itemsByOrder;

        Page<OrderSummaryVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        voPage.setRecords(orderPage.getRecords()
                .stream()
                .map(order -> toOrderSummaryVO(order, finalItemsByOrder.get(order.getId())))
                .toList());
        return voPage;
    }

    public OrderDetailVO getMyOrder(String orderNo, UserPrincipal principal) {
        UserPrincipal user = requirePrincipal(principal);
        Orders order = ordersMapper.selectOne(new LambdaQueryWrapper<Orders>().eq(Orders::getOrderNo, orderNo));
        if (order == null) {
            throw new ServiceException(404, "order_not_found");
        }
        if (!user.getId().equals(order.getUserId())) {
            throw new ServiceException(403, "forbidden");
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        return toOrderDetailVO(order, items);
    }

    private OrderSummaryVO toOrderSummaryVO(Orders order, List<OrderItem> items) {
        OrderSummaryVO vo = new OrderSummaryVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalPoints(order.getTotalPoints());
        vo.setStatus(order.getStatus());
        vo.setCreatedAt(order.getCreatedAt());
        int itemCount = 0;
        if (items != null) {
            itemCount = items.stream()
                    .map(OrderItem::getQty)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
        }
        vo.setItemCount(itemCount);
        return vo;
    }

    private OrderDetailVO toOrderDetailVO(Orders order, List<OrderItem> items) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalPoints(order.getTotalPoints());
        vo.setStatus(order.getStatus());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setAddress(order.getAddress());
        List<OrderItemVO> itemVOs = Collections.emptyList();
        if (items != null && !items.isEmpty()) {
            itemVOs = items.stream().map(this::toOrderItemVO).toList();
        }
        vo.setItems(itemVOs);
        return vo;
    }

    private PoolDrawRecordVO toPoolDrawRecordVO(PoolDraw draw, Pool pool) {
        PoolDrawRecordVO vo = new PoolDrawRecordVO();
        vo.setPoolNo(pool == null ? null : pool.getPoolNo());
        vo.setRewardId(draw.getRewardId());
        vo.setRewardNo(draw.getRewardNoSnapshot());
        vo.setRewardName(draw.getRewardNameSnapshot());
        vo.setPointCost(draw.getPointCost());
        vo.setCreatedAt(draw.getCreatedAt());
        return vo;
    }

    public Page<PoolDrawRecordVO> pageMyPoolDraws(long page, long size, String poolNo, UserPrincipal principal) {
        UserPrincipal user = requirePrincipal(principal);
        long current = Math.max(1, page);
        long pageSize = Math.min(100, Math.max(1, size));

        Long poolId = null;
        Map<Long, Pool> poolMap = Collections.emptyMap();
        if (poolNo != null && !poolNo.isBlank()) {
            Pool pool = poolMapper.selectOne(new LambdaQueryWrapper<Pool>().eq(Pool::getPoolNo, poolNo));
            if (pool == null) {
                return new Page<>(current, pageSize, 0);
            }
            poolId = pool.getId();
            poolMap = Map.of(pool.getId(), pool);
        }

        LambdaQueryWrapper<PoolDraw> wrapper = new LambdaQueryWrapper<PoolDraw>()
                .eq(PoolDraw::getUserId, user.getId())
                .orderByDesc(PoolDraw::getCreatedAt);
        if (poolId != null) {
            wrapper.eq(PoolDraw::getPoolId, poolId);
        }

        Page<PoolDraw> drawPage = poolDrawMapper.selectPage(new Page<>(current, pageSize), wrapper);
        if (drawPage.getRecords().isEmpty()) {
            return new Page<>(drawPage.getCurrent(), drawPage.getSize(), drawPage.getTotal());
        }

        if (poolMap.isEmpty()) {
            List<Long> poolIds = drawPage.getRecords().stream()
                    .map(PoolDraw::getPoolId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!poolIds.isEmpty()) {
                List<Pool> pools = poolMapper.selectBatchIds(poolIds);
                poolMap = pools.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(Pool::getId, p -> p));
            }
        }

        Map<Long, Pool> finalPoolMap = poolMap;
        Page<PoolDrawRecordVO> voPage = new Page<>(drawPage.getCurrent(), drawPage.getSize(), drawPage.getTotal());
        voPage.setRecords(drawPage.getRecords()
                .stream()
                .map(draw -> toPoolDrawRecordVO(draw, finalPoolMap.get(draw.getPoolId())))
                .toList());
        return voPage;
    }

    private OrderItemVO toOrderItemVO(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        vo.setRewardId(item.getRewardId());
        vo.setRewardName(item.getRewardNameSnapshot());
        vo.setPointCost(item.getPointCostSnapshot());
        vo.setQuantity(item.getQty());
        return vo;
    }

    private PointLedgerVO toPointLedgerVO(PointLedger ledger) {
        PointLedgerVO vo = new PointLedgerVO();
        vo.setDelta(ledger.getDelta());
        vo.setRefType(ledger.getRefType() == null ? null : ledger.getRefType().toString());
        vo.setRefId(ledger.getRefId());
        vo.setRemark(ledger.getRemark());
        vo.setCreatedAt(ledger.getCreatedAt());
        return vo;
    }

    private UserPrincipal requirePrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new ServiceException(401, "unauthorized");
        }
        return principal;
    }
}
