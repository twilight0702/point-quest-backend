package com.twilight.pointquestbackend.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.twilight.pointquestbackend.common.ApiResponse;
import com.twilight.pointquestbackend.dto.ApproveSubmissionRequest;
import com.twilight.pointquestbackend.dto.RejectSubmissionRequest;
import com.twilight.pointquestbackend.security.UserPrincipal;
import com.twilight.pointquestbackend.service.SubmissionReviewService;
import com.twilight.pointquestbackend.vo.AdminSubmissionDetailVO;
import com.twilight.pointquestbackend.vo.AdminSubmissionVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/submissions")
public class AdminSubmissionController {

    private final SubmissionReviewService submissionReviewService;

    public AdminSubmissionController(SubmissionReviewService submissionReviewService) {
        this.submissionReviewService = submissionReviewService;
    }

    /**
     * 管理端分页查看提交
     */
    @GetMapping
    public ApiResponse<Page<AdminSubmissionVO>> pageSubmissions(@RequestParam(defaultValue = "1") long page,
                                                                @RequestParam(defaultValue = "10") long size,
                                                                @RequestParam(required = false) String status) {
        Page<AdminSubmissionVO> result = submissionReviewService.pageSubmissions(page, size, status);
        return ApiResponse.success(result);
    }

    /**
     * 查看单个提交详情（含证据链接和审核记录）
     */
    @GetMapping("/{submissionNo}")
    public ApiResponse<AdminSubmissionDetailVO> getSubmissionDetail(@PathVariable String submissionNo) {
        AdminSubmissionDetailVO detail = submissionReviewService.getSubmissionDetail(submissionNo);
        return ApiResponse.success(detail);
    }

    /**
     * 审核通过并发放积分
     */
    @PostMapping("/{submissionNo}/approve")
    public ApiResponse<Void> approve(@PathVariable String submissionNo,
                                     @Valid @RequestBody ApproveSubmissionRequest request,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        submissionReviewService.approve(submissionNo, request, principal);
        return ApiResponse.onlySuccess("submission_approved");
    }

    /**
     * 审核拒绝
     */
    @PostMapping("/{submissionNo}/reject")
    public ApiResponse<Void> reject(@PathVariable String submissionNo,
                                    @Valid @RequestBody RejectSubmissionRequest request,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        submissionReviewService.reject(submissionNo, request, principal);
        return ApiResponse.onlySuccess("submission_rejected");
    }
}
