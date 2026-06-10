package com.bank.transfer.client;

import com.bank.transfer.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Talks to account-service (via Eureka) for the transfer saga.
 * account-service trusts the X-User-Id / X-User-Role headers we pass (same as the gateway).
 * Internal reads/credits use role MANAGER so the destination-owner check passes.
 */
@Component
public class AccountClient {

    private static final String BASE = "http://ACCOUNT-SERVICE/api/accounts";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AccountClient(RestClient.Builder loadBalancedRestClientBuilder, ObjectMapper objectMapper) {
        this.restClient = loadBalancedRestClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public AccountInfo getAccount(String accountNumber, String userId, String role) {
        try {
            return restClient.get()
                    .uri(BASE + "/" + accountNumber)
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .retrieve()
                    .body(AccountInfo.class);
        } catch (HttpStatusCodeException e) {
            throw mapError(e);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "account-service unreachable: " + e.getMessage());
        }
    }

    public AccountInfo withdraw(String accountNumber, BigDecimal amount, String userId, String role) {
        return money(accountNumber + "/withdraw", amount, userId, role);
    }

    public AccountInfo deposit(String accountNumber, BigDecimal amount, String userId, String role) {
        return money(accountNumber + "/deposit", amount, userId, role);
    }

    private AccountInfo money(String path, BigDecimal amount, String userId, String role) {
        try {
            return restClient.post()
                    .uri(BASE + "/" + path)
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .body(Map.of("amount", amount))
                    .retrieve()
                    .body(AccountInfo.class);
        } catch (HttpStatusCodeException e) {
            throw mapError(e);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "account-service unreachable: " + e.getMessage());
        }
    }

    /** Re-surface account-service's status + message so the transfer reports the real reason. */
    private ApiException mapError(HttpStatusCodeException e) {
        String message = e.getStatusText();
        try {
            var node = objectMapper.readTree(e.getResponseBodyAsString());
            if (node.has("message")) {
                message = node.get("message").asText();
            }
        } catch (Exception ignore) {
            // keep default message
        }
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        return new ApiException(status != null ? status : HttpStatus.BAD_GATEWAY, message);
    }
}
