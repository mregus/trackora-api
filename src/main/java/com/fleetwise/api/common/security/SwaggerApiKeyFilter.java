package com.fleetwise.api.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class SwaggerApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-SWAGGER-API-KEY";

    @Value("${swagger.api-key}")
    private String swaggerApiKey;

    private static final List<String> SWAGGER_PATHS = List.of(
            "/swagger-ui",
            "/swagger-ui.html",
            "/v3/api-docs"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return SWAGGER_PATHS.stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String providedKey = request.getHeader(HEADER_NAME);

        if (swaggerApiKey.equals(providedKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("""
                {
                  "error": "Unauthorized",
                  "message": "Invalid Swagger API key"
                }
                """);
    }
}