package com.bank.card.dto;

import com.bank.card.entity.Card;
import com.bank.card.entity.CardStatus;
import com.bank.card.entity.CardType;

import java.time.LocalDate;

/**
 * Returned ONLY once, immediately after issuing a card. This is the single time the
 * full card number and CVV are exposed (like receiving a physical card). The CVV is
 * not stored anywhere — it cannot be retrieved again.
 */
public record IssuedCardResponse(
        String cardId,
        String cardNumber,
        String cvv,
        String cardholderName,
        CardType cardType,
        CardStatus status,
        LocalDate expiryDate
) {
    public static IssuedCardResponse of(Card c, String cvv) {
        return new IssuedCardResponse(
                c.getCardId(),
                c.getCardNumber(),
                cvv,
                c.getCardholderName(),
                c.getCardType(),
                c.getStatus(),
                c.getExpiryDate()
        );
    }
}
