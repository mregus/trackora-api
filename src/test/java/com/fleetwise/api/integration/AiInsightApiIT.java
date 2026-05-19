package com.fleetwise.api.integration;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class AiInsightApiIT extends BaseIntegrationTest {

    @Test
    void fleetAiSummary_ShouldOnlyGenerateOncePerDay() {
        UUID fleetId = createFleet("AI Fleet");

        given()
                .header("Authorization", bearer())
                .contentType(ContentType.JSON)
                .body("""
                {
                  "timeframe": "Last 30 days",
                  "includeFuelStats": true,
                  "includeMaintenanceStats": true
                }
                """)
                .when()
                .post("/api/fleets/{fleetId}/ai/summary", fleetId)
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .body("fleetId", equalTo(fleetId.toString()))
                .body("summary", not(emptyString()));

        given()
                .header("Authorization", bearer())
                .contentType(ContentType.JSON)
                .body("""
                {
                  "timeframe": "Last 30 days",
                  "includeFuelStats": true,
                  "includeMaintenanceStats": true
                }
                """)
                .when()
                .post("/api/fleets/{fleetId}/ai/summary", fleetId)
                .then()
                .statusCode(anyOf(is(400), is(409)));
    }

    @Test
    void vehicleAiSummary_ShouldOnlyGenerateOncePerDay() {
        UUID fleetId = createFleet("Vehicle AI Fleet");
        UUID vehicleId = createVehicle(fleetId);

        given()
                .header("Authorization", bearer())
                .contentType(ContentType.JSON)
                .body("""
                {
                  "timeframe": "Last 30 days",
                  "includeFuelStats": true,
                  "includeMaintenanceStats": true
                }
                """)
                .when()
                .post("/api/vehicles/{vehicleId}/ai/summary", vehicleId)
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .body("fleetId", equalTo(fleetId.toString()))
                .body("summary", not(emptyString()));

        given()
                .header("Authorization", bearer())
                .contentType(ContentType.JSON)
                .body("""
                {
                  "timeframe": "Last 30 days",
                  "includeFuelStats": true,
                  "includeMaintenanceStats": true
                }
                """)
                .when()
                .post("/api/vehicles/{vehicleId}/ai/summary", vehicleId)
                .then()
                .statusCode(anyOf(is(400), is(409)));
    }

    @Test
    void fleetInsights_ShouldNotIncludeVehicleInsights() {
        UUID fleetId = createFleet("Separated AI Fleet");
        UUID vehicleId = createVehicle(fleetId);

        generateFleetSummary(fleetId);
        generateVehicleSummary(vehicleId);

        given()
                .header("Authorization", bearer())
                .when()
                .get("/api/fleets/{fleetId}/ai/insights", fleetId)
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].summary", containsString("Fleet"));
    }

    @Test
    void vehicleInsights_ShouldNotIncludeFleetInsights() {
        UUID fleetId = createFleet("Separated Vehicle AI Fleet");
        UUID vehicleId = createVehicle(fleetId);

        generateFleetSummary(fleetId);
        generateVehicleSummary(vehicleId);

        given()
                .header("Authorization", bearer())
                .when()
                .get("/api/vehicles/{vehicleId}/ai/insights", vehicleId)
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].summary", containsString("Vehicle"));
    }

    private void generateFleetSummary(UUID fleetId) {
        given()
                .header("Authorization", bearer())
                .contentType(ContentType.JSON)
                .body("""
                {
                  "timeframe": "Last 30 days",
                  "includeFuelStats": true,
                  "includeMaintenanceStats": true
                }
                """)
                .when()
                .post("/api/fleets/{fleetId}/ai/summary", fleetId)
                .then()
                .statusCode(anyOf(is(200), is(201)));
    }

    private void generateVehicleSummary(UUID vehicleId) {
        given()
                .header("Authorization", bearer())
                .contentType(ContentType.JSON)
                .body("""
                {
                  "timeframe": "Last 30 days",
                  "includeFuelStats": true,
                  "includeMaintenanceStats": true
                }
                """)
                .when()
                .post("/api/vehicles/{vehicleId}/ai/summary", vehicleId)
                .then()
                .statusCode(anyOf(is(200), is(201)));
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
                          "vin":"1FTFW1E50MFA00888",
                          "make":"Ford",
                          "model":"F-150",
                          "year":2021,
                          "licensePlate":"AI123",
                          "currentMileage":50300
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