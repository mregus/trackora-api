package com.fleetwise.api.common.exception;

public record FieldErrorResponse(
        String field,
        String message
) {}