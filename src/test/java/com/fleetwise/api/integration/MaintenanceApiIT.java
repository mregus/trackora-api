package com.fleetwise.api.integration;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class MaintenanceApiIT extends BaseIntegrationTest {

    @Test
    void createListUpdateDeleteMaintenance() {
        UUID fleetId = createFleet("Maintenance Fleet");
        UUID vehicleId = createVehicle(fleetId);

        String maintenanceId =
                given()
                        .header("Authorization", bearer())
                        .contentType(ContentType.JSON)
                        .body("""
                        {
                          "serviceType": "OIL_CHANGE",
                          "serviceDate": "2026-05-01",
                          "mileage": 125000,
                          "cost": 89.99,
                          "vendor": "Test Shop",
                          "notes": "Synthetic oil change"
                        }
                        """)
                        .when()
                        .post("/api/vehicles/{vehicleId}/maintenance", vehicleId)
                        .then()
                        .statusCode(anyOf(is(200), is(201)))
                        .body("serviceType", equalTo("OIL_CHANGE"))
                        .body("cost", comparesEqualTo(89.99f))
                        .extract()
                        .path("id");

        given()
                .header("Authorization", bearer())
                .when()
                .get("/api/vehicles/{vehicleId}/maintenance", vehicleId)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));

        given()
                .header("Authorization", bearer())
                .contentType(ContentType.JSON)
                .body("""
                {
                  "serviceType": "BRAKES",
                  "serviceDate": "2026-05-02",
                  "mileage": 126000,
                  "cost": 299.99,
                  "vendor": "Brake Shop",
                  "notes": "Front brake pads",
                  "status": "COMPLETED"
                }
                """)
                .when()
                .put("/api/maintenance/{maintenanceId}", maintenanceId)
                .then()
                .statusCode(200)
                .body("serviceType", equalTo("BRAKES"))
                .body("vendor", equalTo("Brake Shop"));

        given()
                .header("Authorization", bearer())
                .when()
                .delete("/api/maintenance/{maintenanceId}", maintenanceId)
                .then()
                .statusCode(anyOf(is(200), is(204)));
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

    private UUID createVehicle(UUID fleetId) {
        return UUID.fromString(
                given()
                        .header("Authorization", bearer())
                        .contentType(ContentType.JSON)
                        .body("""
                        {
                          "vin":"1ZVBP8AM8E5320001",
                          "make":"Ford",
                          "model":"Transit",
                          "year":2020,
                          "licensePlate":"MAIN123",
                          "currentMileage":12345
                        }
                        """)
                        .when()
                        .post("/api/fleets/{fleetId}/vehicles", fleetId)
                        .then()
                        .statusCode(anyOf(is(200), is(201)))
                        .extract()
                        .path("id")
        );
    }
}