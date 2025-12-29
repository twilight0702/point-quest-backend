package com.twilight.pointquestbackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddCartItemRequest {

    @NotNull
    private Long rewardId;

    @NotNull
    @Min(1)
    @Max(99)
    private Integer quantity;
}
