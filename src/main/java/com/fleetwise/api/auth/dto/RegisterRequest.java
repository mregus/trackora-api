package com.fleetwise.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "miguel@example.com") @Email @NotBlank String email,
        @Schema(example = "Password123!") @NotBlank @Size(min = 8) String password,
        @Schema(example = "Miguel") @NotBlank String firstName,
        @Schema(example = "Regus") @NotBlank String lastName
) {}