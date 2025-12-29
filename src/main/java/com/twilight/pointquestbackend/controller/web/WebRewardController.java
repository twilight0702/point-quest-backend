package com.twilight.pointquestbackend.controller.web;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.twilight.pointquestbackend.common.ApiResponse;
import com.twilight.pointquestbackend.service.RewardService;
import com.twilight.pointquestbackend.vo.RewardVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public reward browse endpoints.
 */
@RestController
@RequestMapping("/api")
public class WebRewardController {

    private final RewardService rewardService;

    public WebRewardController(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    @GetMapping({"/rewards", "/rewards/search"})
    public ApiResponse<Page<RewardVO>> listRewards(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Long categoryId,
                                                   @RequestParam(required = false, name = "onlyInStock") Boolean onlyInStock) {
        Page<RewardVO> result = rewardService.searchVisibleRewards(page, size, keyword, categoryId, onlyInStock);
        return ApiResponse.success(result);
    }

    @GetMapping("/rewards/{rewardNo}")
    public ApiResponse<RewardVO> getReward(@PathVariable String rewardNo) {
        return ApiResponse.success(rewardService.getVisibleReward(rewardNo));
    }
}
