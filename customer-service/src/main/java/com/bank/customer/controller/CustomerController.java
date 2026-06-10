package com.bank.customer.controller;

import com.bank.customer.dto.CreateCustomerRequest;
import com.bank.customer.dto.CustomerResponse;
import com.bank.customer.dto.KycUpdateRequest;
import com.bank.customer.dto.UpdateCustomerRequest;
import com.bank.customer.exception.ApiException;
import com.bank.customer.service.CustomerService;
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
 * The gateway validates the JWT and forwards the caller's identity as headers:
 *   X-User-Id, X-User-Email, X-User-Role.
 * This service trusts those headers (it sits behind the gateway).
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer profiles and KYC")
public class CustomerController {

    private final CustomerService customerService;

    private static final Set<String> STAFF_ROLES = Set.of("MANAGER", "ADMIN");

    // ---------- Self-service (any logged-in user) ----------

    @PostMapping
    @Operation(summary = "Create my customer profile")
    public ResponseEntity<CustomerResponse> create(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @Valid @RequestBody CreateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerService.createProfile(userId, email, request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my customer profile")
    public ResponseEntity<CustomerResponse> getMe(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(customerService.getByUserId(userId));
    }

    @PutMapping("/me")
    @Operation(summary = "Update my customer profile")
    public ResponseEntity<CustomerResponse> updateMe(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(customerService.updateProfile(userId, request));
    }

    // ---------- Staff-only (MANAGER / ADMIN) ----------

    @GetMapping
    @Operation(summary = "List all customers (staff only)")
    public ResponseEntity<List<CustomerResponse>> getAll(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireStaff(role);
        return ResponseEntity.ok(customerService.getAll());
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get a customer by userId (staff only)")
    public ResponseEntity<CustomerResponse> getByUserId(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String userId) {
        requireStaff(role);
        return ResponseEntity.ok(customerService.getByUserId(userId));
    }

    @PatchMapping("/{userId}/kyc")
    @Operation(summary = "Update a customer's KYC status (staff only)")
    public ResponseEntity<CustomerResponse> updateKyc(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String userId,
            @Valid @RequestBody KycUpdateRequest request) {
        requireStaff(role);
        return ResponseEntity.ok(customerService.updateKyc(userId, request.kycStatus()));
    }

    private void requireStaff(String role) {
        if (role == null || !STAFF_ROLES.contains(role.toUpperCase())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Requires MANAGER or ADMIN role");
        }
    }
}
