package com.bank.transaction.controller;

import com.bank.transaction.dto.RecordTransactionRequest;
import com.bank.transaction.dto.TransactionResponse;
import com.bank.transaction.exception.ApiException;
import com.bank.transaction.service.TransactionService;
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
 * Behind the gateway. The caller's identity arrives as headers injected by the gateway
 * (X-User-Id, X-User-Role). account-service / transfer-service call POST /record
 * service-to-service, passing X-User-Id of the account owner themselves.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction history / ledger")
public class TransactionController {

    private final TransactionService transactionService;

    private static final Set<String> STAFF_ROLES = Set.of("MANAGER", "ADMIN");

    @PostMapping("/record")
    @Operation(summary = "Record a transaction (called by account/transfer services)")
    public ResponseEntity<TransactionResponse> record(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody RecordTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.record(userId, request));
    }

    @GetMapping("/me")
    @Operation(summary = "My transaction history (newest first)")
    public ResponseEntity<List<TransactionResponse>> myTransactions(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(transactionService.getMyTransactions(userId));
    }

    @GetMapping("/account/{accountNumber}")
    @Operation(summary = "Transactions for an account (owner sees own; staff sees all)")
    public ResponseEntity<List<TransactionResponse>> byAccount(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(transactionService.getByAccount(accountNumber, userId, role));
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get one transaction (owner or staff)")
    public ResponseEntity<TransactionResponse> getOne(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String transactionId) {
        return ResponseEntity.ok(transactionService.getOne(transactionId, userId, role));
    }

    @GetMapping
    @Operation(summary = "List all transactions (staff only)")
    public ResponseEntity<List<TransactionResponse>> getAll(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (role == null || !STAFF_ROLES.contains(role.toUpperCase())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Requires MANAGER or ADMIN role");
        }
        return ResponseEntity.ok(transactionService.getAll());
    }
}
