package com.fleetwise.api.search.controller;

import com.fleetwise.api.auth.security.UserPrincipal;
import com.fleetwise.api.search.dto.SearchResponse;
import com.fleetwise.api.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public SearchResponse search(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String query
    ) {
        return searchService.search(principal.getId(), query);
    }
}