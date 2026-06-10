package com.bank.loan.client;

import com.bank.loan.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Talks to account-service (via Eureka) to disburse (deposit) and collect repayments (withdraw).
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

    public void deposit(String accountNumber, BigDecimal amount, String userId, String role) {
        money(accountNumber + "/deposit", amount, userId, role);
    }

    public void withdraw(String accountNumber, BigDecimal amount, String userId, String role) {
        money(accountNumber + "/withdraw", amount, userId, role);
    }

    private void money(String path, BigDecimal amount, String userId, String role) {
        try {
            restClient.post()
                    .uri(BASE + "/" + path)
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .body(Map.of("amount", amount))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            throw mapError(e);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "account-service unreachable: " + e.getMessage());
        }
    }

    private ApiException mapError(HttpStatusCodeException e) {
        String message = e.getStatusText();
        try {
            var node = objectMapper.readTree(e.getResponseBodyAsString());
            if (node.has("message")) {
                message = node.get("message").asText();
            }
        } catch (Exception ignore) {
            // keep default
        }
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        return new ApiException(status != null ? status : HttpStatus.BAD_GATEWAY, message);
    }
}
