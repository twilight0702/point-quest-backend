package com.twilight.pointquestbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import com.twilight.pointquestbackend.common.TaskSubmissionStatus;
import lombok.Data;

/**
 * 任务提交表：用户提交任务完成证明及审批状态
 * @TableName task_submission
 */
@TableName(value ="task_submission")
@Data
public class TaskSubmission implements Serializable {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "submission_no")
    private String submissionNo;

    /**
     * 
     */
    @TableField(value = "task_id")
    private Long taskId;

    /**
     * 
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 
     */
    @TableField(value = "evidence_text")
    private String evidenceText;

    /**
     * 
     */
    @TableField(value = "status")
    private TaskSubmissionStatus status;

    /**
     * 
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    /**
     * 
     */
    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}