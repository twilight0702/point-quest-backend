package com.twilight.pointquestbackend.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.twilight.pointquestbackend.common.ApiResponse;
import com.twilight.pointquestbackend.dto.PoolRequest;
import com.twilight.pointquestbackend.service.PoolService;
import com.twilight.pointquestbackend.vo.PoolVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/pools")
public class AdminPoolController {

    private final PoolService poolService;

    public AdminPoolController(PoolService poolService) {
        this.poolService = poolService;
    }

    @GetMapping
    public ApiResponse<Page<PoolVO>> pagePools(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String status) {
        Page<PoolVO> result = poolService.pagePools(page, size, status);
        return ApiResponse.success(result);
    }

    @GetMapping("/{poolNo}")
    public ApiResponse<PoolVO> getPool(@PathVariable String poolNo) {
        return ApiResponse.success(poolService.getPoolByNo(poolNo));
    }

    @PostMapping
    public ApiResponse<PoolVO> createPool(@Valid @RequestBody PoolRequest request) {
        PoolVO created = poolService.createPool(request);
        return ApiResponse.success("pool_created", created);
    }

    @PutMapping("/{poolNo}")
    public ApiResponse<PoolVO> updatePool(@PathVariable String poolNo, @Valid @RequestBody PoolRequest request) {
        PoolVO updated = poolService.updatePool(poolNo, request);
        return ApiResponse.success("pool_updated", updated);
    }

    @DeleteMapping("/{poolNo}")
    public ApiResponse<Void> deletePool(@PathVariable String poolNo) {
        poolService.deletePool(poolNo);
        return ApiResponse.onlySuccess("pool_deleted");
    }
}
