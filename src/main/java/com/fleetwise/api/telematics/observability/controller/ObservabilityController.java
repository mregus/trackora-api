package com.fleetwise.api.telematics.observability.controller;

import com.fleetwise.api.telematics.observability.dto.ObservabilityStatusResponse;
import com.fleetwise.api.telematics.observability.service.ObservabilityStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/observability")
public class ObservabilityController {

    private final ObservabilityStatusService service;

    @GetMapping("/status")
    public ObservabilityStatusResponse getStatus() {
        return service.getStatus();
    }
}