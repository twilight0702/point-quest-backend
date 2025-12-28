package com.twilight.pointquestbackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Payload for submitting task evidence.
 */
@Data
public class TaskSubmissionDTO {
    @Size(max = 20000)
    private String evidenceText;
}
