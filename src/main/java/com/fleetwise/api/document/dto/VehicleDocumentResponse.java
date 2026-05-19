package com.fleetwise.api.document.dto;

import com.fleetwise.api.document.entity.VehicleDocumentType;

import java.time.Instant;
import java.util.UUID;

public record VehicleDocumentResponse(
        UUID id,
        UUID vehicleId,
        UUID maintenanceId,
        String originalFileName,
        String contentType,
        Long fileSize,
        VehicleDocumentType documentType,
        Instant createdAt
) {}