package com.bank.card.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountInfo(
        String accountNumber,
        String userId,
        BigDecimal balance,
        String status
) {}
