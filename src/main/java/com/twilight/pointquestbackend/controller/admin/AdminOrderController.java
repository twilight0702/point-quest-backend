package com.twilight.pointquestbackend.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.twilight.pointquestbackend.common.ApiResponse;
import com.twilight.pointquestbackend.dto.OrderStatusUpdateRequest;
import com.twilight.pointquestbackend.service.OrderService;
import com.twilight.pointquestbackend.vo.AdminOrderDetailVO;
import com.twilight.pointquestbackend.vo.AdminOrderSummaryVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<Page<AdminOrderSummaryVO>> pageOrders(@RequestParam(defaultValue = "1") long page,
                                                             @RequestParam(defaultValue = "10") long size,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(required = false) Long userId) {
        Page<AdminOrderSummaryVO> result = orderService.pageOrders(page, size, status, userId);
        return ApiResponse.success(result);
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<AdminOrderDetailVO> getOrder(@PathVariable String orderNo) {
        return ApiResponse.success(orderService.getOrder(orderNo));
    }

    @PutMapping("/{orderNo}/status")
    public ApiResponse<AdminOrderDetailVO> updateOrderStatus(@PathVariable String orderNo,
                                                             @Valid @RequestBody OrderStatusUpdateRequest request) {
        AdminOrderDetailVO updated = orderService.updateOrderStatus(orderNo, request.getStatus());
        return ApiResponse.success("order_status_updated", updated);
    }
}
