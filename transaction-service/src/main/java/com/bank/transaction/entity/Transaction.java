package com.bank.transaction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transaction", indexes = {
        @Index(name = "idx_tx_user", columnList = "user_id"),
        @Index(name = "idx_tx_account", columnList = "account_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public identifier for this transaction. */
    @Column(name = "transaction_id", unique = true, nullable = false, length = 36)
    private String transactionId;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    /** The auth-service user_id that owns the account. */
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    /** Account balance right after this transaction (optional). */
    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    /** The other account in a transfer (optional). */
    @Column(name = "counterparty_account", length = 20)
    private String counterpartyAccount;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        if (this.currency == null) this.currency = "PKR";
        if (this.status == null)   this.status = TransactionStatus.SUCCESS;
    }
}
