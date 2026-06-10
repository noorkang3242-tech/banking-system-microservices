package com.bank.card.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * NOTE: the CVV is intentionally NOT stored (best practice — like a real card,
 * it's shown once at issue time and never persisted). The full PAN is stored but
 * always masked in API responses.
 */
@Entity
@Table(name = "card", indexes = {
        @Index(name = "idx_card_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", unique = true, nullable = false, length = 36)
    private String cardId;

    @Column(name = "card_number", unique = true, nullable = false, length = 16)
    private String cardNumber;

    /** Owner's auth-service user_id. */
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    /** Linked account for DEBIT cards; null for CREDIT. */
    @Column(name = "account_number", length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 20)
    private CardType cardType;

    @Column(name = "cardholder_name", nullable = false, length = 100)
    private String cardholderName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardStatus status;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    /** Credit limit for CREDIT cards; null for DEBIT. */
    @Column(name = "credit_limit", precision = 19, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) this.status = CardStatus.ACTIVE;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
