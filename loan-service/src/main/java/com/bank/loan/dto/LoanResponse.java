package com.bank.loan.dto;

import com.bank.loan.entity.Loan;
import com.bank.loan.entity.LoanStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record LoanResponse(
        String loanId,
        String userId,
        String accountNumber,
        BigDecimal principal,
        BigDecimal interestRate,
        Integer termMonths,
        BigDecimal totalPayable,
        BigDecimal outstanding,
        String purpose,
        LoanStatus status,
        String decisionReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static LoanResponse from(Loan l) {
        return new LoanResponse(
                l.getLoanId(),
                l.getUserId(),
                l.getAccountNumber(),
                l.getPrincipal(),
                l.getInterestRate(),
                l.getTermMonths(),
                l.getTotalPayable(),
                l.getOutstanding(),
                l.getPurpose(),
                l.getStatus(),
                l.getDecisionReason(),
                l.getCreatedAt(),
                l.getUpdatedAt()
        );
    }
}
