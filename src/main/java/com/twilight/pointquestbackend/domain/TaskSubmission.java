package com.twilight.pointquestbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
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
    @TableField(value = "evidence_url")
    private String evidenceUrl;

    /**
     * 
     */
    @TableField(value = "evidence_text")
    private String evidenceText;

    /**
     * 
     */
    @TableField(value = "status")
    private Object status;

    /**
     * 
     */
    @TableField(value = "created_at")
    private Date createdAt;

    /**
     * 
     */
    @TableField(value = "updated_at")
    private Date updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}