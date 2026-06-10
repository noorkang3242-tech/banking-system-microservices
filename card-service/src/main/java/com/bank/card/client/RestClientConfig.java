package com.bank.card.client;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Load-balanced RestClient.Builder (http://SERVICE-NAME/... resolves via Eureka),
 * hardened for production:
 *  - connect/read timeouts so a slow or hung downstream can't pin our threads;
 *  - every outbound call carries the internal shared secret (so the callee's
 *    InternalRequestFilter accepts it) and propagates the current X-Request-Id.
 */
@Configuration
public class RestClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder(
            @Value("${internal.shared-secret:local-dev-internal-secret-change-me}") String internalSecret) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));

        return RestClient.builder()
                .requestFactory(factory)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().set("X-Internal-Secret", internalSecret);
                    String requestId = MDC.get("requestId");
                    if (requestId != null && !requestId.isBlank()) {
                        request.getHeaders().set("X-Request-Id", requestId);
                    }
                    return execution.execute(request, body);
                });
    }
}
