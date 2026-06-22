package com.fleetwise.api.telematics.simulator;

import com.fleetwise.api.telematics.kafka.TelematicsTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;

@Service
//@RequiredArgsConstructor
public class GeometrisSimulatorService {

    private final TaskScheduler taskScheduler;
    private ScheduledFuture<?> scheduledTask;
    private boolean running;
    private Set<String> activeSerialNumbers = Set.of();
    private int intervalSeconds = 5;
    private Instant startedAt;
    private long packetsPublished;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public GeometrisSimulatorService(
            KafkaTemplate<String, String> kafkaTemplate,
            @Qualifier("simulatorTaskScheduler") TaskScheduler taskScheduler
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.taskScheduler = taskScheduler;
    }

    public void simulate(String serialNumber) {
        long now = Instant.now().getEpochSecond();

        double lat = 28.5383 + ((Math.random() - 0.5) / 100);
        double lon = -81.3792 + ((Math.random() - 0.5) / 100);

        int speed = ThreadLocalRandom.current().nextInt(0, 85);
        int heading = ThreadLocalRandom.current().nextInt(0, 360);
        int fuel = ThreadLocalRandom.current().nextInt(10, 95);
        int rpm = speed > 0 ? ThreadLocalRandom.current().nextInt(900, 2800) : 0;
        int coolantC = ThreadLocalRandom.current().nextInt(80, 105);
        int batteryMv = ThreadLocalRandom.current().nextInt(12100, 12900);

        String packet = """
                F001,%s,LIVE,%d,%.6f,%.6f,%d,%d,112450.2,3500,240,%d,%d,112449.8,1FADP3L9XGL274054,%d,0:0,%d
                """.formatted(
                serialNumber,
                now,
                lat,
                lon,
                speed,
                heading,
                rpm,
                coolantC,
                fuel,
                batteryMv
        ).trim();

        kafkaTemplate.send(
                TelematicsTopics.DEVICE_TELEMETRY,
                serialNumber,
                packet
        );
    }

    public synchronized SimulatorStatusResponse start(
            Set<String> serialNumbers,
            Integer requestedIntervalSeconds
    ) {
        if (running) {
            stop();
        }

        activeSerialNumbers = serialNumbers;
        intervalSeconds = requestedIntervalSeconds == null
                ? 5
                : Math.max(requestedIntervalSeconds, 1);
        packetsPublished = 0;
        startedAt = Instant.now();
        running = true;

        scheduledTask = taskScheduler.scheduleAtFixedRate(
                () -> activeSerialNumbers.forEach(serial -> {
                    simulate(serial);
                    packetsPublished++;
                }),
                Duration.ofSeconds(intervalSeconds)
        );

        return status();
    }

    public synchronized SimulatorStatusResponse stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }

        running = false;

        return status();
    }

    public synchronized SimulatorStatusResponse status() {
        return new SimulatorStatusResponse(
                running,
                activeSerialNumbers,
                intervalSeconds,
                startedAt,
                packetsPublished
        );
    }
}