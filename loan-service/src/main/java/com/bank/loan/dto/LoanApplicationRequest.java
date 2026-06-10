package com.bank.loan.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LoanApplicationRequest(

        @NotBlank(message = "accountNumber is required (where the loan is disbursed/repaid)")
        String accountNumber,

        @NotNull(message = "principal is required")
        @DecimalMin(value = "1000.00", message = "principal must be at least 1000")
        BigDecimal principal,

        @NotNull(message = "termMonths is required")
        @Min(value = 1, message = "termMonths must be at least 1")
        @Max(value = 360, message = "termMonths cannot exceed 360")
        Integer termMonths,

        /** Optional annual rate in percent; defaults to 10.00 if omitted. */
        @DecimalMin(value = "0.00", message = "interestRate cannot be negative")
        @DecimalMax(value = "100.00", message = "interestRate cannot exceed 100")
        BigDecimal interestRate,

        String purpose
) {}
