package com.bank.account.dto;

import com.bank.account.entity.Account;
import com.bank.account.entity.AccountStatus;
import com.bank.account.entity.AccountType;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        String accountNumber,
        String userId,
        AccountType accountType,
        BigDecimal balance,
        String currency,
        AccountStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(
                a.getAccountNumber(),
                a.getUserId(),
                a.getAccountType(),
                a.getBalance(),
                a.getCurrency(),
                a.getStatus(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
