package com.bank.transfer.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Subset of account-service's AccountResponse that transfer-service cares about.
 * Unknown fields are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountInfo(
        String accountNumber,
        String userId,
        BigDecimal balance,
        String status
) {}
