package com.bank.loan.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Enforces that every API call arrived through the API gateway (or a trusted
 * internal service) by requiring a shared-secret header. A request that hits this
 * service port directly without the secret is rejected with 401 — this closes the
 * "services blindly trust X-User-* headers, so bypass the gateway and forge them"
 * gap. Health/docs endpoints are exempt so probes and Swagger keep working.
 * Also stamps an X-Request-Id into the logging MDC for cross-service tracing.
 */
@Component
@Order(1)
public class InternalRequestFilter extends OncePerRequestFilter {

    public static final String SECRET_HEADER = "X-Internal-Secret";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final String expectedSecret;

    public InternalRequestFilter(@Value("${internal.shared-secret:}") String expectedSecret) {
        this.expectedSecret = expectedSecret;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getRequestURI();
        return p.startsWith("/actuator")
                || p.startsWith("/swagger-ui")
                || p.startsWith("/v3/api-docs")
                || p.equals("/swagger-ui.html");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(SECRET_HEADER);
        if (expectedSecret == null || expectedSecret.isBlank() || !expectedSecret.equals(provided)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Direct access not allowed; route through the API gateway\"}");
            return;
        }
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put("requestId", requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
        }
    }
}
