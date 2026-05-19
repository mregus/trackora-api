package com.fleetwise.api.integration;

import com.fleetwise.api.alert.service.MaintenanceAlertService;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class MaintenanceAlertGenerationServiceIT extends BaseIntegrationTest {

    @Autowired
    MaintenanceAlertService maintenanceAlertService;

    @Test
    void scheduledMaintenanceWithinSevenDays_ShouldCreateMaintenanceDueAlert() {
        UUID fleetId = createFleet("Alert Due Fleet");
        UUID vehicleId = createVehicle(fleetId, 50000);

        createMaintenance(
                vehicleId,
                LocalDate.now().plusDays(3),
                60000
        );

        maintenanceAlertService.generateMaintenanceDueAlerts();

        given()
                .header("Authorization", bearer())
                .when()
                .get("/api/fleets/{fleetId}/alerts", fleetId)
                .then()
                .statusCode(200)
                .body("type", hasItem("MAINTENANCE_DUE"))
                .body("severity", hasItem("WARNING"));
    }

    @Test
    void scheduledMaintenancePastMileage_ShouldCreateMaintenanceOverdueAlert() {
        UUID fleetId = createFleet("Alert Overdue Fleet");
        UUID vehicleId = createVehicle(fleetId, 70000);

        createMaintenance(
                vehicleId,
                LocalDate.now().plusDays(30),
                65000
        );

        maintenanceAlertService.generateMaintenanceDueAlerts();

        given()
                .header("Authorization", bearer())
                .when()
                .get("/api/fleets/{fleetId}/alerts", fleetId)
                .then()
                .statusCode(200)
                .body("type", hasItem("MAINTENANCE_OVERDUE"))
                .body("severity", hasItem("CRITICAL"));
    }

    private UUID createFleet(String name) {
        return UUID.fromString(
                given()
                        .header("Authorization", bearer())
                        .contentType(ContentType.JSON)
                        .body("{\"name\":\"" + name + "\"}")
                        .when()
                        .post("/api/fleets")
                        .then()
                        .statusCode(anyOf(is(200), is(201)))
                        .extract()
                        .path("id")
        );
    }

    private UUID createVehicle(UUID fleetId, int currentMileage) {
        return UUID.fromString(
                given()
                        .header("Authorization", bearer())
                        .contentType(ContentType.JSON)
                        .body("""
                        {
                          "vin":"1FTFW1E50MFA00777",
                          "make":"Ford",
                          "model":"F-150",
                          "year":2021,
                          "licensePlate":"ALERT123",
                          "currentMileage":%d
                        }
                        """.formatted(currentMileage))
                        .when()
                        .post("/api/fleets/{fleetId}/vehicles", fleetId)
                        .then()
                        .statusCode(anyOf(is(200), is(201)))
                        .extract()
                        .path("id")
        );
    }

    private String createMaintenance(
            UUID vehicleId,
            LocalDate serviceDate,
            int mileage
    ) {
        return given()
                .header("Authorization", bearer())
                .contentType(ContentType.JSON)
                .body("""
                {
                  "serviceType": "OIL_CHANGE",
                  "serviceDate": "%s",
                  "mileage": %d,
                  "cost": 99.99,
                  "vendor": "Test Shop",
                  "description": "Generated alert test"
                }
                """.formatted(serviceDate, mileage))
                .when()
                .post("/api/vehicles/{vehicleId}/maintenance", vehicleId)
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .extract()
                .path("id");
    }
}