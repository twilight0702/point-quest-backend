package com.twilight.pointquestbackend.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.twilight.pointquestbackend.common.ApiResponse;
import com.twilight.pointquestbackend.dto.RewardRequest;
import com.twilight.pointquestbackend.service.RewardService;
import com.twilight.pointquestbackend.vo.AllCategoriesVO;
import com.twilight.pointquestbackend.vo.RewardVO;
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
@RequestMapping("/api/admin/rewards")
public class AdminRewardController {

    private final RewardService rewardService;

    public AdminRewardController(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    @GetMapping
    public ApiResponse<Page<RewardVO>> pageRewards(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String keyword) {
        Page<RewardVO> result = rewardService.pageRewards(page, size, status, keyword);
        return ApiResponse.success(result);
    }

    @GetMapping("/{rewardNo}")
    public ApiResponse<RewardVO> getReward(@PathVariable String rewardNo) {
        return ApiResponse.success(rewardService.getReward(rewardNo));
    }

    @PostMapping
    public ApiResponse<RewardVO> createReward(@Valid @RequestBody RewardRequest request) {
        RewardVO created = rewardService.createReward(request);
        return ApiResponse.success("reward_created", created);
    }

    @PutMapping("/{rewardNo}")
    public ApiResponse<RewardVO> updateReward(@PathVariable String rewardNo,
                                              @Valid @RequestBody RewardRequest request) {
        RewardVO updated = rewardService.updateReward(rewardNo, request);
        return ApiResponse.success("reward_updated", updated);
    }

    @DeleteMapping("/{rewardNo}")
    public ApiResponse<Void> deleteReward(@PathVariable String rewardNo) {
        rewardService.deleteReward(rewardNo);
        return ApiResponse.onlySuccess("reward_deleted");
    }

    @GetMapping("/all-category")
    public ApiResponse<AllCategoriesVO> getAllCategories() {
        AllCategoriesVO allCategoriesVO = rewardService.getAllCategories();
        return ApiResponse.success(allCategoriesVO);
    }

    @PostMapping("/category")
    public ApiResponse<Void> addCategory(@RequestBody String categoryName) {
        rewardService.addCategory(categoryName);
        return ApiResponse.onlySuccess("category_added");
    }

    @PutMapping("/category/{categoryId}")
    public ApiResponse<Void> updateCategory(@PathVariable Long categoryId, @RequestBody String categoryName) {
        rewardService.updateCategory(categoryId, categoryName);
        return ApiResponse.onlySuccess("category_updated");
    }

    @DeleteMapping("/category/{categoryId}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long categoryId) {
        rewardService.deleteCategory(categoryId);
        return ApiResponse.onlySuccess("category_deleted");
    }
}
