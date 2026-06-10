package com.bank.account.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/**
 * Calls transaction-service (via Eureka) to record a transaction whenever money moves.
 * Best-effort: if transaction-service is down, the deposit/withdraw still succeeds — we
 * only log a warning, never fail the banking operation.
 */
@Component
public class TransactionClient {

    private static final Logger log = LoggerFactory.getLogger(TransactionClient.class);

    private final RestClient restClient;

    public TransactionClient(RestClient.Builder loadBalancedRestClientBuilder) {
        this.restClient = loadBalancedRestClientBuilder.build();
    }

    public void record(String userId, String accountNumber, String type,
                       BigDecimal amount, BigDecimal balanceAfter, String description) {
        try {
            restClient.post()
                    .uri("http://TRANSACTION-SERVICE/api/transactions/record")
                    .header("X-User-Id", userId)
                    .body(new TxnPayload(accountNumber, type, amount, balanceAfter, description))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Could not record transaction (best-effort) for account {}: {}", accountNumber, e.getMessage());
        }
    }

    /** Matches transaction-service RecordTransactionRequest (counterparty omitted). */
    public record TxnPayload(String accountNumber, String type, BigDecimal amount,
                             BigDecimal balanceAfter, String description) {}
}
