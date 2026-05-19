package com.fleetwise.api.fuel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateFuelLogRequest(
        @Schema(example = "2026-05-07")
        @NotNull LocalDate fuelDate,
        @Schema(example = "120500")
        @Min(0) Integer mileage,
        @Schema(example = "12.40")
        @NotNull @DecimalMin("0.01") BigDecimal gallons,
        @Schema(example = "38.44")
        @NotNull @DecimalMin("0.01") BigDecimal totalCost,
        @Schema(example = "Regular unleaded fuel")
        @Size(max = 500) String notes
) {}