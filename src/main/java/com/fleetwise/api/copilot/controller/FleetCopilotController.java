package com.fleetwise.api.copilot.controller;

import com.fleetwise.api.auth.security.UserPrincipal;
import com.fleetwise.api.copilot.dto.*;
import com.fleetwise.api.copilot.service.FleetCopilotConversationService;
import com.fleetwise.api.copilot.service.FleetCopilotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fleets/{fleetId}/copilot")
public class FleetCopilotController {

    private final FleetCopilotService fleetCopilotService;
    private final FleetCopilotConversationService conversationService;

    @PostMapping("/ask")
    public FleetCopilotResponse ask(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId,
            @Valid @RequestBody FleetCopilotRequest request
    ) {
        return fleetCopilotService.ask(
                principal.getId(),
                fleetId,
                request.conversationId(),
                request.question()
        );
    }

    @GetMapping("/conversations")
    public List<CopilotConversationSummaryResponse> getConversations(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId
    ) {
        return conversationService.getConversations(
                fleetId,
                principal.getId()
        );
    }

    @GetMapping("/conversations/{conversationId}")
    public CopilotConversationDetailResponse getConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId,
            @PathVariable UUID conversationId
    ) {
        return conversationService.getConversation(
                conversationId,
                fleetId,
                principal.getId()
        );
    }

    @PatchMapping("/conversations/{conversationId}")
    public CopilotConversationSummaryResponse renameConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId,
            @PathVariable UUID conversationId,
            @Valid @RequestBody RenameCopilotConversationRequest request
    ) {
        return conversationService.renameConversation(
                conversationId,
                fleetId,
                principal.getId(),
                request.title()
        );
    }

    @DeleteMapping("/conversations/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId,
            @PathVariable UUID conversationId
    ) {
        conversationService.deleteConversation(
                conversationId,
                fleetId,
                principal.getId()
        );
    }
}