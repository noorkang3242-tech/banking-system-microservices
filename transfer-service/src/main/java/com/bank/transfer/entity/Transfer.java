package com.bank.transfer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transfer", indexes = {
        @Index(name = "idx_transfer_user", columnList = "initiated_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_id", unique = true, nullable = false, length = 36)
    private String transferId;

    @Column(name = "from_account", nullable = false, length = 20)
    private String fromAccount;

    @Column(name = "to_account", nullable = false, length = 20)
    private String toAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status;

    /** The auth-service user_id that initiated the transfer. */
    @Column(name = "initiated_by", nullable = false, length = 36)
    private String initiatedBy;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        if (this.currency == null) this.currency = "PKR";
    }
}
