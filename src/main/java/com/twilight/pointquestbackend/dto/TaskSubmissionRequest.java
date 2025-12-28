package com.twilight.pointquestbackend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for submitting task evidence.
 */
@Data
public class TaskSubmissionRequest {
    @Size(max = 512)
    private String evidenceUrl;

    @Size(max = 20000)
    private String evidenceText;
}
