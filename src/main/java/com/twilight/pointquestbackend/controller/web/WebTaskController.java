package com.twilight.pointquestbackend.controller.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.twilight.pointquestbackend.common.ApiResponse;
import com.twilight.pointquestbackend.dto.TaskSubmissionDTO;
import com.twilight.pointquestbackend.security.UserPrincipal;
import com.twilight.pointquestbackend.service.TaskService;
import com.twilight.pointquestbackend.service.TaskSubmissionService;
import com.twilight.pointquestbackend.vo.PageSubmissionsVO;
import com.twilight.pointquestbackend.vo.TaskSubmissionDetailVO;
import com.twilight.pointquestbackend.vo.TaskSubmissionVO;
import jakarta.validation.Valid;
import com.twilight.pointquestbackend.domain.Task;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class WebTaskController {

    private final TaskService taskService;
    private final TaskSubmissionService submissionService;

    public WebTaskController(TaskService taskService, TaskSubmissionService submissionService) {
        this.taskService = taskService;
        this.submissionService = submissionService;
    }

    /**
     * 分页查看任务
     */
    @GetMapping("/tasks")
    public ApiResponse<Page<Task>> pageTasks(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String status) {
        Page<Task> result = taskService.pageTasks(page, size, status);
        return ApiResponse.success(result);
    }

    /**
     * 查看任务详情
     */
    @GetMapping("/tasks/{taskNo}")
    public ApiResponse<Task> getTask(@PathVariable String taskNo) {
        Task task = taskService.getTask(taskNo);
        return ApiResponse.success(task);
    }

    /**
     * 完成某个任务（提交凭证）
     */
    @PostMapping("/tasks/{taskNo}/submissions")
    public ApiResponse<TaskSubmissionVO> submitTask(@PathVariable String taskNo,
                                                    @Valid @RequestBody TaskSubmissionDTO request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        var submission = submissionService.submit(taskNo, request, principal);
        return ApiResponse.success("submission_created", submissionService.toVO(submission));
    }

    /**
     * 分页查看本人提交与审核状态
     */
    @GetMapping("/me/submissions")
    public ApiResponse<PageSubmissionsVO> pageMySubmissions(@RequestParam(defaultValue = "1") long page,
                                                             @RequestParam(defaultValue = "10") long size,
                                                             @RequestParam(required = false) String status,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        Page<PageSubmissionsVO.TaskSubmissionSummaryVO> taskSubmissionSummaryVOPage = submissionService.pageMySubmissions(page, size, status, principal);

        PageSubmissionsVO vo = new PageSubmissionsVO();
        vo.setTotal(taskSubmissionSummaryVOPage.getTotal());
        vo.setSize(taskSubmissionSummaryVOPage.getSize());
        vo.setCurrent(taskSubmissionSummaryVOPage.getCurrent());
        vo.setPages(taskSubmissionSummaryVOPage.getPages());
        vo.setRecords(taskSubmissionSummaryVOPage.getRecords());

        return ApiResponse.success("get_my_page_submission_success",vo);
    }

    /**
     * 查看单条提交记录详情
     */
    @GetMapping("/me/submissions/{submissionNo}")
    public ApiResponse<TaskSubmissionDetailVO> getMySubmission(@PathVariable String submissionNo,
                                                                  @AuthenticationPrincipal UserPrincipal principal) {
        var submission = submissionService.getMySubmission(submissionNo, principal);
        TaskSubmissionDetailVO vo = submissionService.toDetailVO(submission);
        // 计算预览 url
        vo.setEvidencePreviewUrls(submissionService.buildSignedUrls(submissionNo, principal));
        return ApiResponse.success(vo);
    }

    /**
     * 上传凭证文件，返回可复用的 URL 列表
     */
    @PostMapping("/tasks/{submissionNo}/uploads")
    public ApiResponse<Void> uploadEvidence(@PathVariable String submissionNo,
                                                        @RequestPart("files") List<MultipartFile> files,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        submissionService.uploadEvidence(submissionNo, files, principal);
        return ApiResponse.onlySuccess("upload_success");
    }
}
