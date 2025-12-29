package com.twilight.pointquestbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.twilight.pointquestbackend.common.ServiceException;
import com.twilight.pointquestbackend.common.TaskSubmissionStatus;
import com.twilight.pointquestbackend.common.UserType;
import com.twilight.pointquestbackend.domain.Message;
import com.twilight.pointquestbackend.domain.PointAccount;
import com.twilight.pointquestbackend.domain.PointLedger;
import com.twilight.pointquestbackend.domain.SubmissionReview;
import com.twilight.pointquestbackend.domain.Task;
import com.twilight.pointquestbackend.domain.TaskSubmission;
import com.twilight.pointquestbackend.dto.ApproveSubmissionRequest;
import com.twilight.pointquestbackend.dto.RejectSubmissionRequest;
import com.twilight.pointquestbackend.mapper.MessageMapper;
import com.twilight.pointquestbackend.mapper.PointAccountMapper;
import com.twilight.pointquestbackend.mapper.PointLedgerMapper;
import com.twilight.pointquestbackend.mapper.SubmissionReviewMapper;
import com.twilight.pointquestbackend.mapper.TaskMapper;
import com.twilight.pointquestbackend.mapper.TaskSubmissionMapper;
import com.twilight.pointquestbackend.security.UserPrincipal;
import com.twilight.pointquestbackend.vo.AdminSubmissionDetailVO;
import com.twilight.pointquestbackend.vo.AdminSubmissionVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
public class SubmissionReviewService {

    private static final Set<String> ALLOWED_STATUS = Set.of("PENDING", "APPROVED", "REJECTED");

    private final TaskSubmissionMapper taskSubmissionMapper;
    private final TaskMapper taskMapper;
    private final SubmissionReviewMapper submissionReviewMapper;
    private final PointAccountMapper pointAccountMapper;
    private final PointLedgerMapper pointLedgerMapper;
    private final MessageMapper messageMapper;
    private final StorageService storageService;

    public SubmissionReviewService(TaskSubmissionMapper taskSubmissionMapper,
                                   TaskMapper taskMapper,
                                   SubmissionReviewMapper submissionReviewMapper,
                                   PointAccountMapper pointAccountMapper,
                                   PointLedgerMapper pointLedgerMapper,
                                   MessageMapper messageMapper,
                                   StorageService storageService) {
        this.taskSubmissionMapper = taskSubmissionMapper;
        this.taskMapper = taskMapper;
        this.submissionReviewMapper = submissionReviewMapper;
        this.pointAccountMapper = pointAccountMapper;
        this.pointLedgerMapper = pointLedgerMapper;
        this.messageMapper = messageMapper;
        this.storageService = storageService;
    }

    public Page<AdminSubmissionVO> pageSubmissions(long page, long size, String status) {
        long current = Math.max(1, page);
        long pageSize = Math.min(100, Math.max(1, size));
        String normalized = null;
        if (StringUtils.hasText(status)) {
            normalized = normalizeStatus(status);
        }
        return taskSubmissionMapper.pageAdminSubmissions(new Page<>(current, pageSize), normalized);
    }

