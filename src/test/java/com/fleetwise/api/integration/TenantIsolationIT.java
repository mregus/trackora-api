package com.fleetwise.api.integration;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class TenantIsolationIT extends BaseIntegrationTest {

    @Test
    void userB_ShouldNotAccessUserAFleetOrVehicle() {
        String userAToken = registerAndLogin("tenant_a_" + System.currentTimeMillis() + "@fleetwise.com");
        String userBToken = registerAndLogin("tenant_b_" + System.currentTimeMillis() + "@fleetwise.com");

        UUID userAFleetId = UUID.fromString(
                given()
                        .header("Authorization", bearer(userAToken))
                        .contentType(ContentType.JSON)
                        .body("{\"name\":\"User A Fleet\"}")
                        .when()
                        .post("/api/fleets")
                        .then()
                        .statusCode(anyOf(is(200), is(201)))
                        .extract()
                        .path("id")
        );

        UUID userAVehicleId = UUID.fromString(
                given()
                        .header("Authorization", bearer(userAToken))
                        .contentType(ContentType.JSON)
                        .body("""
                        {
                          "vin":"1ZVBP8AM8E5329999",
                          "make":"Ford",
                          "model":"Transit",
                          "year":2020,
                          "licensePlate":"TENANT1",
                          "currentMileage":10000
                        }
                        """)
                        .when()
                        .post("/api/fleets/{fleetId}/vehicles", userAFleetId)
                        .then()
                        .statusCode(anyOf(is(200), is(201)))
                        .extract()
                        .path("id")
        );

        given()
                .header("Authorization", bearer(userBToken))
                .when()
                .get("/api/fleets/{fleetId}", userAFleetId)
                .then()
                .statusCode(anyOf(is(400), is(404)));

        given()
                .header("Authorization", bearer(userBToken))
                .when()
                .get("/api/fleets/{fleetId}/vehicles", userAFleetId)
                .then()
                .statusCode(anyOf(is(400), is(404), is(403)));

        given()
                .header("Authorization", bearer(userBToken))
                .when()
                .get("/api/vehicles/{vehicleId}", userAVehicleId)
                .then()
                .statusCode(anyOf(is(400), is(404)));

        given()
                .header("Authorization", bearer(userBToken))
                .when()
                .get("/api/fleets/{fleetId}/dashboard/summary", userAFleetId)
                .then()
                .statusCode(anyOf(is(400), is(404), is(403)));
    }
}