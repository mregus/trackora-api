package com.fleetwise.api.telematics.controller;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.fleetwise.api.telematics.dto.DeadLetterMessageResponse;
import com.fleetwise.api.telematics.azure.GeometrisServiceBusDlqService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/servicebus")
@ConditionalOnProperty(
        name = "azure.servicebus.enabled",
        havingValue = "true"
)
public class ServiceBusAdminController {

    private final GeometrisServiceBusDlqService dlqService;

    @GetMapping("/deadletters")
    public List<DeadLetterMessageResponse> deadLetters(
            @RequestParam(defaultValue = "20") int maxMessages
    ) {
        return dlqService.peekDeadLetters(maxMessages)
                .stream()
                .map(message -> new DeadLetterMessageResponse(
                        message.getMessageId(),
                        message.getBody().toString(),
                        message.getDeadLetterReason(),
                        message.getDeadLetterErrorDescription(),
                        message.getDeliveryCount(),
                        message.getEnqueuedTime()
                ))
                .toList();
    }

    @DeleteMapping("/deadletters/{messageId}")
    public Map<String, String> deleteDeadLetter(
            @PathVariable String messageId
    ) {
        dlqService.deleteDeadLetter(messageId);

        return Map.of("message", "Dead letter deleted");
    }

    @PostMapping("/deadletters/{messageId}/replay")
    public Map<String, String> replayDeadLetter(
            @PathVariable String messageId
    ) {
        dlqService.replayDeadLetter(messageId);

        return Map.of("message", "Dead letter replayed");
    }
}