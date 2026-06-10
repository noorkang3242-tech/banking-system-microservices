package com.bank.account.dto;

import com.bank.account.entity.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OpenAccountRequest(

        @NotNull(message = "accountType is required (SAVINGS or CURRENT)")
        AccountType accountType,

        /** Optional opening deposit. Defaults to 0 if not provided. */
        @DecimalMin(value = "0.00", message = "Initial deposit cannot be negative")
        BigDecimal initialDeposit
) {}
