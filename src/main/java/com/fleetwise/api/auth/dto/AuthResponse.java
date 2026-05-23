package com.fleetwise.api.auth.dto;

import com.fleetwise.api.auth.entity.UserRole;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String email,
        String firstName,
        String lastName,
        UserRole role
) {}