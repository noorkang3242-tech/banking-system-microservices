package com.bank.loan.entity;

public enum LoanStatus {
    PENDING,    // applied, awaiting staff decision
    REJECTED,   // staff rejected
    ACTIVE,     // approved + disbursed, being repaid
    CLOSED      // fully repaid
}
