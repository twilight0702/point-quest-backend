package com.twilight.pointquestbackend.controller.web;

import com.twilight.pointquestbackend.common.ApiResponse;
import com.twilight.pointquestbackend.dto.CheckoutSubmitRequest;
import com.twilight.pointquestbackend.security.UserPrincipal;
import com.twilight.pointquestbackend.service.CheckoutService;
import com.twilight.pointquestbackend.vo.CheckoutResultVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/submit")
    public ApiResponse<CheckoutResultVO> submit(@Valid @RequestBody CheckoutSubmitRequest request,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        CheckoutResultVO result = checkoutService.submit(request, principal);
        return ApiResponse.success("checkout_success", result);
    }
}
