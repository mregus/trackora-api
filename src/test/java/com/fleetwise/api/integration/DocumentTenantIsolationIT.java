package com.fleetwise.api.integration;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class DocumentTenantIsolationIT extends BaseIntegrationTest {

    @Test
    void userCannotAccessAnotherUsersVehicleDocument() throws Exception {
        String userAToken = bearer();

        String userBToken = "Bearer " + registerAndLogin(
                "user_b_" + System.currentTimeMillis() + "@fleetwise.com"
        );

        UUID fleetId = createFleet(userAToken, "Tenant Docs Fleet");
        UUID vehicleId = createVehicle(userAToken, fleetId);

        File file = File.createTempFile("tenant-doc-", ".txt");

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Tenant private document");
        }

        String documentId =
                given()
                        .header("Authorization", userAToken)
                        .multiPart("documentType", "REGISTRATION")
                        .multiPart("file", file, "text/plain")
                        .when()
                        .post("/api/vehicles/{vehicleId}/documents", vehicleId)
                        .then()
                        .statusCode(anyOf(is(200), is(201)))
                        .extract()
                        .path("id");

        given()
                .header("Authorization", userBToken)
                .when()
                .get("/api/vehicle-documents/{documentId}/download", documentId)
                .then()
                .statusCode(anyOf(is(403), is(404)));

        given()
                .header("Authorization", userBToken)
                .when()
                .delete("/api/vehicle-documents/{documentId}", documentId)
                .then()
                .statusCode(anyOf(is(403), is(404)));

        file.delete();
    }

    private UUID createFleet(String bearerToken, String name) {
        return UUID.fromString(
                given()
                        .header("Authorization", bearerToken)
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

    private UUID createVehicle(String bearerToken, UUID fleetId) {
        return UUID.fromString(
                given()
                        .header("Authorization", bearerToken)
                        .contentType(ContentType.JSON)
                        .body("""
                        {
                          "vin":"1FTFW1E50MFA00999",
                          "make":"Ford",
                          "model":"F-150",
                          "year":2021,
                          "licensePlate":"TENANT1",
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