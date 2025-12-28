package com.twilight.pointquestbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.twilight.pointquestbackend.common.ServiceException;
import com.twilight.pointquestbackend.domain.Task;
import com.twilight.pointquestbackend.domain.TaskSubmission;
import com.twilight.pointquestbackend.dto.TaskSubmissionRequest;
import com.twilight.pointquestbackend.mapper.TaskMapper;
import com.twilight.pointquestbackend.mapper.TaskSubmissionMapper;
import com.twilight.pointquestbackend.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;

@Service
public class TaskSubmissionService {

    private static final Set<String> ALLOWED_STATUS = Set.of("PENDING", "APPROVED", "REJECTED");

    private final TaskMapper taskMapper;
    private final TaskSubmissionMapper taskSubmissionMapper;

    public TaskSubmissionService(TaskMapper taskMapper, TaskSubmissionMapper taskSubmissionMapper) {
        this.taskMapper = taskMapper;
        this.taskSubmissionMapper = taskSubmissionMapper;
    }

    public TaskSubmission submit(String taskNo, TaskSubmissionRequest request, UserPrincipal principal) {
        UserPrincipal user = requirePrincipal(principal);
        Task task = taskMapper.selectOne(new LambdaQueryWrapper<Task>().eq(Task::getTaskNo, taskNo));
        if (task == null) {
            throw new ServiceException(404, "task_not_found");
        }
        ensureTaskOpen(task);
        ensureEvidenceProvided(request);

        TaskSubmission submission = new TaskSubmission();
        submission.setTaskId(task.getId());
        submission.setUserId(user.getId());
        submission.setEvidenceUrl(request.getEvidenceUrl());
        submission.setEvidenceText(request.getEvidenceText());
        submission.setStatus("PENDING");

        int inserted = taskSubmissionMapper.insert(submission);
        if (inserted != 1 || submission.getId() == null) {
            throw new ServiceException(500, "failed_to_create_submission");
        }
        return submission;
    }

    public Page<TaskSubmission> pageMySubmissions(long page, long size, String status, UserPrincipal principal) {
        UserPrincipal user = requirePrincipal(principal);
        long current = Math.max(1, page);
        long pageSize = Math.min(100, Math.max(1, size));

        LambdaQueryWrapper<TaskSubmission> wrapper = new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getUserId, user.getId())
                .orderByDesc(TaskSubmission::getCreatedAt);
        if (StringUtils.hasText(status)) {
            wrapper.eq(TaskSubmission::getStatus, normalizeStatus(status));
        }
        return taskSubmissionMapper.selectPage(new Page<>(current, pageSize), wrapper);
    }

    public TaskSubmission getMySubmission(String submissionNo, UserPrincipal principal) {
        UserPrincipal user = requirePrincipal(principal);
        TaskSubmission submission = taskSubmissionMapper.selectOne(new LambdaQueryWrapper<TaskSubmission>().eq(TaskSubmission::getSubmissionNo, submissionNo));
        if (submission == null) {
            throw new ServiceException(404, "submission_not_found");
        }
        if (!user.getId().equals(submission.getUserId())) {
            throw new ServiceException(403, "forbidden");
        }
        return submission;
    }

    private void ensureTaskOpen(Task task) {
        String status = task.getStatus() == null ? null : task.getStatus().toString();
        if (status != null && !"OPEN".equalsIgnoreCase(status)) {
            throw new ServiceException(422, "task_not_open");
        }
    }

    private void ensureEvidenceProvided(TaskSubmissionRequest request) {
        if (!StringUtils.hasText(request.getEvidenceText()) && !StringUtils.hasText(request.getEvidenceUrl())) {
            throw new ServiceException(400, "evidence_required");
        }
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!ALLOWED_STATUS.contains(normalized)) {
            throw new ServiceException(400, "invalid_submission_status");
        }
        return normalized;
    }

    private UserPrincipal requirePrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new ServiceException(401, "unauthorized");
        }
        return principal;
    }
}
