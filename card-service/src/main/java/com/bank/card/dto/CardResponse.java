package com.bank.card.dto;

import com.bank.card.entity.Card;
import com.bank.card.entity.CardStatus;
import com.bank.card.entity.CardType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Safe view of a card: the PAN is masked (only the last 4 digits) and the CVV is
 * never included.
 */
public record CardResponse(
        String cardId,
        String maskedNumber,
        String userId,
        String accountNumber,
        CardType cardType,
        String cardholderName,
        CardStatus status,
        LocalDate expiryDate,
        BigDecimal creditLimit,
        Instant createdAt,
        Instant updatedAt
) {
    public static CardResponse from(Card c) {
        return new CardResponse(
                c.getCardId(),
                mask(c.getCardNumber()),
                c.getUserId(),
                c.getAccountNumber(),
                c.getCardType(),
                c.getCardholderName(),
                effectiveStatus(c),
                c.getExpiryDate(),
                c.getCreditLimit(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    /**
     * A card past its expiry date is reported as EXPIRED (unless explicitly BLOCKED),
     * even though no scheduler flips the stored status.
     */
    static CardStatus effectiveStatus(Card c) {
        if (c.getStatus() != CardStatus.BLOCKED
                && c.getExpiryDate() != null
                && c.getExpiryDate().isBefore(LocalDate.now())) {
            return CardStatus.EXPIRED;
        }
        return c.getStatus();
    }

    /** Show only the last 4 digits, e.g. "**** **** **** 1234". */
    static String mask(String pan) {
        if (pan == null || pan.length() < 4) {
            return "****";
        }
        String last4 = pan.substring(pan.length() - 4);
        return "**** **** **** " + last4;
    }
}
