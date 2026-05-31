package com.fleetwise.api.notification.email;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class EmailService {

    private final RestClient restClient;

    @Value("${email.enabled:false}")
    private boolean enabled;

    @Value("${email.from}")
    private String from;

    public EmailService(
            @Value("${email.resend.api-key}") String apiKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public void sendEmail(String to, String subject, String html) {
        if (!enabled) {
            return;
        }

        restClient.post()
                .uri("/emails")
                .body(Map.of(
                        "from", from,
                        "to", to,
                        "subject", subject,
                        "html", html
                ))
                .retrieve()
                .toBodilessEntity();
    }
}