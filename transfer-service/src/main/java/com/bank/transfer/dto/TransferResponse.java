package com.bank.transfer.dto;

import com.bank.transfer.entity.Transfer;
import com.bank.transfer.entity.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        String transferId,
        String fromAccount,
        String toAccount,
        BigDecimal amount,
        String currency,
        TransferStatus status,
        String initiatedBy,
        String failureReason,
        Instant createdAt
) {
    public static TransferResponse from(Transfer t) {
        return new TransferResponse(
                t.getTransferId(),
                t.getFromAccount(),
                t.getToAccount(),
                t.getAmount(),
                t.getCurrency(),
                t.getStatus(),
                t.getInitiatedBy(),
                t.getFailureReason(),
                t.getCreatedAt()
        );
    }
}
