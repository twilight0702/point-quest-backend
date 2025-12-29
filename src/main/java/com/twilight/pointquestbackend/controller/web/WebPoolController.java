package com.twilight.pointquestbackend.controller.web;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.twilight.pointquestbackend.common.ApiResponse;
import com.twilight.pointquestbackend.service.PoolService;
import com.twilight.pointquestbackend.vo.PoolVO;
import com.twilight.pointquestbackend.security.UserPrincipal;
import com.twilight.pointquestbackend.service.PoolDrawService;
import com.twilight.pointquestbackend.vo.PoolDrawResultVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public pool browse endpoints.
 */
@RestController
@RequestMapping("/api")
public class WebPoolController {

    private final PoolService poolService;
    private final PoolDrawService poolDrawService;

    public WebPoolController(PoolService poolService, PoolDrawService poolDrawService) {
        this.poolService = poolService;
        this.poolDrawService = poolDrawService;
    }

    @GetMapping("/pools")
    public ApiResponse<Page<PoolVO>> listPools(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size) {
        Page<PoolVO> result = poolService.pageVisiblePools(page, size);
        return ApiResponse.success(result);
    }

    @GetMapping("/pools/{poolNo}")
    public ApiResponse<PoolVO> getPool(@PathVariable String poolNo) {
        return ApiResponse.success(poolService.getVisiblePool(poolNo));
    }

    @PostMapping("/pools/{poolNo}/draw")
    public ApiResponse<PoolDrawResultVO> draw(@PathVariable String poolNo,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        PoolDrawResultVO result = poolDrawService.draw(poolNo, principal);
        return ApiResponse.success("draw_success", result);
    }
}
