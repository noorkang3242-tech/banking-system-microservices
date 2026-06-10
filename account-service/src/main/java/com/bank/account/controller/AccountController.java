package com.bank.account.controller;

import com.bank.account.dto.AccountResponse;
import com.bank.account.dto.AmountRequest;
import com.bank.account.dto.OpenAccountRequest;
import com.bank.account.exception.ApiException;
import com.bank.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * Behind the gateway, which validates the JWT and forwards:
 *   X-User-Id, X-User-Email, X-User-Role.
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Open accounts, check balance, deposit and withdraw")
public class AccountController {

    private final AccountService accountService;

    private static final Set<String> STAFF_ROLES = Set.of("MANAGER", "ADMIN");

    @PostMapping
    @Operation(summary = "Open a new account for the logged-in user")
    public ResponseEntity<AccountResponse> open(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody OpenAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.openAccount(userId, request));
    }

    @GetMapping("/me")
    @Operation(summary = "List my accounts")
    public ResponseEntity<List<AccountResponse>> myAccounts(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(accountService.getMyAccounts(userId));
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get one account (owner or staff)")
    public ResponseEntity<AccountResponse> getOne(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getOne(accountNumber, userId, role));
    }

    @PostMapping("/{accountNumber}/deposit")
    @Operation(summary = "Deposit money into an account")
    public ResponseEntity<AccountResponse> deposit(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String accountNumber,
            @Valid @RequestBody AmountRequest request) {
        return ResponseEntity.ok(accountService.deposit(accountNumber, request.amount(), userId, role));
    }

    @PostMapping("/{accountNumber}/withdraw")
    @Operation(summary = "Withdraw money from an account")
    public ResponseEntity<AccountResponse> withdraw(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String accountNumber,
            @Valid @RequestBody AmountRequest request) {
        return ResponseEntity.ok(accountService.withdraw(accountNumber, request.amount(), userId, role));
    }

    @GetMapping
    @Operation(summary = "List all accounts (staff only)")
    public ResponseEntity<List<AccountResponse>> getAll(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (role == null || !STAFF_ROLES.contains(role.toUpperCase())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Requires MANAGER or ADMIN role");
        }
        return ResponseEntity.ok(accountService.getAll());
    }
}
