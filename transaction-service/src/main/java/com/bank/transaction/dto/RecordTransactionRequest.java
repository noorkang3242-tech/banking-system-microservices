package com.bank.transaction.dto;

import com.bank.transaction.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Sent by account-service / transfer-service (or staff) to record a transaction.
 * The owner's userId comes from the X-User-Id header, not this body.
 */
public record RecordTransactionRequest(

        @NotBlank(message = "accountNumber is required")
        String accountNumber,

        @NotNull(message = "type is required (DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT)")
        TransactionType type,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than 0")
        BigDecimal amount,

        BigDecimal balanceAfter,

        String counterpartyAccount,

        String description
) {}
