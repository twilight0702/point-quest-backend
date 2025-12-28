package com.twilight.pointquestbackend.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ApproveSubmissionRequest {
    /**
     * 积分发放数，缺省时使用任务默认奖励
     */
    @Min(value = 0, message = "pointsAwarded must be non-negative")
    private Long pointsAwarded;

    /**
     * 审核备注，可选
     */
    private String comment;
}
