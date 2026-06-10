package com.bank.card.client;

import com.bank.card.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

/**
 * Used to validate that a DEBIT card's linked account exists and belongs to the caller.
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
