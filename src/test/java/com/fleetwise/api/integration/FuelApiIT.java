package com.fleetwise.api.integration;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class FuelApiIT extends BaseIntegrationTest {

    @Test
    void createAndListFuelLogs() {
        UUID fleetId = createFleet("Fuel Fleet");
        UUID vehicleId = createVehicle(fleetId);

        given()
                .header("Authorization", bearer())
                .contentType(ContentType.JSON)
                .body("""
                {
                  "fuelDate": "2026-05-01",
                  "mileage": 125500,
                  "gallons": 12.5,
                  "totalCost": 45.75,
                  "pricePerGallon": 3.66
                }
                """)
                .when()
                .post("/api/vehicles/{vehicleId}/fuel-logs", vehicleId)
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .body("gallons", comparesEqualTo(12.5f))
                .body("totalCost", comparesEqualTo(45.75f));

        given()
                .header("Authorization", bearer())
                .when()
                .get("/api/vehicles/{vehicleId}/fuel-logs", vehicleId)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));

        given()
                .header("Authorization", bearer())
                .when()
                .get("/api/fleets/{fleetId}/fuel-logs", fleetId)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
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
                          "vin":"1ZVBP8AM8E5320002",
                          "make":"Ford",
                          "model":"F-150",
                          "year":2021,
                          "licensePlate":"FUEL123",
                          "currentMileage":50000
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