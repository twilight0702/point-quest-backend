package com.twilight.pointquestbackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Data;

/**
 * Payload for creating or updating a reward item.
 */
@Data
public class RewardRequest {
    @NotBlank
    private String name;

    private String description;

    @NotNull
    @Min(0)
    private Long pointCost;

    @Pattern(regexp = "ON|OFF", message = "status must be ON or OFF")
    private String status;

    @Min(0)
    private Integer stock;

    /**
     * Category ids to bind this reward to.
     */
    private List<Long> categoryIds;
}
