package com.fleetwise.api.integration;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class MaintenanceDocumentApiIT extends BaseIntegrationTest {

    @Test
    void uploadListDownloadDeleteMaintenanceDocument() throws Exception {
        UUID fleetId = createFleet("Maintenance Docs Fleet");
        UUID vehicleId = createVehicle(fleetId);
        String maintenanceId = createMaintenance(vehicleId);

        File file = File.createTempFile("maintenance-invoice-", ".txt");

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Test maintenance invoice");
        }

        String documentId =
                given()
                        .header("Authorization", bearer())
                        .multiPart("documentType", "MAINTENANCE_INVOICE")
                        .multiPart("file", file, "text/plain")
                        .when()
                        .post("/api/maintenance/{maintenanceId}/documents", maintenanceId)
                        .then()
                        .statusCode(anyOf(is(200), is(201)))
                        .body("id", notNullValue())
                        .body("vehicleId", equalTo(vehicleId.toString()))
                        .body("maintenanceId", equalTo(maintenanceId))
                        .body("documentType", equalTo("MAINTENANCE_INVOICE"))
                        .extract()
                        .path("id");

        given()
                .header("Authorization", bearer())
                .when()
                .get("/api/maintenance/{maintenanceId}/documents", maintenanceId)
                .then()
                .statusCode(200)
                .body("id", hasItem(documentId));

        given()
                .header("Authorization", bearer())
                .when()
                .get("/api/vehicle-documents/{documentId}/download", documentId)
                .then()
                .statusCode(200)
                .contentType(containsString("text/plain"))
                .body(containsString("Test maintenance invoice"));

        given()
                .header("Authorization", bearer())
                .when()
                .delete("/api/vehicle-documents/{documentId}", documentId)
                .then()
                .statusCode(anyOf(is(200), is(204)));

        given()
                .header("Authorization", bearer())
                .when()
                .get("/api/maintenance/{maintenanceId}/documents", maintenanceId)
                .then()
                .statusCode(200)
                .body("id", not(hasItem(documentId)));

        file.delete();
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
                          "vin":"1FTFW1E50MFA00101",
                          "make":"Ford",
                          "model":"F-150",
                          "year":2021,
                          "licensePlate":"MDOC123",
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

    private String createMaintenance(UUID vehicleId) {
        return given()
                .header("Authorization", bearer())
                .contentType(ContentType.JSON)
                .body("""
                {
                  "serviceType": "OIL_CHANGE",
                  "serviceDate": "2026-05-15",
                  "mileage": 50300,
                  "cost": 99.99,
                  "vendor": "Test Shop",
                  "description": "Oil change invoice test"
                }
                """)
                .when()
                .post("/api/vehicles/{vehicleId}/maintenance", vehicleId)
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .extract()
                .path("id");
    }
}