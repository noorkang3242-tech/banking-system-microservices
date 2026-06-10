package com.bank.transfer.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Best-effort delivery of notifications to notification-service (via Eureka).
 * Never fails the transfer if notification-service is unavailable.
 */
@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient restClient;

    public NotificationClient(RestClient.Builder loadBalancedRestClientBuilder) {
        this.restClient = loadBalancedRestClientBuilder.build();
    }

    public void send(String recipientUserId, String type, String title, String message) {
        try {
            restClient.post()
                    .uri("http://NOTIFICATION-SERVICE/api/notifications/send")
                    .header("X-User-Role", "ADMIN") // internal/trusted call
                    .body(new Payload(recipientUserId, type, title, message))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Could not send notification (best-effort) to {}: {}", recipientUserId, e.getMessage());
        }
    }

    public record Payload(String recipientUserId, String type, String title, String message) {}
}
