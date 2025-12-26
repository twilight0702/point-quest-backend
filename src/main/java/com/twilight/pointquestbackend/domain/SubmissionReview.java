package com.twilight.pointquestbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 审核记录表：记录管理员对提交的评审与奖励积分
 * @TableName submission_review
 */
@TableName(value ="submission_review")
@Data
public class SubmissionReview implements Serializable {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 
     */
    @TableField(value = "submission_id")
    private Long submission_id;

    /**
     * 
     */
    @TableField(value = "reviewer_id")
    private Long reviewer_id;

    /**
     * 
     */
    @TableField(value = "comment")
    private String comment;

    /**
     * 
     */
    @TableField(value = "points_awarded")
    private Long points_awarded;

    /**
     * 
     */
    @TableField(value = "created_at")
    private Date created_at;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}