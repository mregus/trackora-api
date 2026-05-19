package com.fleetwise.api.integration;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import java.util.UUID;

class VehicleApiIT extends BaseIntegrationTest {

    @Test
    void createAndListVehicles() {
        // 1. Create a Fleet (returns real ID tied to the authenticated user)
        UUID fleetId =
                UUID.fromString(
                        given()
                                .header("Authorization", bearer())
                                .contentType(ContentType.JSON)
                                .body("{\"name\":\"Integration Fleet\"}")
                                .when()
                                .post("/api/fleets")
                                .then()
                                .statusCode(anyOf(is(200), is(201)))
                                .extract()
                                .jsonPath()
                                .getString("id")
                );

        // 2. Create a Vehicle for that Fleet
        given()
                .header("Authorization", bearer())
                .contentType(ContentType.JSON)
                .body("""
                {
                  "vin":"1ZVBP8AM8E5320001",
                  "make":"Ford",
                  "model":"Transit",
                  "year":2020,
                  "licensePlate":"REST123",
                  "currentMileage":12345
                }
                """)
                .when()
                .post("/api/fleets/{fleetId}/vehicles", fleetId)
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .body("make", equalTo("Ford"));

        // 3. List vehicles for that Fleet
        given()
                .header("Authorization", bearer())
                .when()
                .get("/api/fleets/{fleetId}/vehicles", fleetId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", notNullValue());
    }

    @Test
    void getUpdateAndDeleteVehicle() {
        UUID fleetId = UUID.fromString(
                given()
                        .header("Authorization", bearer())
                        .contentType(ContentType.JSON)
                        .body("{\"name\":\"Vehicle CRUD Fleet\"}")
                        .when()
                        .post("/api/fleets")
                        .then()
                        .statusCode(anyOf(is(200), is(201)))
                        .extract()
                        .jsonPath()
                        .getString("id")
        );

        String vehicleId =
                given()
                        .header("Authorization", bearer())
                        .contentType(ContentType.JSON)
                        .body("""
                    {
                      "vin":"1ZVBP8AM8E5320001",
                      "make":"Ford",
                      "model":"Transit",
                      "year":2020,
                      "licensePlate":"REST123",
                      "currentMileage":12345
                    }
                    """)
                        .when()
                        .post("/api/fleets/{fleetId}/vehicles", fleetId)
                        .then()
                        .statusCode(anyOf(is(200), is(201)))
                        .extract()
                        .jsonPath()
                        .getString("id");

        given()
                .header("Authorization", bearer())
                .when()
                .get("/api/vehicles/{vehicleId}", vehicleId)
                .then()
                .statusCode(200)
                .body("id", equalTo(vehicleId))
                .body("make", equalTo("Ford"));

        given()
                .header("Authorization", bearer())
                .contentType(ContentType.JSON)
                .body("""
            {
              "vin":"1ZVBP8AM8E5320001",
              "make":"Ford",
              "model":"Transit Connect",
              "year":2020,
              "licensePlate":"REST999",
              "currentMileage":13000,
              "status":"IN_SHOP"
            }
            """)
                .when()
                .put("/api/vehicles/{vehicleId}", vehicleId)
                .then()
                .statusCode(200)
                .body("model", equalTo("Transit Connect"))
                .body("licensePlate", equalTo("REST999"))
                .body("status", equalTo("IN_SHOP"));

        given()
                .header("Authorization", bearer())
                .when()
                .delete("/api/vehicles/{vehicleId}", vehicleId)
                .then()
                .statusCode(anyOf(is(200), is(204)));

        given()
                .header("Authorization", bearer())
                .when()
                .get("/api/vehicles/{vehicleId}", vehicleId)
                .then()
                .statusCode(anyOf(is(400), is(404)));
    }
}
