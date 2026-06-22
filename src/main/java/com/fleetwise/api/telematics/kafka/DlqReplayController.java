package com.fleetwise.api.telematics.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/kafka/dlq")
public class DlqReplayController {

    private final DlqReplayService dlqReplayService;

    @PostMapping("/device-telemetry/replay")
    public Map<String, Object> replay(
            @RequestBody(required = false) DlqReplayRequest request
    ) {
        int maxMessages = request == null || request.maxMessages() <= 0
                ? 10
                : request.maxMessages();

        int replayed = dlqReplayService.replayDeviceTelemetryDlq(maxMessages);

        return Map.of(
                "message", "DLQ replay completed",
                "replayed", replayed
        );
    }
}