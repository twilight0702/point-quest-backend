package com.twilight.pointquestbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordDTO {
    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 6, max = 64)
    private String newPassword;
}
