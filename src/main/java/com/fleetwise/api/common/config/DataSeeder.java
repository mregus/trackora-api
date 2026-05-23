package com.fleetwise.api.common.config;

import com.fleetwise.api.ai.entity.AiInsight;
import com.fleetwise.api.ai.repository.AiInsightRepository;
import com.fleetwise.api.alert.entity.Alert;
import com.fleetwise.api.alert.entity.AlertSeverity;
import com.fleetwise.api.alert.entity.AlertType;
import com.fleetwise.api.alert.repository.AlertRepository;
import com.fleetwise.api.auth.entity.User;
import com.fleetwise.api.auth.entity.UserRole;
import com.fleetwise.api.auth.repository.UserRepository;
import com.fleetwise.api.fleet.entity.Fleet;
import com.fleetwise.api.fleet.repository.FleetRepository;
import com.fleetwise.api.fuel.entity.FuelLog;
import com.fleetwise.api.fuel.repository.FuelLogRepository;
import com.fleetwise.api.maintenance.entity.Maintenance;
import com.fleetwise.api.maintenance.entity.MaintenanceStatus;
import com.fleetwise.api.maintenance.repository.MaintenanceRepository;
import com.fleetwise.api.vehicle.entity.Vehicle;
import com.fleetwise.api.vehicle.entity.VehicleStatus;
import com.fleetwise.api.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final FleetRepository fleetRepository;
    private final VehicleRepository vehicleRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final FuelLogRepository fuelLogRepository;
    private final AlertRepository alertRepository;
    private final AiInsightRepository aiInsightRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmailIgnoreCase("demo@fleetwise.com")) {
            return;
        }

        User user = userRepository.findByEmailIgnoreCase("demo@fleetwise.com")
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email("demo@fleetwise.com")
                                .passwordHash(passwordEncoder.encode("Password123!"))
                                .firstName("Demo")
                                .lastName("User")
                                .role(UserRole.OWNER)
                                .build()
                ));

        Fleet fleet = fleetRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(f -> f.getName().equals("Demo Fleet"))
                .findFirst()
                .orElseGet(() -> fleetRepository.save(
                        Fleet.builder()
                                .name("Demo Fleet")
                                .owner(user)
                                .build()
                ));

        if (!vehicleRepository.findByFleetIdOrderByCreatedAtDesc(fleet.getId()).isEmpty()) {
            return;
        }

        Vehicle van = vehicleRepository.save(
                Vehicle.builder()
                        .fleet(fleet)
                        .vin("1FTYE1ZM0KKA00001")
                        .make("Ford")
                        .model("Transit")
                        .year(2020)
                        .licensePlate("DEMO-001")
                        .currentMileage(75200)
                        .status(VehicleStatus.ACTIVE)
                        .build()
        );

        Vehicle truck = vehicleRepository.save(
                Vehicle.builder()
                        .fleet(fleet)
                        .vin("1FTEW1EP5MFA00002")
                        .make("Ford")
                        .model("F-150")
                        .year(2021)
                        .licensePlate("DEMO-002")
                        .currentMileage(50300)
                        .status(VehicleStatus.IN_SHOP)
                        .build()
        );

        Vehicle focus = vehicleRepository.save(
                Vehicle.builder()
                        .fleet(fleet)
                        .vin("1FADP3L90GL000003")
                        .make("Ford")
                        .model("Focus ST")
                        .year(2016)
                        .licensePlate("DEMO-003")
                        .currentMileage(109000)
                        .status(VehicleStatus.ACTIVE)
                        .build()
        );

        maintenanceRepository.save(
                Maintenance.builder()
                        .vehicle(van)
                        .serviceType("OIL_CHANGE")
                        .serviceDate(LocalDate.now().minusDays(10))
                        .mileage(74800)
                        .cost(new BigDecimal("89.99"))
                        .vendor("Quick Lube")
                        .description("Synthetic oil change")
                        .status(MaintenanceStatus.COMPLETED)
                        .build()
        );

        maintenanceRepository.save(
                Maintenance.builder()
                        .vehicle(truck)
                        .serviceType("BRAKES")
                        .serviceDate(LocalDate.now().minusDays(4))
                        .mileage(50200)
                        .cost(new BigDecimal("420.00"))
                        .vendor("Brake Shop")
                        .description("Front brake pads and rotors")
                        .status(MaintenanceStatus.COMPLETED)
                        .build()
        );

        maintenanceRepository.save(
                Maintenance.builder()
                        .vehicle(focus)
                        .serviceType("INSPECTION")
                        .serviceDate(LocalDate.now().plusDays(7))
                        .mileage(109000)
                        .cost(new BigDecimal("0.00"))
                        .vendor("Demo Shop")
                        .description("Upcoming inspection")
                        .status(MaintenanceStatus.SCHEDULED)
                        .build()
        );

        fuelLogRepository.save(
                FuelLog.builder()
                        .vehicle(van)
                        .fuelDate(LocalDate.now().minusDays(6))
                        .mileage(75100)
                        .gallons(new BigDecimal("15.2"))
                        .totalCost(new BigDecimal("56.24"))
                        .pricePerGallon(new BigDecimal("3.70"))
                        .build()
        );

        fuelLogRepository.save(
                FuelLog.builder()
                        .vehicle(truck)
                        .fuelDate(LocalDate.now().minusDays(5))
                        .mileage(50250)
                        .gallons(new BigDecimal("21.5"))
                        .totalCost(new BigDecimal("81.70"))
                        .pricePerGallon(new BigDecimal("3.80"))
                        .build()
        );

        fuelLogRepository.save(
                FuelLog.builder()
                        .vehicle(focus)
                        .fuelDate(LocalDate.now().minusDays(2))
                        .mileage(109050)
                        .gallons(new BigDecimal("10.8"))
                        .totalCost(new BigDecimal("39.96"))
                        .pricePerGallon(new BigDecimal("3.70"))
                        .build()
        );

        alertRepository.save(
                Alert.builder()
                        .fleet(fleet)
                        .vehicle(focus)
                        .type(AlertType.MAINTENANCE_DUE)
                        .severity(AlertSeverity.WARNING)
                        .message("Focus ST is due for inspection soon.")
                        .resolved(false)
                        .build()
        );

        alertRepository.save(
                Alert.builder()
                        .fleet(fleet)
                        .vehicle(truck)
                        .type(AlertType.INSPECTION_REMINDER)
                        .severity(AlertSeverity.CRITICAL)
                        .message("F-150 is currently marked as in shop.")
                        .resolved(false)
                        .build()
        );

        aiInsightRepository.save(
                AiInsight.builder()
                        .fleet(fleet)
                        .promptHash("demo-seed-summary")
                        .summary("""
                                Demo Fleet Summary:
                                - Fleet has 3 vehicles.
                                - 2 vehicles are active and 1 is currently in shop.
                                - Recent maintenance costs are driven mainly by brake work on the F-150.
                                - Fuel spending appears normal for the current period.
                                - Focus ST has an upcoming inspection reminder.
                                """)
                        .build()
        );

        System.out.println("Demo seed data created.");
        System.out.println("Login: demo@fleetwise.com / Password123!");
    }
}