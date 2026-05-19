package com.fleetwise.api.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.active=test")
public abstract class BaseIntegrationTest {

    @LocalServerPort
    int port;

    protected String jwtToken;
    protected String email;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // create a fresh test user each run
        email = "it_user_" + System.currentTimeMillis() + "@fleetwise.com";
        String password = "Password123!";

        // register the user
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                  "firstName":"IT",
                  "lastName":"Tester",
                  "email":"%s",
                  "password":"%s"
                }
                """.formatted(email, password))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200);

        // login and capture JWT
        jwtToken =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                    {"email":"%s","password":"%s"}
                    """.formatted(email, password))
                        .when()
                        .post("/api/auth/login")
                        .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getString("token");
    }

    /** Convenience header builder */
    protected String bearer() {
        return "Bearer " + jwtToken;
    }

    protected String registerAndLogin(String email) {
        String password = "Password123!";

        // register
        given()
                .contentType(ContentType.JSON)
                .body("""
            {
              "firstName":"IT",
              "lastName":"Tester",
              "email":"%s",
              "password":"%s"
            }
            """.formatted(email, password))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200);

        // login
        return given()
                .contentType(ContentType.JSON)
                .body("""
            {
              "email":"%s",
              "password":"%s"
            }
            """.formatted(email, password))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("token");
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
