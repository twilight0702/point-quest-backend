package com.twilight.pointquestbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectSubmissionRequest {
    /**
     * 拒绝原因
     */
    @NotBlank(message = "comment is required when rejecting")
    private String comment;
}
