package com.fleetwise.api.integration;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class DashboardApiIT extends BaseIntegrationTest {

    @Test
    void getDashboardSummary_ShouldReturnAggregates() {
        UUID fleetId = createFleet("Dashboard Fleet");
        UUID vehicleId = createVehicle(fleetId);

        given()
                .header("Authorization", bearer())
                .contentType(ContentType.JSON)
                .body("""
                {
                  "serviceType": "OIL_CHANGE",
                  "serviceDate": "2026-05-01",
                  "mileage": 125000,
                  "cost": 100.00,
                  "vendorName": "Test Shop",
                  "notes": "Oil change"
                }
                """)
                .when()
                .post("/api/vehicles/{vehicleId}/maintenance", vehicleId)
                .then()
                .statusCode(anyOf(is(200), is(201)));

        given()
                .header("Authorization", bearer())
                .contentType(ContentType.JSON)
                .body("""
                {
                  "fuelDate": "2026-05-01",
                  "mileage": 125500,
                  "gallons": 10.0,
                  "totalCost": 40.00,
                  "pricePerGallon": 4.00
                }
                """)
                .when()
                .post("/api/vehicles/{vehicleId}/fuel-logs", vehicleId)
                .then()
                .statusCode(anyOf(is(200), is(201)));

        given()
                .header("Authorization", bearer())
                .when()
                .get("/api/fleets/{fleetId}/dashboard/summary", fleetId)
                .then()
                .statusCode(200)
                .body("fleetId", equalTo(fleetId.toString()))
                .body("fleetName", equalTo("Dashboard Fleet"))
                .body("totalVehicles", greaterThanOrEqualTo(1))
                .body("monthlyMaintenanceCost", notNullValue())
                .body("monthlyFuelCost", notNullValue());
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
                          "vin":"1ZVBP8AM8E5320003",
                          "make":"Ford",
                          "model":"Transit",
                          "year":2022,
                          "licensePlate":"DASH123",
                          "currentMileage":75000
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