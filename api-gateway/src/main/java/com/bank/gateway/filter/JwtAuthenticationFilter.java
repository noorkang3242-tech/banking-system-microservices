package com.bank.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * The single trusted edge of the system. On every request it:
 *   1. strips any client-supplied identity / internal-secret headers (anti-spoof),
 *   2. stamps the shared internal secret so downstream services know the call came
 *      through the gateway (they reject anything lacking it),
 *   3. propagates an X-Request-Id for cross-service tracing,
 *   4. on protected paths, validates the JWT and injects X-User-Id/Email/Role.
 * IMPORTANT: jwt.secret here MUST match auth-service; internal.shared-secret MUST
 * match every downstream service.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String SECRET_HEADER = "X-Internal-Secret";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final SecretKey key;
    private final String internalSecret;

    private static final List<String> OPEN_PATHS = List.of(
            "/api/auth/",
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator"
    );

    public JwtAuthenticationFilter(
            @Value("${jwt.secret}") String secret,
            @Value("${internal.shared-secret:local-dev-internal-secret-change-me}") String internalSecret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.internalSecret = internalSecret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // CORS preflight: let OPTIONS pass without auth so the browser receives the
        // CORS headers (otherwise the gateway would 401 the preflight and block the UI).
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String incomingId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        final String requestId = (incomingId == null || incomingId.isBlank())
                ? UUID.randomUUID().toString() : incomingId;

        if (isOpen(path)) {
            ServerHttpRequest openReq = request.mutate()
                    .headers(h -> {
                        // Even on open paths, never forward client-forged identity headers.
                        h.remove("X-User-Id");
                        h.remove("X-User-Email");
                        h.remove("X-User-Role");
                        h.set(SECRET_HEADER, internalSecret);
                        h.set(REQUEST_ID_HEADER, requestId);
                    })
                    .build();
            return chain.filter(exchange.mutate().request(openReq).build());
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            ServerHttpRequest mutated = request.mutate()
                    .headers(h -> {
                        h.set("X-User-Id", String.valueOf(claims.get("userId")));
                        h.set("X-User-Email", String.valueOf(claims.get("email")));
                        h.set("X-User-Role", String.valueOf(claims.get("role")));
                        h.set(SECRET_HEADER, internalSecret);
                        h.set(REQUEST_ID_HEADER, requestId);
                    })
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception e) {
            return unauthorized(exchange, "Invalid or expired token");
        }
    }

    private boolean isOpen(String path) {
        return OPEN_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        String body = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
