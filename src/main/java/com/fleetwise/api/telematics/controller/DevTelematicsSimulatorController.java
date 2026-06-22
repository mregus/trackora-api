package com.fleetwise.api.telematics.controller;

import com.fleetwise.api.telematics.simulator.GeometrisSimulatorService;
import com.fleetwise.api.telematics.simulator.SimulatorStatusResponse;
import com.fleetwise.api.telematics.simulator.StartGeometrisSimulatorRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dev/telematics")
public class DevTelematicsSimulatorController {

    private final GeometrisSimulatorService simulatorService;

    @PostMapping("/simulate/geometris/{serialNumber}")
    public Map<String, String> simulate(
            @PathVariable String serialNumber
    ) {
        simulatorService.simulate(serialNumber);

        return Map.of("message", "Simulated packet published to Kafka");
    }

    @PostMapping("/simulators/geometris/start")
    public SimulatorStatusResponse start(
            @RequestBody StartGeometrisSimulatorRequest request
    ) {
        return simulatorService.start(
                request.serialNumbers(),
                request.intervalSeconds()
        );
    }

    @PostMapping("/simulators/geometris/stop")
    public SimulatorStatusResponse stop() {
        return simulatorService.stop();
    }

    @GetMapping("/simulators/geometris/status")
    public SimulatorStatusResponse status() {
        return simulatorService.status();
    }
}
