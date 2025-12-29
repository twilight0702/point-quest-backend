package com.twilight.pointquestbackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * Request body for pool management.
 */
@Data
public class PoolRequest {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    @Min(0)
    private Long pointCost;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Pattern(regexp = "ON|OFF", message = "status must be ON or OFF")
    private String status;

    private String type;

    @Valid
    @Size(max = 100, message = "too many pool items")
    private List<PoolItemRequest> items;

    @Data
    public static class PoolItemRequest {
        @NotNull
        private Long rewardId;

        private Integer sortNo;

        @NotNull
        @Min(0)
        private Long weight;
    }
}
