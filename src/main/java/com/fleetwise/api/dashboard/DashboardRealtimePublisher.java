package com.fleetwise.api.dashboard;

import com.fleetwise.api.dashboard.dto.DashboardSummaryResponse;
import com.fleetwise.api.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardRealtimePublisher {

    private final DashboardService dashboardService;
    private final SimpMessagingTemplate messagingTemplate;

    public void publish(UUID fleetId) {

        DashboardSummaryResponse summary =
                dashboardService.getSystemSummary(fleetId);

        messagingTemplate.convertAndSend(
                "/topic/fleets/" + fleetId + "/dashboard",
                summary
        );
    }
}