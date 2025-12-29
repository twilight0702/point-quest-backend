package com.twilight.pointquestbackend.controller.web;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.twilight.pointquestbackend.common.ApiResponse;
import com.twilight.pointquestbackend.service.PoolService;
import com.twilight.pointquestbackend.vo.PoolVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    public WebPoolController(PoolService poolService) {
        this.poolService = poolService;
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
}
