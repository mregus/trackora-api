package com.fleetwise.api.auth.dto;

import java.util.UUID;

public record MeResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String role
) {}