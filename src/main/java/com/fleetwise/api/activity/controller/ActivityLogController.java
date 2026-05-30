package com.fleetwise.api.activity.controller;

import com.fleetwise.api.activity.dto.ActivityLogResponse;
import com.fleetwise.api.activity.service.ActivityLogService;
import com.fleetwise.api.auth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fleets/{fleetId}/activity")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping
    public List<ActivityLogResponse> getFleetActivity(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId
    ) {
        return activityLogService.getFleetActivity(fleetId, principal.getId());
    }
}