package com.twilight.pointquestbackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import net.sf.jsqlparser.expression.DateTimeLiteralExpression;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Payload for creating or updating a task.
 */
@Data
public class TaskRequest {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    @Min(0)
    private Long pointReward;

    private LocalDateTime deadline;

    @Pattern(regexp = "OPEN|CLOSED|ENDED", message = "status must be OPEN, CLOSED or ENDED")
    private String status;
}
