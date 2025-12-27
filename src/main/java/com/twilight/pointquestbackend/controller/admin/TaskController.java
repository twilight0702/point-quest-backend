package com.twilight.pointquestbackend.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.twilight.pointquestbackend.common.ApiResponse;
import com.twilight.pointquestbackend.domain.Task;
import com.twilight.pointquestbackend.dto.TaskRequest;
import com.twilight.pointquestbackend.security.UserPrincipal;
import com.twilight.pointquestbackend.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/admin/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * 分页查询任务
     */
    @GetMapping
    public ApiResponse<Page<Task>> pageTasks(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String status) {
        Page<Task> result = taskService.pageTasks(page, size, status);
        return ApiResponse.success(result);
    }

    /**
     * 创建任务
     */
    @PostMapping
    public ApiResponse<Void> createTask(@Valid @RequestBody TaskRequest request,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        taskService.createTask(request, principal);
        return ApiResponse.onlySuccess("task_created");
    }

    /**
     * 查询任务
     */
    @GetMapping("/{taskNo}")
    public ApiResponse<Task> getTask(@PathVariable String taskNo) {
        Task task = taskService.getTask(taskNo);
        return ApiResponse.success(task);
    }

    /**
     * 修改任务
     */
    @PutMapping("/{taskNo}")
    public ApiResponse<Task> updateTask(@PathVariable String taskNo, @Valid @RequestBody TaskRequest request) {
        Task updated = taskService.updateTask(taskNo, request);
        return ApiResponse.success("task_updated", updated);
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/{taskNo}")
    public ApiResponse<Void> deleteTask(@PathVariable String taskNo) {
        taskService.deleteTask(taskNo);
        return ApiResponse.success("task_deleted", null);
    }
}
