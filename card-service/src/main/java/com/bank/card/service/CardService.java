package com.bank.card.service;

import com.bank.card.client.AccountClient;
import com.bank.card.client.AccountInfo;
import com.bank.card.client.NotificationClient;
import com.bank.card.dto.CardResponse;
import com.bank.card.dto.IssueCardRequest;
import com.bank.card.dto.IssuedCardResponse;
import com.bank.card.entity.Card;
import com.bank.card.entity.CardStatus;
import com.bank.card.entity.CardType;
import com.bank.card.exception.ApiException;
import com.bank.card.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CardService {

    private static final Set<String> STAFF_ROLES = Set.of("MANAGER", "ADMIN");
    private static final BigDecimal DEFAULT_CREDIT_LIMIT = new BigDecimal("50000.00");
    private static final int VALIDITY_YEARS = 4;

    private final CardRepository repository;
    private final AccountClient accountClient;
    private final NotificationClient notificationClient;

    /** Issue a card. Returns the full PAN + CVV exactly once (never stored/returned again). */
    public IssuedCardResponse issue(String userId, String email, String role, IssueCardRequest req) {
        String accountNumber = null;
        BigDecimal creditLimit = null;

        if (req.cardType() == CardType.DEBIT) {
            if (req.accountNumber() == null || req.accountNumber().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "DEBIT cards require an accountNumber");
            }
            // The linked account must exist and belong to the caller. We deliberately use the
            // CUSTOMER role for this check so a staff caller can't link someone else's account,
            // and we compare the owner explicitly rather than trusting the lookup alone.
            AccountInfo acct;
            try {
                acct = accountClient.getAccount(req.accountNumber(), userId, "CUSTOMER");
            } catch (ApiException e) {
                // Only a genuine not-found / forbidden means "bad account"; a 502/5xx means
                // account-service is unavailable — surface that clearly instead of blaming the input.
                if (e.getStatus() == HttpStatus.NOT_FOUND || e.getStatus() == HttpStatus.FORBIDDEN) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "accountNumber not found or not yours: " + req.accountNumber());
                }
                throw e;
            }
            if (acct == null || !userId.equals(acct.userId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "accountNumber not yours: " + req.accountNumber());
            }
            accountNumber = req.accountNumber();
        } else { // CREDIT
            creditLimit = req.creditLimit() != null ? req.creditLimit() : DEFAULT_CREDIT_LIMIT;
        }

        String holder = (req.cardholderName() != null && !req.cardholderName().isBlank())
                ? req.cardholderName()
                : (email != null ? email : "CARDHOLDER");

        String cvv = generateCvv();

        Card card = Card.builder()
                .cardId(UUID.randomUUID().toString())
                .cardNumber(generateCardNumber())
                .userId(userId)
                .accountNumber(accountNumber)
                .cardType(req.cardType())
                .cardholderName(holder)
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now().plusYears(VALIDITY_YEARS))
                .creditLimit(creditLimit)
                .build();

        Card saved = repository.save(card);
        String last4 = saved.getCardNumber().substring(saved.getCardNumber().length() - 4);
        notificationClient.send(userId, "CARD", "Card issued",
                "Your " + saved.getCardType() + " card ending " + last4 + " is now active.");
        return IssuedCardResponse.of(saved, cvv); // cvv shown once, not persisted
    }

    public List<CardResponse> getMyCards(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(CardResponse::from).toList();
    }

    public CardResponse getOne(String cardId, String userId, String role) {
        Card card = findOrThrow(cardId);
        requireOwnerOrStaff(card, userId, role);
        return CardResponse.from(card);
    }

    public CardResponse block(String cardId, String userId, String role) {
        Card card = findOrThrow(cardId);
        requireOwnerOrStaff(card, userId, role);
        ensureNotExpired(card);
        card.setStatus(CardStatus.BLOCKED);
        return CardResponse.from(repository.save(card));
    }

    public CardResponse unblock(String cardId, String userId, String role) {
        Card card = findOrThrow(cardId);
        requireOwnerOrStaff(card, userId, role);
        ensureNotExpired(card);
        card.setStatus(CardStatus.ACTIVE);
        return CardResponse.from(repository.save(card));
    }

    public List<CardResponse> getAll() {
        return repository.findAll().stream().map(CardResponse::from).toList();
    }

    // ---------- helpers ----------

    private Card findOrThrow(String cardId) {
        return repository.findByCardId(cardId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Card not found"));
    }

    /** A card whose expiry date has passed cannot be blocked/unblocked. */
    private void ensureNotExpired(Card card) {
        if (card.getExpiryDate() != null && card.getExpiryDate().isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.CONFLICT, "Card is expired");
        }
    }

    private void requireOwnerOrStaff(Card card, String userId, String role) {
        boolean owner = card.getUserId().equals(userId);
        boolean staff = role != null && STAFF_ROLES.contains(role.toUpperCase());
        if (!owner && !staff) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This card is not yours");
        }
    }

    private String generateCardNumber() {
        String candidate;
        do {
            // exactly 16 digits = two 8-digit blocks. block1 starts with 4 (Visa-like).
            long block1 = ThreadLocalRandom.current().nextLong(4000_0000L, 5000_0000L);   // 8 digits: 40000000..49999999
            long block2 = ThreadLocalRandom.current().nextLong(1000_0000L, 1_0000_0000L); // 8 digits: 10000000..99999999
            candidate = String.valueOf(block1) + block2;
        } while (repository.existsByCardNumber(candidate));
        return candidate;
    }

    private String generateCvv() {
        return String.format("%03d", ThreadLocalRandom.current().nextInt(0, 1000));
    }
}
