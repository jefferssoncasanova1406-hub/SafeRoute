package com.upc.grupo3.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordChangeRequestDTO {
    @NotBlank
    private String currentPassword;

    @NotBlank
    private String newPassword;
}