    public AdminSubmissionDetailVO getSubmissionDetail(String submissionNo) {
        AdminSubmissionDetailVO detail = taskSubmissionMapper.getAdminSubmissionDetail(submissionNo);
        if (detail == null) {
            throw new ServiceException(404, "submission_not_found");
        }
        if (detail.getUserId() != null) {
            detail.setEvidencePreviewUrls(buildSignedUrls(detail.getUserId(), detail.getSubmissionNo()));
        }
        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(String submissionNo, ApproveSubmissionRequest request, UserPrincipal principal) {
        UserPrincipal reviewer = requireAdmin(principal);
        TaskSubmission submission = requirePendingSubmission(submissionNo);
        Task task = requireTask(submission.getTaskId());
        long points = resolvePoints(request, task);

        SubmissionReview review = new SubmissionReview();
        review.setSubmissionId(submission.getId());
        review.setReviewerId(reviewer.getId());
        review.setComment(request.getComment());
        review.setPointsAwarded(points);
        int inserted = submissionReviewMapper.insert(review);
        if (inserted != 1) {
            throw new ServiceException(500, "create_review_failed");
        }

        updateSubmissionStatus(submission, TaskSubmissionStatus.APPROVED);
        increaseUserPoints(submission, points);
        writeLedger(submission, points, "Task submission approved");
        sendMessage(submission.getUserId(),
                "任务提交审核通过",
                String.format("任务 %s 的提交已通过，奖励积分 %d%s",
                        task.getTaskNo(),
                        points,
                        StringUtils.hasText(request.getComment()) ? ("，备注：" + request.getComment()) : ""));
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(String submissionNo, RejectSubmissionRequest request, UserPrincipal principal) {
        UserPrincipal reviewer = requireAdmin(principal);
        TaskSubmission submission = requirePendingSubmission(submissionNo);
        Task task = requireTask(submission.getTaskId());

        SubmissionReview review = new SubmissionReview();
        review.setSubmissionId(submission.getId());
        review.setReviewerId(reviewer.getId());
        review.setComment(request.getComment());
        review.setPointsAwarded(0L);
        int inserted = submissionReviewMapper.insert(review);
        if (inserted != 1) {
            throw new ServiceException(500, "create_review_failed");
        }

        updateSubmissionStatus(submission, TaskSubmissionStatus.REJECTED);
        sendMessage(submission.getUserId(),
                "任务提交审核拒绝",
                String.format("任务 %s 的提交未通过，原因：%s", task.getTaskNo(), request.getComment()));
    }

    private void updateSubmissionStatus(TaskSubmission submission, TaskSubmissionStatus targetStatus) {
        int updated = taskSubmissionMapper.update(null,
                new LambdaUpdateWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getId, submission.getId())
                        .eq(TaskSubmission::getStatus, TaskSubmissionStatus.PENDING)
                        .set(TaskSubmission::getStatus, targetStatus));
        if (updated != 1) {
            throw new ServiceException(400, "submission_not_pending");
        }
    }

    private void increaseUserPoints(TaskSubmission submission, long points) {
        PointAccount account = pointAccountMapper.selectOne(
                new LambdaQueryWrapper<PointAccount>().eq(PointAccount::getUserId, submission.getUserId()));
        if (account == null) {
            throw new ServiceException(500, "point_account_missing");
        }
        account.setBalance(account.getBalance() + points);
        pointAccountMapper.updateById(account);
    }

    private void writeLedger(TaskSubmission submission, long points, String remark) {
        PointLedger ledger = new PointLedger();
        ledger.setUserId(submission.getUserId());
        ledger.setDelta(points);
        ledger.setRefType("SUBMISSION");
        ledger.setRefId(submission.getId());
        ledger.setRemark(remark);
        pointLedgerMapper.insert(ledger);
    }

    private TaskSubmission requirePendingSubmission(String submissionNo) {
        TaskSubmission submission = taskSubmissionMapper.selectOne(
                new LambdaQueryWrapper<TaskSubmission>().eq(TaskSubmission::getSubmissionNo, submissionNo));
        if (submission == null) {
            throw new ServiceException(404, "submission_not_found");
        }
        if (submission.getStatus() != TaskSubmissionStatus.PENDING) {
            throw new ServiceException(400, "submission_not_pending");
        }
        return submission;
    }

    private Task requireTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException(404, "task_not_found");
        }
        return task;
    }

    private long resolvePoints(ApproveSubmissionRequest request, Task task) {
        if (request.getPointsAwarded() == null) {
            return task.getPointReward();
        }
        return request.getPointsAwarded();
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!ALLOWED_STATUS.contains(normalized)) {
            throw new ServiceException(400, "invalid_submission_status");
        }
        return normalized;
    }

    private List<String> buildSignedUrls(Long userId, String submissionNo) {
        List<String> keys = storageService.listKeys("evidence/" + userId + "/" + submissionNo);
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return keys.stream()
                .map(k -> storageService.getSignedUrl(k, 3600))
                .toList();
    }

    private UserPrincipal requireAdmin(UserPrincipal principal) {
        if (principal == null) {
            throw new ServiceException(401, "unauthorized");
        }
        if (!UserType.ADMIN.getName().equalsIgnoreCase(principal.getRole())) {
            throw new ServiceException(403, "forbidden");
        }
        return principal;
    }

    private void sendMessage(Long receiverId, String title, String content) {
        Message message = new Message();
        message.setReceiverId(receiverId);
        message.setTitle(title);
        message.setContent(content);
        message.setIsRead(0);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
    }
}
