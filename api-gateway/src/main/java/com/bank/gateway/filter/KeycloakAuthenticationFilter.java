package com.bank.gateway.filter;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Keycloak (OIDC) edge auth. Active only when gateway.auth-mode=keycloak (cloud).
 * Validates the realm's RS256 access token against the JWKS, then injects
 * X-User-Id/Email/Role for downstream services (same contract as the JWT mode).
 */
@Component
@ConditionalOnProperty(name = "gateway.auth-mode", havingValue = "keycloak")
public class KeycloakAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAuthenticationFilter.class);

    private static final String SECRET_HEADER = "X-Internal-Secret";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final List<String> OPEN_PATHS = List.of("/swagger-ui", "/v3/api-docs", "/actuator");

    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    private final String internalSecret;

    public KeycloakAuthenticationFilter(
            @Value("${keycloak.issuer-uri}") String issuerUri,
            @Value("${keycloak.jwks-uri}") String jwksUri,
            @Value("${internal.shared-secret:local-dev-internal-secret-change-me}") String internalSecret) throws Exception {
        this.internalSecret = internalSecret;
        JWKSource<SecurityContext> jwkSource = JWKSourceBuilder.create(new URI(jwksUri).toURL()).build();
        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource));
        processor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                new JWTClaimsSet.Builder().issuer(issuerUri).build(),
                Set.of("sub", "exp")));
        this.jwtProcessor = processor;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String incomingId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        final String requestId = (incomingId == null || incomingId.isBlank())
                ? UUID.randomUUID().toString() : incomingId;

        if (isOpen(path)) {
            ServerHttpRequest openReq = request.mutate()
                    .headers(h -> { stripIdentity(h); h.set(SECRET_HEADER, internalSecret); h.set(REQUEST_ID_HEADER, requestId); })
                    .build();
            return chain.filter(exchange.mutate().request(openReq).build());
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);

        // Validate off the event loop; catch validation error HERE so downstream
        // 5xx/routing errors are NOT misreported as 401.
        return Mono.fromCallable(() -> {
                    try {
                        return Optional.of(jwtProcessor.process(token, null));
                    } catch (Exception e) {
                        log.warn("KEYCLOAK_TOKEN_REJECTED {}: {}", e.getClass().getSimpleName(), e.getMessage());
                        return Optional.<JWTClaimsSet>empty();
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return unauthorized(exchange, "Invalid or expired token");
                    }
                    JWTClaimsSet claims = opt.get();
                    String userId = claims.getSubject();
                    String email = stringClaim(claims, "email");
                    if (email == null) email = stringClaim(claims, "preferred_username");
                    final String fEmail = email == null ? "" : email;
                    final String role = extractRole(claims);

                    ServerHttpRequest mutated = request.mutate()
                            .headers(h -> {
                                stripIdentity(h);
                                h.set("X-User-Id", userId);
                                h.set("X-User-Email", fEmail);
                                h.set("X-User-Role", role);
                                h.set(SECRET_HEADER, internalSecret);
                                h.set(REQUEST_ID_HEADER, requestId);
                            })
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                });
    }

    private static void stripIdentity(HttpHeaders h) {
        h.remove("X-User-Id");
        h.remove("X-User-Email");
        h.remove("X-User-Role");
        h.remove(SECRET_HEADER);
    }

    private static String stringClaim(JWTClaimsSet c, String name) {
        try { return c.getStringClaim(name); } catch (Exception e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private static String extractRole(JWTClaimsSet claims) {
        Object realmAccess = claims.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> m && m.get("roles") instanceof List<?> list) {
            List<String> roles = (List<String>) (List<?>) list;
            if (roles.contains("ADMIN")) return "ADMIN";
            if (roles.contains("MANAGER")) return "MANAGER";
        }
        return "CUSTOMER";
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
