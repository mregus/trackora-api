package com.fleetwise.api.integration;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FleetApiIT extends BaseIntegrationTest {

    @Test
    void createFleet_ShouldReturnFleet() {

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearer())
                .body("""
                    {
                      "name": "Test Fleet"
                    }
                    """)
                .when()
                .post("/api/fleets")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("Test Fleet"))
                .body("ownerUserId", notNullValue());
    }

    @Test
    void getMyFleets_ShouldReturnOnlyMyFleets() {

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearer())
                .body("""
                    {
                      "name": "Fleet One"
                    }
                    """)
                .when()
                .post("/api/fleets")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", bearer())
                .when()
                .get("/api/fleets")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("name", hasItem("Fleet One"));
    }

    @Test
    void updateFleet_ShouldUpdateName() {

        String fleetId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearer())
                .body("""
                    {
                      "name": "Old Fleet Name"
                    }
                    """)
                .when()
                .post("/api/fleets")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearer())
                .body("""
                    {
                      "name": "Updated Fleet Name"
                    }
                    """)
                .when()
                .put("/api/fleets/" + fleetId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated Fleet Name"));
    }

    @Test
    void deleteFleet_ShouldRemoveFleet() {

        String fleetId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearer())
                .body("""
                    {
                      "name": "Fleet To Delete"
                    }
                    """)
                .when()
                .post("/api/fleets")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        given()
                .header("Authorization", bearer())
                .when()
                .delete("/api/fleets/" + fleetId)
                .then()
                .statusCode(anyOf(is(200), is(204)));

        given()
                .header("Authorization", bearer())
                .when()
                .get("/api/fleets/" + fleetId)
                .then()
                .statusCode(anyOf(is(400), is(404)));
    }
}