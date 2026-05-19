package com.fleetwise.api.fleet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFleetRequest(
        @Schema(example = "Miguel Fleet Operations")
        @NotBlank
        @Size(max = 255)
        String name
) {}