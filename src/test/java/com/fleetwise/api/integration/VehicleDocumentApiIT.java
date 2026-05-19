package com.fleetwise.api.integration;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class VehicleDocumentApiIT extends BaseIntegrationTest {

    @Test
    void uploadListDownloadDeleteVehicleDocument() throws Exception {
        UUID fleetId = createFleet("Docs Fleet");
        UUID vehicleId = createVehicle(fleetId);

        File file = File.createTempFile("registration-", ".txt");

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Test registration document");
        }

        String documentId =
                given()
                        .header("Authorization", bearer())
                        .multiPart("documentType", "REGISTRATION")
                        .multiPart("file", file, "text/plain")
                        .when()
                        .post("/api/vehicles/{vehicleId}/documents", vehicleId)
                        .then()
                        .statusCode(anyOf(is(200), is(201)))
                        .body("id", notNullValue())
                        .body("vehicleId", equalTo(vehicleId.toString()))
                        .body("documentType", equalTo("REGISTRATION"))
                        .body("originalFileName", not(emptyString()))
                        .extract()
                        .path("id");

        given()
                .header("Authorization", bearer())
                .when()
                .get("/api/vehicles/{vehicleId}/documents", vehicleId)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("id", hasItem(documentId));

        given()
                .header("Authorization", bearer())
                .when()
                .get("/api/vehicle-documents/{documentId}/download", documentId)
                .then()
                .statusCode(200)
                .contentType(containsString("text/plain"))
                .body(containsString("Test registration document"));

        given()
                .header("Authorization", bearer())
                .when()
                .delete("/api/vehicle-documents/{documentId}", documentId)
                .then()
                .statusCode(anyOf(is(200), is(204)));

        given()
                .header("Authorization", bearer())
                .when()
                .get("/api/vehicles/{vehicleId}/documents", vehicleId)
                .then()
                .statusCode(200)
                .body("id", not(hasItem(documentId)));

        file.delete();
    }

    @Test
    void uploadPhotoWithNonImageFile_ShouldReturnBadRequest() throws Exception {
        UUID fleetId = createFleet("Photo Validation Fleet");
        UUID vehicleId = createVehicle(fleetId);

        File file = File.createTempFile("not-image-", ".txt");

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("This is not an image");
        }

        given()
                .header("Authorization", bearer())
                .multiPart("documentType", "PHOTO")
                .multiPart("file", file, "text/plain")
                .when()
                .post("/api/vehicles/{vehicleId}/documents", vehicleId)
                .then()
                .statusCode(anyOf(is(400), is(422)));

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
                          "vin":"1FTFW1E50MFA00099",
                          "make":"Ford",
                          "model":"F-150",
                          "year":2021,
                          "licensePlate":"DOC123",
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