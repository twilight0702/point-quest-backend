package com.twilight.pointquestbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CheckoutSubmitRequest {
    /**
     * Arbitrary address payload that will be stored as JSON.
     */
    @NotNull
    @Size(min = 1, max = 255)
    private String address;
}
