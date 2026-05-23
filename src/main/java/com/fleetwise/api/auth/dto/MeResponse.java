package com.fleetwise.api.auth.dto;

import com.fleetwise.api.auth.entity.UserRole;

import java.util.UUID;

public record MeResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        UserRole role
) {}