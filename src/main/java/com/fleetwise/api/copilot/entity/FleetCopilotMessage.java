package com.fleetwise.api.copilot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fleet_copilot_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FleetCopilotMessage {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private FleetCopilotConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CopilotMessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "supporting_facts", columnDefinition = "TEXT")
    private String supportingFacts;

    @Column(name = "ai_generated", nullable = false)
    private boolean aiGenerated;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void createTimestamp() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}