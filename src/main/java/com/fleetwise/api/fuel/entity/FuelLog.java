package com.fleetwise.api.fuel.entity;

import com.fleetwise.api.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "fuel_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuelLog {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "fuel_date", nullable = false)
    private LocalDate fuelDate;

    private Integer mileage;

    @Column(nullable = false)
    private BigDecimal gallons;

    @Column(name = "total_cost", nullable = false)
    private BigDecimal totalCost;

    @Column(name = "price_per_gallon", insertable = false, updatable = false)
    private BigDecimal pricePerGallon;

    @Column(columnDefinition = "TEXT")
    private String notes;

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
        if (gallons != null && totalCost != null && pricePerGallon == null) {
            pricePerGallon = gallons.compareTo(BigDecimal.ZERO) > 0
                    ? totalCost.divide(gallons, 3, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
