package com.twilight.pointquestbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.twilight.pointquestbackend.common.ServiceException;
import com.twilight.pointquestbackend.common.TaskStatus;
import com.twilight.pointquestbackend.domain.Task;
import com.twilight.pointquestbackend.dto.TaskRequest;
import com.twilight.pointquestbackend.mapper.TaskMapper;
import com.twilight.pointquestbackend.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Service
public class TaskService {

    private static final Set<String> ALLOWED_STATUS = Set.of("OPEN", "CLOSED", "ENDED");

    private final TaskMapper taskMapper;

    public TaskService(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public Page<Task> pageTasks(long page, long size, String status) {
        long current = Math.max(1, page);
        long pageSize = Math.min(100, Math.max(1, size));
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            String normalized = normalizeStatus(status);
            wrapper.eq(Task::getStatus, normalized);
        }
        wrapper.orderByDesc(Task::getCreatedAt);
        return taskMapper.selectPage(new Page<>(current, pageSize), wrapper);
    }

    public void createTask(TaskRequest request, UserPrincipal principal) {
        UserPrincipal operator = requirePrincipal(principal);
        Task task = new Task();
        applyRequestToTask(task, request, true);
        task.setCreatedBy(operator.getId());
        task.setCreatedUserType(operator.getRole());
        int inserted = taskMapper.insert(task);
        if (inserted != 1 || task.getId() == null) {
            throw new ServiceException(500, "failed_to_create_task");
        }
    }

    public Task getTask(String uuid) {
        Task task = taskMapper.selectOne(new LambdaQueryWrapper<Task>().eq(Task::getTaskNo, uuid));
        if (task == null) {
            throw new ServiceException(404, "task_not_found");
        }
        return task;
    }

    public Task updateTask(String uuid, TaskRequest request) {
        Task existing = taskMapper.selectOne(new  LambdaQueryWrapper<Task>().eq(Task::getTaskNo, uuid));
        if (existing == null) {
            throw new ServiceException(404, "task_not_found");
        }
        applyRequestToTask(existing, request, false);
        taskMapper.updateById(existing);
        return existing;
    }

    public void deleteTask(String uuid) {
        Task existing = taskMapper.selectOne(new LambdaQueryWrapper<Task>().eq(Task::getTaskNo, uuid));
        if (existing == null) {
            throw new ServiceException(404, "task_not_found");
        }
        taskMapper.deleteById(existing.getId());
    }

    private void applyRequestToTask(Task task, TaskRequest request, boolean isCreate) {
        task.setTaskNo(UUID.randomUUID().toString());
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPointReward(request.getPointReward());
        task.setDeadline(request.getDeadline());
        String currentStatus = stringStatus(task.getStatus());
        String resolvedStatus = resolveStatus(request.getStatus(), isCreate ? "OPEN" : currentStatus);
        task.setStatus(TaskStatus.valueOf(resolvedStatus));
    }

    private String resolveStatus(String requested, String fallback) {
        if (StringUtils.hasText(requested)) {
            return normalizeStatus(requested);
        }
        if (fallback != null) {
            return normalizeStatus(fallback);
        }
        return "OPEN";
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!ALLOWED_STATUS.contains(normalized)) {
            throw new ServiceException(400, "invalid_task_status");
        }
        return normalized;
    }

    private String stringStatus(Object status) {
        return status == null ? null : status.toString();
    }

    private UserPrincipal requirePrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new ServiceException(401, "unauthorized");
        }
        return principal;
    }
}
