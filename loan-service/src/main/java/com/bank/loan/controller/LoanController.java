package com.bank.loan.controller;

import com.bank.loan.dto.LoanApplicationRequest;
import com.bank.loan.dto.LoanResponse;
import com.bank.loan.dto.RejectRequest;
import com.bank.loan.dto.RepayRequest;
import com.bank.loan.exception.ApiException;
import com.bank.loan.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Apply, approve/reject, disburse and repay loans")
public class LoanController {

    private final LoanService loanService;

    private static final Set<String> STAFF_ROLES = Set.of("MANAGER", "ADMIN");

    // ---------- Customer ----------

    @PostMapping
    @Operation(summary = "Apply for a loan")
    public ResponseEntity<LoanResponse> apply(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody LoanApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.apply(userId, role, request));
    }

    @GetMapping("/me")
    @Operation(summary = "My loans (newest first)")
    public ResponseEntity<List<LoanResponse>> myLoans(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(loanService.getMyLoans(userId));
    }

    @GetMapping("/{loanId}")
    @Operation(summary = "Get one loan (owner or staff)")
    public ResponseEntity<LoanResponse> getOne(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String loanId) {
        return ResponseEntity.ok(loanService.getOne(loanId, userId, role));
    }

    @PostMapping("/{loanId}/repay")
    @Operation(summary = "Repay an active loan from your account")
    public ResponseEntity<LoanResponse> repay(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String loanId,
            @Valid @RequestBody RepayRequest request) {
        return ResponseEntity.ok(loanService.repay(loanId, userId, role, request.amount()));
    }

    // ---------- Staff (MANAGER / ADMIN) ----------

    @PatchMapping("/{loanId}/approve")
    @Operation(summary = "Approve + disburse a loan (staff only)")
    public ResponseEntity<LoanResponse> approve(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String loanId) {
        requireStaff(role);
        return ResponseEntity.ok(loanService.approve(loanId));
    }

    @PatchMapping("/{loanId}/reject")
    @Operation(summary = "Reject a loan (staff only)")
    public ResponseEntity<LoanResponse> reject(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String loanId,
            @RequestBody(required = false) RejectRequest request) {
        requireStaff(role);
        String reason = request != null ? request.reason() : null;
        return ResponseEntity.ok(loanService.reject(loanId, reason));
    }

    @GetMapping
    @Operation(summary = "List all loans (staff only)")
    public ResponseEntity<List<LoanResponse>> getAll(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireStaff(role);
        return ResponseEntity.ok(loanService.getAll());
    }

    private void requireStaff(String role) {
        if (role == null || !STAFF_ROLES.contains(role.toUpperCase())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Requires MANAGER or ADMIN role");
        }
    }
}
