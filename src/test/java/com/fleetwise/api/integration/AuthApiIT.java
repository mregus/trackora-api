package com.fleetwise.api.integration;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthApiIT extends BaseIntegrationTest {

    @Test
    void registerAndLogin_ShouldReturnJwtToken() {
        String email = "it_test_" + System.currentTimeMillis() + "@fleetwise.com";

        // Register user
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "firstName": "Miguel",
                  "lastName": "Test",
                  "email": "%s",
                  "password": "Password123!"
                }
                """.formatted(email))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200)
                .body("token", not(emptyString()))
                .body("email", equalTo(email));

        // Login
        given()
                .contentType(ContentType.JSON)
                .body("""
                {"email":"%s","password":"Password123!"}
                """.formatted(email))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("token", not(emptyString()))
                .body("email", equalTo(email));
    }
}
