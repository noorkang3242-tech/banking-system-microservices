package com.bank.account.service;

import com.bank.account.client.TransactionClient;
import com.bank.account.dto.AccountResponse;
import com.bank.account.dto.OpenAccountRequest;
import com.bank.account.entity.Account;
import com.bank.account.entity.AccountStatus;
import com.bank.account.exception.ApiException;
import com.bank.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository repository;
    private final TransactionClient transactionClient;

    private static final Set<String> STAFF_ROLES = Set.of("MANAGER", "ADMIN");

    public AccountResponse openAccount(String userId, OpenAccountRequest request) {
        BigDecimal opening = request.initialDeposit() != null ? request.initialDeposit() : BigDecimal.ZERO;
        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .userId(userId)
                .accountType(request.accountType())
                .balance(opening)
                .currency("PKR")
                .status(AccountStatus.ACTIVE)
                .build();
        Account saved = repository.save(account);
        if (opening.signum() > 0) {
            transactionClient.record(userId, saved.getAccountNumber(), "DEPOSIT", opening, saved.getBalance(), "Opening deposit");
        }
        return AccountResponse.from(saved);
    }

    public List<AccountResponse> getMyAccounts(String userId) {
        return repository.findByUserId(userId).stream().map(AccountResponse::from).toList();
    }

    public AccountResponse getOne(String accountNumber, String userId, String role) {
        Account account = findOrThrow(accountNumber);
        requireOwnerOrStaff(account, userId, role);
        return AccountResponse.from(account);
    }

    public List<AccountResponse> getAll() {
        return repository.findAll().stream().map(AccountResponse::from).toList();
    }

    @Transactional
    public AccountResponse deposit(String accountNumber, BigDecimal amount, String userId, String role) {
        Account account = findOrThrow(accountNumber);
        requireOwnerOrStaff(account, userId, role);
        requireActive(account);
        account.setBalance(account.getBalance().add(amount));
        Account saved = repository.save(account);
        transactionClient.record(account.getUserId(), saved.getAccountNumber(), "DEPOSIT", amount, saved.getBalance(), "Deposit");
        return AccountResponse.from(saved);
    }

    @Transactional
    public AccountResponse withdraw(String accountNumber, BigDecimal amount, String userId, String role) {
        Account account = findOrThrow(accountNumber);
        requireOwnerOrStaff(account, userId, role);
        requireActive(account);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient balance");
        }
        account.setBalance(account.getBalance().subtract(amount));
        Account saved = repository.save(account);
        transactionClient.record(account.getUserId(), saved.getAccountNumber(), "WITHDRAWAL", amount, saved.getBalance(), "Withdrawal");
        return AccountResponse.from(saved);
    }

    // ---------- helpers ----------

    private Account findOrThrow(String accountNumber) {
        return repository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private void requireActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "Account is not ACTIVE (status: " + account.getStatus() + ")");
        }
    }

    private void requireOwnerOrStaff(Account account, String userId, String role) {
        boolean isOwner = account.getUserId().equals(userId);
        boolean isStaff = role != null && STAFF_ROLES.contains(role.toUpperCase());
        if (!isOwner && !isStaff) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not own this account");
        }
    }

    /** 16-digit numeric account number, retried until unique. */
    private String generateAccountNumber() {
        String candidate;
        do {
            long block1 = ThreadLocalRandom.current().nextLong(1000_0000L, 9999_9999L);
            long block2 = ThreadLocalRandom.current().nextLong(1000_0000L, 9999_9999L);
            candidate = String.valueOf(block1) + block2;
        } while (repository.existsByAccountNumber(candidate));
        return candidate;
    }
}
