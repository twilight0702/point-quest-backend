package com.twilight.pointquestbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {

    @NotBlank
    @Pattern(regexp = "CREATED|PROCESSING|SHIPPED|COMPLETED|CANCELLED",
            message = "status must be CREATED, PROCESSING, SHIPPED, COMPLETED or CANCELLED")
    private String status;
}
