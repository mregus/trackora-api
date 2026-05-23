package com.fleetwise.api.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", 403,
                        "error", "Forbidden",
                        "message", "You do not have permission to perform this action",
                        "path", request.getRequestURI()
                ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", 401,
                        "error", "Unauthorized",
                        "message", "Invalid email or password",
                        "path", request.getRequestURI()
                ));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthorizationDenied(
            AuthorizationDeniedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", 403,
                        "error", "Forbidden",
                        "message", "You do not have permission to perform this action",
                        "path", request.getRequestURI()
                ));
    }
}