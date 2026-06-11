package com.bank.account.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Looks up a customer's name (via Eureka) so a transfer can show the beneficiary
 * before money is sent. Best-effort: if customer-service is down or there is no
 * profile, we just return null and the UI shows the account without a name.
 */
@Component
public class CustomerClient {

    private static final Logger log = LoggerFactory.getLogger(CustomerClient.class);

    private final RestClient restClient;

    public CustomerClient(RestClient.Builder loadBalancedRestClientBuilder) {
        this.restClient = loadBalancedRestClientBuilder.build();
    }

    public CustomerInfo getByUserId(String userId) {
        try {
            return restClient.get()
                    .uri("http://CUSTOMER-SERVICE/api/customers/" + userId)
                    .header("X-User-Role", "ADMIN") // internal staff-level read
                    .retrieve()
                    .body(CustomerInfo.class);
        } catch (Exception e) {
            log.warn("Could not fetch customer name for {}: {}", userId, e.getMessage());
            return null;
        }
    }
}
