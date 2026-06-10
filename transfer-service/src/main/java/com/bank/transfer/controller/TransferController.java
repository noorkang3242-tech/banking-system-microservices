package com.bank.transfer.controller;

import com.bank.transfer.dto.TransferRequest;
import com.bank.transfer.dto.TransferResponse;
import com.bank.transfer.exception.ApiException;
import com.bank.transfer.service.TransferService;
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
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "Move money between accounts (saga)")
public class TransferController {

    private final TransferService transferService;

    private static final Set<String> STAFF_ROLES = Set.of("MANAGER", "ADMIN");

    @PostMapping
    @Operation(summary = "Transfer money from one account to another")
    public ResponseEntity<TransferResponse> transfer(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transferService.transfer(userId, role, request));
    }

    @GetMapping("/me")
    @Operation(summary = "My transfers (newest first)")
    public ResponseEntity<List<TransferResponse>> myTransfers(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(transferService.getMyTransfers(userId));
    }

    @GetMapping("/{transferId}")
    @Operation(summary = "Get one transfer (owner or staff)")
    public ResponseEntity<TransferResponse> getOne(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String transferId) {
        return ResponseEntity.ok(transferService.getOne(transferId, userId, role));
    }

    @GetMapping
    @Operation(summary = "List all transfers (staff only)")
    public ResponseEntity<List<TransferResponse>> getAll(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (role == null || !STAFF_ROLES.contains(role.toUpperCase())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Requires MANAGER or ADMIN role");
        }
        return ResponseEntity.ok(transferService.getAll());
    }
}
