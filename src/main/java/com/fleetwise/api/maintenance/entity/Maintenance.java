package com.fleetwise.api.maintenance.entity;

import com.fleetwise.api.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "maintenance")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Maintenance {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "service_type", nullable = false, length = 100)
    private String serviceType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    private Integer mileage;

    private BigDecimal cost;

    private String vendor;

    @Column(name = "next_service_date")
    private LocalDate nextServiceDate;

    @Enumerated(EnumType.STRING)
    private MaintenanceStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        id = id == null ? UUID.randomUUID() : id;
        createdAt = now;
        updatedAt = now;
        if (status == null) status = MaintenanceStatus.COMPLETED;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
