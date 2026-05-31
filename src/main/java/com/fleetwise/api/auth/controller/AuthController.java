package com.fleetwise.api.auth.controller;

import com.fleetwise.api.auth.dto.*;
import com.fleetwise.api.auth.security.UserPrincipal;
import com.fleetwise.api.auth.security.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Auth", description = "Authentication and current user APIs")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request: {}", request);
        return authService.register(request);
    }

    @Operation(summary = "Login to account")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Inquiry user information")
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return authService.me(principal);
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authService.forgotPassword(request);

        return Map.of(
                "message",
                "If the account exists, a reset link has been sent."
        );
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);

        return Map.of("message", "Password has been reset successfully.");
    }
}