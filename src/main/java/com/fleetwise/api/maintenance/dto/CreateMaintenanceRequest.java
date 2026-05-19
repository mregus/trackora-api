package com.fleetwise.api.maintenance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateMaintenanceRequest(
        @Schema(example = "Oil Change")
        @NotBlank String serviceType,
        @Schema(example = "Updated record – checked brakes too")
        String description,
        @Schema(example = "2026-05-05")
        @NotNull LocalDate serviceDate,
        @Schema(example = "110200")
        @Min(0) Integer mileage,
        @Schema(example = "89.99")
        @DecimalMin("0.0") BigDecimal cost,
        @Schema(example = "QuickLube")
        String vendor,
        @Schema(example = "2026-08-05")
        LocalDate nextServiceDate
) {}