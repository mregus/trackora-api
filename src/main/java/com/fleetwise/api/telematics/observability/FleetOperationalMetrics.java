package com.fleetwise.api.telematics.observability;

import com.fleetwise.api.alert.repository.AlertRepository;
import com.fleetwise.api.telematics.repository.VehicleCurrentStateRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class FleetOperationalMetrics {

    public FleetOperationalMetrics(
            MeterRegistry registry,
            VehicleCurrentStateRepository stateRepository,
            AlertRepository alertRepository
    ) {
        Gauge.builder("trackora.vehicles.online", stateRepository, repo ->
                        repo.findAll().stream()
                                .filter(state -> status(state.getLastSeenAt()).equals("ONLINE"))
                                .count()
                )
                .description("Current online vehicles")
                .register(registry);

        Gauge.builder("trackora.vehicles.stale", stateRepository, repo ->
                        repo.findAll().stream()
                                .filter(state -> status(state.getLastSeenAt()).equals("STALE"))
                                .count()
                )
                .description("Current stale vehicles")
                .register(registry);

        Gauge.builder("trackora.vehicles.offline", stateRepository, repo ->
                        repo.findAll().stream()
                                .filter(state -> status(state.getLastSeenAt()).equals("OFFLINE"))
                                .count()
                )
                .description("Current offline vehicles")
                .register(registry);

        Gauge.builder("trackora.alerts.active", alertRepository, repo ->
                        repo.findAll().stream()
                                .filter(alert -> !alert.isResolved())
                                .count()
                )
                .description("Current unresolved alerts")
                .register(registry);
    }

    private static String status(Instant lastSeenAt) {
        if (lastSeenAt == null) {
            return "OFFLINE";
        }

        long minutes = ChronoUnit.MINUTES.between(lastSeenAt, Instant.now());

        if (minutes <= 5) {
            return "ONLINE";
        }

        if (minutes <= 15) {
            return "STALE";
        }

        return "OFFLINE";
    }
}