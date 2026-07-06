package com.fleetwise.api.telematics.dto;

import java.time.OffsetDateTime;

public record DeadLetterMessageResponse(
        String messageId,
        String body,
        String deadLetterReason,
        String deadLetterErrorDescription,
        long deliveryCount,
        OffsetDateTime enqueuedTime
) {}