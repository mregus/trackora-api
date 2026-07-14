package com.fleetwise.api.copilot;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

class FleetCopilotApiIT extends com.fleetwise.api.integration.BaseIntegrationTest {

    @Test
    void shouldCreateAndContinueConversation() {
        UUID fleetId = createFleet("Copilot Test Fleet");

        String firstQuestion =
                "What maintenance should I prioritize?";

        String conversationId =
                given()
                        .header("Authorization", bearer())
                        .contentType(ContentType.JSON)
                        .body(Map.of(
                                "question", firstQuestion
                        ))
                        .when()
                        .post(
                                "/api/fleets/{fleetId}/copilot/ask",
                                fleetId
                        )
                        .then()
                        .statusCode(200)
                        .body("conversationId", notNullValue())
                        .body("answer", not(blankOrNullString()))
                        .body("aiGenerated", equalTo(false))
                        .body("supportingFacts", notNullValue())
                        .extract()
                        .path("conversationId");

        assertThat(conversationId).isNotBlank();

        String secondQuestion =
                "How many vehicles are offline?";

        given()
                .header("Authorization", bearer())
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "question", secondQuestion,
                        "conversationId", conversationId
                ))
                .when()
                .post(
                        "/api/fleets/{fleetId}/copilot/ask",
                        fleetId
                )
                .then()
                .statusCode(200)
                .body(
                        "conversationId",
                        equalTo(conversationId)
                )
                .body("answer", not(blankOrNullString()))
                .body("aiGenerated", equalTo(false));

        given()
                .header("Authorization", bearer())
                .when()
                .get(
                        "/api/fleets/{fleetId}/copilot/conversations/{conversationId}",
                        fleetId,
                        conversationId
                )
                .then()
                .statusCode(200)
                .body("id", equalTo(conversationId))
                .body("messages", hasSize(4))
                .body(
                        "messages[0].role",
                        equalTo("USER")
                )
                .body(
                        "messages[0].content",
                        equalTo(firstQuestion)
                )
                .body(
                        "messages[1].role",
                        equalTo("ASSISTANT")
                )
                .body(
                        "messages[2].role",
                        equalTo("USER")
                )
                .body(
                        "messages[2].content",
                        equalTo(secondQuestion)
                )
                .body(
                        "messages[3].role",
                        equalTo("ASSISTANT")
                );
    }

    @Test
    void shouldListUsersConversations() {
        UUID fleetId = createFleet("Conversation List Fleet");

        String conversationId =
                given()
                        .header("Authorization", bearer())
                        .contentType(ContentType.JSON)
                        .body(Map.of(
                                "question",
                                "Give me a fleet overview"
                        ))
                        .when()
                        .post(
                                "/api/fleets/{fleetId}/copilot/ask",
                                fleetId
                        )
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("conversationId");

        given()
                .header("Authorization", bearer())
                .when()
                .get(
                        "/api/fleets/{fleetId}/copilot/conversations",
                        fleetId
                )
                .then()
                .statusCode(200)
                .body("id", hasItem(conversationId))
                .body(
                        "find { it.id == '%s' }.title"
                                .formatted(conversationId),
                        equalTo("Give me a fleet overview")
                );
    }

    @Test
    void shouldRenameAndDeleteConversation() {
        UUID fleetId = createFleet("Rename Copilot Fleet");

        String conversationId =
                given()
                        .header("Authorization", bearer())
                        .contentType(ContentType.JSON)
                        .body(Map.of(
                                "question",
                                "What alerts need attention?"
                        ))
                        .when()
                        .post(
                                "/api/fleets/{fleetId}/copilot/ask",
                                fleetId
                        )
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("conversationId");

        given()
                .header("Authorization", bearer())
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title",
                        "Critical alert review"
                ))
                .when()
                .patch(
                        "/api/fleets/{fleetId}/copilot/conversations/{conversationId}",
                        fleetId,
                        conversationId
                )
                .then()
                .statusCode(200)
                .body(
                        "title",
                        equalTo("Critical alert review")
                );

        given()
                .header("Authorization", bearer())
                .when()
                .delete(
                        "/api/fleets/{fleetId}/copilot/conversations/{conversationId}",
                        fleetId,
                        conversationId
                )
                .then()
                .statusCode(204);

        given()
                .header("Authorization", bearer())
                .when()
                .get(
                        "/api/fleets/{fleetId}/copilot/conversations/{conversationId}",
                        fleetId,
                        conversationId
                )
                .then()
                .statusCode(404);
    }

    @Test
    void shouldPreventAnotherUserFromReadingConversation() {
        UUID fleetId = createFleet("Private Copilot Fleet");

        String conversationId =
                given()
                        .header("Authorization", bearer())
                        .contentType(ContentType.JSON)
                        .body(Map.of(
                                "question",
                                "Give me a private fleet overview"
                        ))
                        .when()
                        .post(
                                "/api/fleets/{fleetId}/copilot/ask",
                                fleetId
                        )
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("conversationId");

        String otherUserBearer = createAnotherUserBearer();

        given()
                .header(
                        "Authorization",
                        otherUserBearer
                )
                .when()
                .get(
                        "/api/fleets/{fleetId}/copilot/conversations/{conversationId}",
                        fleetId,
                        conversationId
                )
                .then()
                .statusCode(anyOf(
                        equalTo(403),
                        equalTo(404)
                ));
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
}