package com.bank.transaction.dto;

import com.bank.transaction.entity.Transaction;
import com.bank.transaction.entity.TransactionStatus;
import com.bank.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        String transactionId,
        String accountNumber,
        String userId,
        TransactionType type,
        BigDecimal amount,
        String currency,
        BigDecimal balanceAfter,
        String counterpartyAccount,
        String description,
        TransactionStatus status,
        Instant createdAt
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getTransactionId(),
                t.getAccountNumber(),
                t.getUserId(),
                t.getType(),
                t.getAmount(),
                t.getCurrency(),
                t.getBalanceAfter(),
                t.getCounterpartyAccount(),
                t.getDescription(),
                t.getStatus(),
                t.getCreatedAt()
        );
    }
}
