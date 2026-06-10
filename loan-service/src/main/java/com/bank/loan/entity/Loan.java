package com.bank.loan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "loan", indexes = {
        @Index(name = "idx_loan_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", unique = true, nullable = false, length = 36)
    private String loanId;

    /** Borrower's auth-service user_id. */
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    /** Account the loan is disbursed into and repaid from. */
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principal;

    /** Annual interest rate in percent, e.g. 10.00 */
    @Column(name = "interest_rate", nullable = false, precision = 6, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    /** principal + simple interest; computed at approval. */
    @Column(name = "total_payable", precision = 19, scale = 2)
    private BigDecimal totalPayable;

    /** Remaining amount to repay. */
    @Column(precision = 19, scale = 2)
    private BigDecimal outstanding;

    @Column(length = 255)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status;

    @Column(name = "decision_reason", length = 255)
    private String decisionReason;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        if (this.status == null)      this.status = LoanStatus.PENDING;
        if (this.outstanding == null) this.outstanding = BigDecimal.ZERO;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
