package com.twilight.pointquestbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilight.pointquestbackend.common.ServiceException;
import com.twilight.pointquestbackend.common.TaskStatus;
import com.twilight.pointquestbackend.common.TaskSubmissionStatus;
import com.twilight.pointquestbackend.domain.Task;
import com.twilight.pointquestbackend.domain.TaskSubmission;
import com.twilight.pointquestbackend.dto.TaskSubmissionDTO;
import com.twilight.pointquestbackend.mapper.TaskMapper;
import com.twilight.pointquestbackend.mapper.TaskSubmissionMapper;
import com.twilight.pointquestbackend.security.UserPrincipal;
import com.twilight.pointquestbackend.vo.PageSubmissionsVO;
import com.twilight.pointquestbackend.vo.TaskSubmissionDetailVO;
import com.twilight.pointquestbackend.vo.TaskSubmissionVO;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TaskSubmissionService {

    private static final Set<String> ALLOWED_STATUS = Set.of("PENDING", "APPROVED", "REJECTED");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf"
    );

    private final TaskMapper taskMapper;
    private final TaskSubmissionMapper taskSubmissionMapper;
    private final StorageService storageService;

    public TaskSubmissionService(TaskMapper taskMapper,
                                 TaskSubmissionMapper taskSubmissionMapper,
                                 StorageService storageService) {
        this.taskMapper = taskMapper;
        this.taskSubmissionMapper = taskSubmissionMapper;
        this.storageService = storageService;
    }

    public TaskSubmission submit(String taskNo, TaskSubmissionDTO request, UserPrincipal principal) {
        UserPrincipal user = requirePrincipal(principal);
        Task task = taskMapper.selectOne(new LambdaQueryWrapper<Task>().eq(Task::getTaskNo, taskNo));
        if (task == null) {
            throw new ServiceException(404, "task_not_found");
        }
        ensureTaskOpen(task);

        TaskSubmission submission = new TaskSubmission();
        submission.setSubmissionNo(UUID.randomUUID().toString());
        submission.setTaskId(task.getId());
        submission.setUserId(user.getId());
        submission.setEvidenceText(request.getEvidenceText());
        submission.setStatus(TaskSubmissionStatus.PENDING);

        int inserted = taskSubmissionMapper.insert(submission);
        if (inserted != 1 || submission.getId() == null) {
            throw new ServiceException(500, "failed_to_create_submission");
        }
        return submission;
    }

    public Page<PageSubmissionsVO.TaskSubmissionSummaryVO> pageMySubmissions(long page, long size, String status, UserPrincipal principal) {
        UserPrincipal user = requirePrincipal(principal);
        long current = Math.max(1, page);
        long pageSize = Math.min(100, Math.max(1, size));

//        LambdaQueryWrapper<TaskSubmission> wrapper = new LambdaQueryWrapper<TaskSubmission>()
//                .eq(TaskSubmission::getUserId, user.getId())
//                .orderByDesc(TaskSubmission::getCreatedAt);
//        if (StringUtils.hasText(status)) {
//            wrapper.eq(TaskSubmission::getStatus, normalizeStatus(status));
//        }
        return taskSubmissionMapper.pageSubmissions(new Page<>(current, pageSize), user.getId());
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

    public void uploadEvidence(String submissionNo, List<MultipartFile> files, UserPrincipal principal) {
        requirePrincipal(principal);
        if (CollectionUtils.isEmpty(files)) {
            throw new ServiceException(400, "file_empty");
        }
        if (files.size() > 5) {
            throw new ServiceException(400, "too_many_files");
        }
        TaskSubmission submission = taskSubmissionMapper.selectOne(new LambdaQueryWrapper<TaskSubmission>().eq(TaskSubmission::getSubmissionNo, submissionNo));
        if (submission == null) {
            throw new ServiceException(404, "submission_not_found");
        }
        if (submission.getStatus() != TaskSubmissionStatus.PENDING) {
            throw new ServiceException(400, "submission_not_pending");
        }
        if (!submission.getUserId().equals(principal.getId())) {
            throw new ServiceException(403, "forbidden");
        }

        Task task = taskMapper.selectOne(new LambdaQueryWrapper<Task>().eq(Task::getId, submission.getTaskId()));
        if (task == null) {
            throw new ServiceException(404, "task_not_found");
        }
        ensureTaskOpen(task);

        for(MultipartFile file : files) {
            validateFile(file);
            String key = buildObjectKey(principal.getId(), submissionNo, file.getOriginalFilename());
            try {
                storageService.store(key, file.getInputStream(), file.getSize(), file.getContentType());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void ensureTaskOpen(Task task) {
        String status = task.getStatus() == null ? null : task.getStatus().toString();
        if (status != null && !TaskStatus.OPEN.getName().equalsIgnoreCase(status)) {
            throw new ServiceException(422, "task_not_open");
        }
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!ALLOWED_STATUS.contains(normalized)) {
            throw new ServiceException(400, "invalid_submission_status");
        }
        return normalized;
    }

    public TaskSubmissionVO toVO(TaskSubmission submission) {
        TaskSubmissionVO vo = new TaskSubmissionVO();
        vo.setSubmissionNo(submission.getSubmissionNo());
        vo.setStatus(submission.getStatus() == null ? null : submission.getStatus().toString());
        return vo;
    }

    private String writeEvidenceKeys(List<String> urls) {
        if (CollectionUtils.isEmpty(urls)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(urls);
        } catch (JsonProcessingException e) {
            throw new ServiceException(500, "evidence_serialize_failed");
        }
    }

    private List<String> readEvidenceKeys(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(value, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(400, "file_empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ServiceException(400, "file_too_large");
        }
        String contentType = file.getContentType();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ServiceException(400, "unsupported_file_type");
        }
    }

    private String buildObjectKey(Long userId, String submissionNo, String originalFilename) {
        String safeName = "file.bin";
        if (StringUtils.hasText(originalFilename)) {
            // 从原始文件名中提取文件名，防止路径遍历
            String fileName = originalFilename.substring(originalFilename.lastIndexOf('/') + 1);
            fileName = fileName.substring(fileName.lastIndexOf('\\') + 1);
            // 进一步清理文件名，只保留字母、数字、点、连字符和下划线
            fileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
            safeName = fileName;
        }

        String uuid = UUID.randomUUID().toString();
        return "evidence/" + userId + "/" + submissionNo + "/" + uuid + "_" + safeName;
    }


    public List<String> buildSignedUrls(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return List.of();
        }
        List<String> urls = new ArrayList<>(keys.size());
        for (String key : keys) {
            urls.add(storageService.getSignedUrl(key, 3600));
        }
        return urls;
    }

    public List<String> buildSignedUrls(String submissionNo, UserPrincipal user) {
        List<String> keys = storageService.listKeys("evidence/" + user.getId() + "/" + submissionNo);
        return keys.stream()
                .map(k -> storageService.getSignedUrl(k, 3600))
                .toList();
    }

    public UserPrincipal requirePrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new ServiceException(401, "unauthorized");
        }
        return principal;
    }

    public TaskSubmissionDetailVO toDetailVO(TaskSubmission submission) {
        TaskSubmissionDetailVO vo = new TaskSubmissionDetailVO();
        vo.setSubmissionNo(submission.getSubmissionNo());
        vo.setStatus(submission.getStatus() == null ? null : submission.getStatus().toString());
        vo.setTaskNo(taskMapper.selectById(submission.getTaskId()).getTaskNo()); // Fixed method call

        return vo;
    }

    public record UploadResult(String submissionNo, List<String> objectKeys) {
    }
}
