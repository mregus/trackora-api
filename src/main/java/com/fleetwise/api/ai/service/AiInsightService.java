package com.fleetwise.api.ai.service;

import com.fleetwise.api.ai.dto.AiInsightResponse;
import com.fleetwise.api.ai.dto.GenerateAiSummaryRequest;
import com.fleetwise.api.ai.entity.AiInsight;
import com.fleetwise.api.ai.openai.AiPromptType;
import com.fleetwise.api.ai.openai.OpenAiClient;
import com.fleetwise.api.ai.repository.AiInsightRepository;
import com.fleetwise.api.common.exception.ExternalServiceException;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.fleet.repository.FleetRepository;
import com.fleetwise.api.vehicle.entity.Vehicle;
import com.fleetwise.api.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiInsightService {

    private final VehicleRepository vehicleRepository;
    private final FleetRepository fleetRepository;
    private final AiInsightRepository aiRepository;
    private final OpenAiClient openAiClient;

    @Transactional
    public AiInsightResponse generateFleetSummary(
            UUID fleetId,
            UUID ownerId,
            GenerateAiSummaryRequest req
    ) {
        var fleet = fleetRepository.findByIdAndOwnerId(fleetId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet not found"));

        Instant since = Instant.now().minus(1, ChronoUnit.DAYS);

        boolean alreadyGenerated =
                aiRepository.existsByFleetIdAndVehicleIsNullAndCreatedAtAfter(
                        fleetId,
                        since
                );

        if (alreadyGenerated) {
            throw new IllegalStateException(
                    "Fleet AI summary can only be generated once per day."
            );
        }

        String prompt = """
                Provide an operations summary for fleet "%s".

                Timeframe: %s

                Focus on the entire fleet only:
                - fleet health
                - maintenance trends
                - fuel usage trends
                - open alerts
                - operational risk

                Do not focus on a single vehicle.
                """.formatted(
                fleet.getName(),
                req.timeframe()
        );

        if (req.includeFuelStats()) {
            prompt += "\nInclude recent fleet-level fuel anomalies.";
        }

        if (req.includeMaintenanceStats()) {
            prompt += "\nInclude fleet-level maintenance reliability trends.";
        }

        String summary = openAiClient.generateText(prompt, AiPromptType.FLEET);

        AiInsight insight = AiInsight.builder()
                .fleet(fleet)
                .vehicle(null)
                .promptHash(sha(prompt))
                .summary(summary)
                .build();

        AiInsight saved = aiRepository.save(insight);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AiInsightResponse> listInsights(UUID fleetId, UUID ownerId) {
        fleetRepository.findByIdAndOwnerId(fleetId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet not found"));

        return aiRepository.findByFleetIdAndVehicleIsNullOrderByCreatedAtDesc(fleetId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AiInsightResponse getLatestFleetInsight(UUID fleetId, UUID ownerId) {
        fleetRepository.findByIdAndOwnerId(fleetId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet not found"));

        return aiRepository.findFirstByFleetIdAndVehicleIsNullOrderByCreatedAtDesc(fleetId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public AiInsightResponse generateVehicleSummary(
            UUID vehicleId,
            UUID ownerId,
            GenerateAiSummaryRequest request
    ) {
        Vehicle vehicle = vehicleRepository.findByIdAndFleetOwnerId(vehicleId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        Instant since = Instant.now().minus(1, ChronoUnit.DAYS);

        boolean alreadyGenerated =
                aiRepository.existsByVehicleIdAndCreatedAtAfter(
                        vehicleId,
                        since
                );

        if (alreadyGenerated) {
            throw new IllegalStateException(
                    "Vehicle AI summary can only be generated once per day."
            );
        }

        String prompt = """
                Provide an operations summary for this vehicle only.

                Vehicle:
                - Year: %s
                - Make: %s
                - Model: %s
                - VIN: %s
                - Mileage: %s
                - Status: %s

                Timeframe: %s

                Focus only on this vehicle's:
                - maintenance history
                - fuel usage
                - alerts
                - operational risk
                - recommended next actions

                Do not summarize the entire fleet.
                """.formatted(
                vehicle.getYear(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getVin(),
                vehicle.getCurrentMileage(),
                vehicle.getStatus(),
                request.timeframe()
        );

        String summary = openAiClient.generateText(prompt, AiPromptType.VEHICLE);

        AiInsight insight = AiInsight.builder()
                .fleet(vehicle.getFleet())
                .vehicle(vehicle)
                .promptHash(sha(prompt))
                .summary(summary)
                .build();

        AiInsight saved = aiRepository.save(insight);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AiInsightResponse getLatestVehicleInsight(UUID vehicleId, UUID ownerId) {
        vehicleRepository.findByIdAndFleetOwnerId(vehicleId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        return aiRepository.findFirstByVehicleIdOrderByCreatedAtDesc(vehicleId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AiInsightResponse> listVehicleInsights(UUID vehicleId, UUID ownerId) {
        vehicleRepository.findByIdAndFleetOwnerId(vehicleId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        return aiRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AiInsightResponse toResponse(AiInsight insight) {
        return new AiInsightResponse(
                insight.getId(),
                insight.getFleet().getId(),
                insight.getSummary(),
                insight.getCreatedAt()
        );
    }

    private String sha(String prompt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(prompt.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash AI prompt", e);
        }
    }
}