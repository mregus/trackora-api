package com.fleetwise.api.safety.websocket;

import com.fleetwise.api.safety.dto.VehicleSafetyScoreResponse;
import com.fleetwise.api.safety.service.SafetyScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SafetyScoreRealtimePublisher {

    private final SafetyScoreService safetyScoreService;
    private final SimpMessagingTemplate messagingTemplate;

    public void publish(UUID fleetId) {
        List<VehicleSafetyScoreResponse> scores =
                safetyScoreService.getSystemFleetSafetyScores(fleetId);

        messagingTemplate.convertAndSend(
                "/topic/fleets/" + fleetId + "/safety",
                scores
        );
    }
}