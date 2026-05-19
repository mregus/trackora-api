package com.fleetwise.api.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import java.io.IOException;
import java.util.UUID;

public class RequestIdFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            MDC.put("traceId", UUID.randomUUID().toString());
            chain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
        }
    }
}
