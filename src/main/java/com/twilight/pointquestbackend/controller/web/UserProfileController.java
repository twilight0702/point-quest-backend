package com.twilight.pointquestbackend.controller.web;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.twilight.pointquestbackend.common.ApiResponse;
import com.twilight.pointquestbackend.security.UserPrincipal;
import com.twilight.pointquestbackend.service.UserProfileService;
import com.twilight.pointquestbackend.vo.MyPointsVO;
import com.twilight.pointquestbackend.vo.OrderDetailVO;
import com.twilight.pointquestbackend.vo.OrderSummaryVO;
import com.twilight.pointquestbackend.vo.PointLedgerVO;
import com.twilight.pointquestbackend.vo.PoolDrawRecordVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * 查看当前用户积分账户
     */
    @GetMapping("/me/points")
    public ApiResponse<MyPointsVO> getMyPoints(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(userProfileService.getMyPoints(principal));
    }

    /**
     * 分页查看积分流水
     */
    @GetMapping("/me/point-ledger")
    public ApiResponse<Page<PointLedgerVO>> pageMyPointLedger(@RequestParam(defaultValue = "1") long page,
                                                              @RequestParam(defaultValue = "10") long size,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        Page<PointLedgerVO> voPage = userProfileService.pageMyPointLedger(page, size, principal);
        return ApiResponse.success(voPage);
    }

    /**
     * 分页查看抽卡记录
     */
    @GetMapping("/me/pool-draws")
    public ApiResponse<Page<PoolDrawRecordVO>> pageMyPoolDraws(@RequestParam(defaultValue = "1") long page,
                                                               @RequestParam(defaultValue = "10") long size,
                                                               @RequestParam(required = false) String poolNo,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        Page<PoolDrawRecordVO> voPage = userProfileService.pageMyPoolDraws(page, size, poolNo, principal);
        return ApiResponse.success(voPage);
    }

    /**
     * 分页查看我的订单
     */
    @GetMapping("/orders")
    public ApiResponse<Page<OrderSummaryVO>> pageMyOrders(@RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(defaultValue = "10") long size,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        Page<OrderSummaryVO> voPage = userProfileService.pageMyOrders(page, size, principal);
        return ApiResponse.success(voPage);
    }

    /**
     * 查看单个订单详情
     */
    @GetMapping("/orders/{orderNo}")
    public ApiResponse<OrderDetailVO> getMyOrder(@PathVariable String orderNo,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        OrderDetailVO vo = userProfileService.getMyOrder(orderNo, principal);
        return ApiResponse.success(vo);
    }
}
