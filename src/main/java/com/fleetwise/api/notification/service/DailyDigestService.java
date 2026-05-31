package com.fleetwise.api.notification.service;

import com.fleetwise.api.dashboard.dto.DashboardSummaryResponse;
import com.fleetwise.api.dashboard.dto.FleetRecommendationResponse;
import com.fleetwise.api.dashboard.service.DashboardService;
import com.fleetwise.api.fleet.entity.Fleet;
import com.fleetwise.api.fleet.repository.FleetRepository;
import com.fleetwise.api.notification.email.EmailService;
import com.fleetwise.api.notification.entity.UserNotificationSettings;
import com.fleetwise.api.notification.repository.UserNotificationSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyDigestService {

    private final FleetRepository fleetRepository;
    private final DashboardService dashboardService;
    private final EmailService emailService;
    private final UserNotificationSettingsRepository settingsRepository;

    public void sendDailyDigest() {
        List<Fleet> fleets = fleetRepository.findAll();

        for (Fleet fleet : fleets) {
            var owner = fleet.getOwner();

            DashboardSummaryResponse summary =
                    dashboardService.getSummary(fleet.getId(), owner.getId());

            List<FleetRecommendationResponse> recommendations =
                    dashboardService.getRecommendations(fleet.getId(), owner.getId());

            boolean dailyDigestEnabled = settingsRepository.findById(owner.getId())
                    .map(UserNotificationSettings::isDailyDigestEnabled)
                    .orElse(true);

            if (!dailyDigestEnabled) {
                continue;
            }

            String html = buildHtml(fleet, summary, recommendations);

            emailService.sendEmail(
                    owner.getEmail(),
                    "Trackora Daily Fleet Digest - " + fleet.getName(),
                    html
            );
        }
    }

    private String buildHtml(
            Fleet fleet,
            DashboardSummaryResponse summary,
            List<FleetRecommendationResponse> recommendations
    ) {
        String recommendationHtml = recommendations.isEmpty()
                ? "<p>No urgent recommendations today. Fleet looks stable.</p>"
                : recommendations.stream()
                .map(rec -> """
                                <li>
                                  <strong>%s</strong> - %s
                                </li>
                                """.formatted(rec.severity(), rec.message()))
                .reduce("", String::concat);

        return """
                <div style="font-family: Arial, sans-serif; color: #0f172a;">
                  <h1>Trackora Daily Fleet Digest</h1>

                  <h2>%s</h2>

                  <p>
                    Fleet Health Score:
                    <strong style="font-size: 24px;">%s</strong>
                  </p>

                  <ul>
                    <li>Active vehicles: %s</li>
                    <li>Open alerts: %s</li>
                    <li>Maintenance due: %s</li>
                    <li>Fuel spend last 30 days: $%s</li>
                  </ul>

                  <h3>Recommendations</h3>
                  <ul>
                    %s
                  </ul>

                  <p style="color: #64748b; font-size: 12px;">
                    This email was generated automatically by Trackora.
                  </p>
                </div>
                """.formatted(
                fleet.getName(),
                summary.fleetHealthScore(),
                summary.activeVehicles(),
                summary.openAlerts(),
                summary.fleetHealthBreakdown().overdueMaintenance(),
                summary.monthlyFuelCost(),
                recommendationHtml
        );
    }
}