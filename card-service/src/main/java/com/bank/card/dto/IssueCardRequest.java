package com.bank.card.dto;

import com.bank.card.entity.CardType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DEBIT  -> accountNumber required (must be the caller's own account), creditLimit ignored.
 * CREDIT -> accountNumber ignored, creditLimit optional (defaults applied in the service).
 * cardholderName optional; falls back to the caller's email.
 */
public record IssueCardRequest(

        @NotNull(message = "cardType is required (DEBIT or CREDIT)")
        CardType cardType,

        String accountNumber,

        @Size(max = 100)
        String cardholderName,

        @DecimalMin(value = "0.00", message = "creditLimit cannot be negative")
        BigDecimal creditLimit
) {}
