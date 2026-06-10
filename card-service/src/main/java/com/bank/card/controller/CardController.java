package com.bank.card.controller;

import com.bank.card.dto.CardResponse;
import com.bank.card.dto.IssueCardRequest;
import com.bank.card.dto.IssuedCardResponse;
import com.bank.card.exception.ApiException;
import com.bank.card.service.CardService;
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
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Issue and manage debit/credit cards")
public class CardController {

    private final CardService cardService;

    private static final Set<String> STAFF_ROLES = Set.of("MANAGER", "ADMIN");

    @PostMapping
    @Operation(summary = "Issue a new card (full number + CVV returned once)")
    public ResponseEntity<IssuedCardResponse> issue(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody IssueCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.issue(userId, email, role, request));
    }

    @GetMapping("/me")
    @Operation(summary = "My cards (masked)")
    public ResponseEntity<List<CardResponse>> myCards(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(cardService.getMyCards(userId));
    }

    @GetMapping("/{cardId}")
    @Operation(summary = "Get one card (owner or staff, masked)")
    public ResponseEntity<CardResponse> getOne(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String cardId) {
        return ResponseEntity.ok(cardService.getOne(cardId, userId, role));
    }

    @PostMapping("/{cardId}/block")
    @Operation(summary = "Block a card (owner or staff)")
    public ResponseEntity<CardResponse> block(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String cardId) {
        return ResponseEntity.ok(cardService.block(cardId, userId, role));
    }

    @PostMapping("/{cardId}/unblock")
    @Operation(summary = "Unblock a card (owner or staff)")
    public ResponseEntity<CardResponse> unblock(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String cardId) {
        return ResponseEntity.ok(cardService.unblock(cardId, userId, role));
    }

    @GetMapping
    @Operation(summary = "List all cards (staff only)")
    public ResponseEntity<List<CardResponse>> getAll(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (role == null || !STAFF_ROLES.contains(role.toUpperCase())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Requires MANAGER or ADMIN role");
        }
        return ResponseEntity.ok(cardService.getAll());
    }
}
