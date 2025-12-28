package com.twilight.pointquestbackend.domain;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

import com.twilight.pointquestbackend.common.TaskStatus;
import lombok.Data;

/**
 * 任务表：记录可领取任务、时间范围与发布者
 * @TableName task
 */
@TableName(value ="task")
@Data
public class Task implements Serializable {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "task_no")
    private String taskNo;

    /**
     * 
     */
    @TableField(value = "title")
    private String title;

    /**
     * 
     */
    @TableField(value = "description")
    private String description;

    /**
     * 
     */
    @TableField(value = "point_reward")
    private Long pointReward;

    /**
     * 
     */
    @TableField(value = "deadline")
    private LocalDateTime deadline;

    /**
     * 
     */
    @TableField(value = "status")
    private TaskStatus status;

    /**
     * 
     */
    @TableField(value = "created_by")
    private Long createdBy;


    @TableField(value = "created_user_type")
    private String createdUserType;

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

    @TableLogic
    @TableField(value = "is_del")
    private Integer isDel;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}