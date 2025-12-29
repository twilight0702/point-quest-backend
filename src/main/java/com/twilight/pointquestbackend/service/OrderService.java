package com.twilight.pointquestbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.twilight.pointquestbackend.common.OrderStatus;
import com.twilight.pointquestbackend.common.ServiceException;
import com.twilight.pointquestbackend.domain.OrderItem;
import com.twilight.pointquestbackend.domain.Orders;
import com.twilight.pointquestbackend.mapper.OrderItemMapper;
import com.twilight.pointquestbackend.mapper.OrdersMapper;
import com.twilight.pointquestbackend.vo.AdminOrderDetailVO;
import com.twilight.pointquestbackend.vo.AdminOrderSummaryVO;
import com.twilight.pointquestbackend.vo.OrderItemVO;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderService {

    private static final Set<String> ALLOWED_STATUS = Set.of(
            OrderStatus.CREATED.name(),
            OrderStatus.PROCESSING.name(),
            OrderStatus.SHIPPED.name(),
            OrderStatus.COMPLETED.name(),
            OrderStatus.CANCELLED.name()
    );

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;

    public OrderService(OrdersMapper ordersMapper, OrderItemMapper orderItemMapper) {
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
    }

    public Page<AdminOrderSummaryVO> pageOrders(long page, long size, String status, Long userId) {
        long current = Math.max(1, page);
        long pageSize = Math.min(100, Math.max(1, size));
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Orders::getUserId, userId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Orders::getStatus, normalizeStatus(status));
        }
        wrapper.orderByDesc(Orders::getCreatedAt);

        Page<Orders> orderPage = ordersMapper.selectPage(new Page<>(current, pageSize), wrapper);
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
        Page<AdminOrderSummaryVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        voPage.setRecords(orderPage.getRecords()
                .stream()
                .map(order -> toSummaryVO(order, finalItemsByOrder.get(order.getId())))
                .toList());
        return voPage;
    }

    public AdminOrderDetailVO getOrder(String orderNo) {
        Orders order = ordersMapper.selectOne(new LambdaQueryWrapper<Orders>().eq(Orders::getOrderNo, orderNo));
        if (order == null) {
            throw new ServiceException(404, "order_not_found");
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        return toDetailVO(order, items);
    }

    public AdminOrderDetailVO updateOrderStatus(String orderNo, String status) {
        Orders order = ordersMapper.selectOne(new LambdaQueryWrapper<Orders>().eq(Orders::getOrderNo, orderNo));
        if (order == null) {
            throw new ServiceException(404, "order_not_found");
        }
        String normalized = normalizeStatus(status);
        if (!normalized.equals(order.getStatus())) {
            Orders update = new Orders();
            update.setId(order.getId());
            update.setStatus(normalized);
            int updated = ordersMapper.updateById(update);
            if (updated != 1) {
                throw new ServiceException(500, "order_update_failed");
            }
            order.setStatus(normalized);
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        return toDetailVO(order, items);
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new ServiceException(400, "order_status_required");
        }
        String normalized = status.trim().toUpperCase();
        if (!ALLOWED_STATUS.contains(normalized)) {
            throw new ServiceException(400, "invalid_order_status");
        }
        return normalized;
    }

    private AdminOrderSummaryVO toSummaryVO(Orders order, List<OrderItem> items) {
        AdminOrderSummaryVO vo = new AdminOrderSummaryVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalPoints(order.getTotalPoints());
        vo.setStatus(order.getStatus());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setItemCount(calculateItemCount(items));
        return vo;
    }

    private AdminOrderDetailVO toDetailVO(Orders order, List<OrderItem> items) {
        AdminOrderDetailVO vo = new AdminOrderDetailVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalPoints(order.getTotalPoints());
        vo.setStatus(order.getStatus());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setAddress(order.getAddress());
        vo.setItems(toItemVOs(items));
        return vo;
    }

    private List<OrderItemVO> toItemVOs(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream().map(this::toItemVO).toList();
    }

    private OrderItemVO toItemVO(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        vo.setRewardId(item.getRewardId());
        vo.setRewardName(item.getRewardNameSnapshot());
        vo.setPointCost(item.getPointCostSnapshot());
        vo.setQuantity(item.getQty());
        return vo;
    }

    private int calculateItemCount(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        return items.stream()
                .map(OrderItem::getQty)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }
}
