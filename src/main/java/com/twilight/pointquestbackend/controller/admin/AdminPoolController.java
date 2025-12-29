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

    @GetMapping("/{id}")
    public ApiResponse<PoolVO> getPool(@PathVariable Long id) {
        return ApiResponse.success(poolService.getPool(id));
    }

    @PostMapping
    public ApiResponse<PoolVO> createPool(@Valid @RequestBody PoolRequest request) {
        PoolVO created = poolService.createPool(request);
        return ApiResponse.success("pool_created", created);
    }

    @PutMapping("/{id}")
    public ApiResponse<PoolVO> updatePool(@PathVariable Long id, @Valid @RequestBody PoolRequest request) {
        PoolVO updated = poolService.updatePool(id, request);
        return ApiResponse.success("pool_updated", updated);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePool(@PathVariable Long id) {
        poolService.deletePool(id);
        return ApiResponse.onlySuccess("pool_deleted");
    }
}
