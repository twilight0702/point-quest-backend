package com.twilight.pointquestbackend.controller.web;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.twilight.pointquestbackend.common.ApiResponse;
import com.twilight.pointquestbackend.domain.Task;
import com.twilight.pointquestbackend.domain.TaskSubmission;
import com.twilight.pointquestbackend.dto.TaskSubmissionRequest;
import com.twilight.pointquestbackend.security.UserPrincipal;
import com.twilight.pointquestbackend.service.TaskService;
import com.twilight.pointquestbackend.service.TaskSubmissionService;
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
    public ApiResponse<TaskSubmission> submitTask(@PathVariable String taskNo,
                                                  @Valid @RequestBody TaskSubmissionRequest request,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        TaskSubmission submission = submissionService.submit(taskNo, request, principal);
        return ApiResponse.success("submission_created", submission);
    }

    /**
     * 分页查看本人提交与审核状态
     */
    @GetMapping("/me/submissions")
    public ApiResponse<Page<TaskSubmission>> pageMySubmissions(@RequestParam(defaultValue = "1") long page,
                                                               @RequestParam(defaultValue = "10") long size,
                                                               @RequestParam(required = false) String status,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        Page<TaskSubmission> submissions = submissionService.pageMySubmissions(page, size, status, principal);
        return ApiResponse.success(submissions);
    }

    /**
     * 查看单条提交记录详情
     */
    @GetMapping("/me/submissions/{submissionNo}")
    public ApiResponse<TaskSubmission> getMySubmission(@PathVariable String submissionNo,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        TaskSubmission submission = submissionService.getMySubmission(submissionNo, principal);
        return ApiResponse.success(submission);
    }
}
