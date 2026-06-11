package com.bank.account.dto;

/**
 * Safe, limited view of an account for beneficiary verification before a transfer.
 * Deliberately NO balance — only enough to confirm you are sending to the right person.
 */
public record BeneficiaryResponse(
        String accountNumber,
        String accountType,
        String status,
        String holderName
) {}
