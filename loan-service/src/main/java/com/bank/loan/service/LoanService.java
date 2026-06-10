package com.bank.loan.service;

import com.bank.loan.client.AccountClient;
import com.bank.loan.client.NotificationClient;
import com.bank.loan.dto.LoanApplicationRequest;
import com.bank.loan.dto.LoanResponse;
import com.bank.loan.entity.Loan;
import com.bank.loan.entity.LoanStatus;
import com.bank.loan.exception.ApiException;
import com.bank.loan.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanService {

    private static final Set<String> STAFF_ROLES = Set.of("MANAGER", "ADMIN");
    private static final BigDecimal DEFAULT_RATE = new BigDecimal("10.00");
    private static final String INTERNAL_ROLE = "MANAGER";

    private final LoanRepository repository;
    private final AccountClient accountClient;
    private final NotificationClient notificationClient;

    /** A customer applies for a loan against one of their own accounts. */
    public LoanResponse apply(String userId, String role, LoanApplicationRequest req) {
        // verify the disbursement account exists and belongs to the applicant
        try {
            accountClient.getAccount(req.accountNumber(), userId, "CUSTOMER");
        } catch (ApiException e) {
            // Only not-found / forbidden means a bad account; surface outages (502/5xx) as-is.
            if (e.getStatus() == HttpStatus.NOT_FOUND || e.getStatus() == HttpStatus.FORBIDDEN) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "accountNumber not found or not yours: " + req.accountNumber());
            }
            throw e;
        }
        BigDecimal rate = req.interestRate() != null ? req.interestRate() : DEFAULT_RATE;
        Loan loan = Loan.builder()
                .loanId(UUID.randomUUID().toString())
                .userId(userId)
                .accountNumber(req.accountNumber())
                .principal(req.principal().setScale(2, RoundingMode.HALF_UP))
                .interestRate(rate)
                .termMonths(req.termMonths())
                .purpose(req.purpose())
                .status(LoanStatus.PENDING)
                .outstanding(BigDecimal.ZERO)
                .build();
        return LoanResponse.from(repository.save(loan));
    }

    /** Staff approves: compute payable, disburse principal to the account, mark ACTIVE. */
    public LoanResponse approve(String loanId) {
        Loan loan = findOrThrow(loanId);
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Only PENDING loans can be approved (is " + loan.getStatus() + ")");
        }
        BigDecimal interest = loan.getPrincipal()
                .multiply(loan.getInterestRate()).divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(loan.getTermMonths())).divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
        BigDecimal totalPayable = loan.getPrincipal().add(interest).setScale(2, RoundingMode.HALF_UP);

        // disburse the principal into the borrower's account (internal credit)
        accountClient.deposit(loan.getAccountNumber(), loan.getPrincipal(), loan.getUserId(), INTERNAL_ROLE);

        loan.setTotalPayable(totalPayable);
        loan.setOutstanding(totalPayable);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setDecisionReason("Approved and disbursed");
        LoanResponse resp = LoanResponse.from(repository.save(loan));
        notificationClient.send(loan.getUserId(), "LOAN", "Loan approved",
                "Your loan of PKR " + loan.getPrincipal() + " was approved and disbursed. Total payable PKR "
                        + loan.getTotalPayable() + ".");
        return resp;
    }

    public LoanResponse reject(String loanId, String reason) {
        Loan loan = findOrThrow(loanId);
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Only PENDING loans can be rejected (is " + loan.getStatus() + ")");
        }
        loan.setStatus(LoanStatus.REJECTED);
        loan.setDecisionReason(reason != null ? reason : "Rejected");
        LoanResponse resp = LoanResponse.from(repository.save(loan));
        notificationClient.send(loan.getUserId(), "LOAN", "Loan rejected",
                "Your loan application was rejected: " + loan.getDecisionReason());
        return resp;
    }

    /** Borrower repays from their account; closes the loan when fully paid. */
    public LoanResponse repay(String loanId, String userId, String role, BigDecimal amount) {
        Loan loan = findOrThrow(loanId);
        if (!loan.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This loan is not yours");
        }
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "Loan is not ACTIVE (is " + loan.getStatus() + ")");
        }
        // never collect more than what's owed
        BigDecimal pay = amount.min(loan.getOutstanding()).setScale(2, RoundingMode.HALF_UP);

        // pull the repayment from the borrower's account (they own it)
        accountClient.withdraw(loan.getAccountNumber(), pay, userId, role != null ? role : "CUSTOMER");

        BigDecimal remaining = loan.getOutstanding().subtract(pay);
        if (remaining.signum() <= 0) {
            loan.setOutstanding(BigDecimal.ZERO);
            loan.setStatus(LoanStatus.CLOSED);
        } else {
            loan.setOutstanding(remaining);
        }
        return LoanResponse.from(repository.save(loan));
    }

    public List<LoanResponse> getMyLoans(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(LoanResponse::from).toList();
    }

    public LoanResponse getOne(String loanId, String userId, String role) {
        Loan loan = findOrThrow(loanId);
        boolean staff = role != null && STAFF_ROLES.contains(role.toUpperCase());
        if (!staff && !loan.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This loan is not yours");
        }
        return LoanResponse.from(loan);
    }

    public List<LoanResponse> getAll() {
        return repository.findAll().stream().map(LoanResponse::from).toList();
    }

    private Loan findOrThrow(String loanId) {
        return repository.findByLoanId(loanId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loan not found"));
    }
}
