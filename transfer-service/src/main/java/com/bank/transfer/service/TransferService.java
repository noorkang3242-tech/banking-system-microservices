package com.bank.transfer.service;

import com.bank.transfer.client.AccountClient;
import com.bank.transfer.client.AccountInfo;
import com.bank.transfer.client.NotificationClient;
import com.bank.transfer.dto.TransferRequest;
import com.bank.transfer.dto.TransferResponse;
import com.bank.transfer.entity.Transfer;
import com.bank.transfer.entity.TransferStatus;
import com.bank.transfer.exception.ApiException;
import com.bank.transfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private static final Set<String> STAFF_ROLES = Set.of("MANAGER", "ADMIN");
    /** Role used for the internal credit to the destination (we don't own that account). */
    private static final String INTERNAL_ROLE = "MANAGER";

    private final AccountClient accountClient;
    private final TransferRepository repository;
    private final NotificationClient notificationClient;

    /**
     * Saga:
     *   1. validate (different accounts, destination exists)
     *   2. DEBIT source  (as the initiator — they must own it)
     *   3. CREDIT dest   (internal). If this fails -> COMPENSATE: refund the source.
     * NOTE: not a single DB transaction — money lives in account-service, so we use
     * a compensating action instead of a rollback.
     */
    public TransferResponse transfer(String initiatorUserId, String initiatorRole, TransferRequest req) {
        String role = initiatorRole != null ? initiatorRole : "CUSTOMER";

        if (req.fromAccount().equals(req.toAccount())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot transfer to the same account");
        }

        // 1. destination must exist (internal read) — fail before any money moves
        AccountInfo dest;
        try {
            dest = accountClient.getAccount(req.toAccount(), initiatorUserId, INTERNAL_ROLE);
        } catch (ApiException e) {
            // Only a genuine not-found means a bad destination; surface outages (502/5xx) as-is
            // so a transfer fails clearly when account-service is unavailable.
            if (e.getStatus() == HttpStatus.NOT_FOUND) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Destination account not found: " + req.toAccount());
            }
            throw e;
        }
        if (dest != null && dest.status() != null && !"ACTIVE".equals(dest.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "Destination account is not ACTIVE");
        }

        // 2. DEBIT source as the initiator (ownership / balance enforced by account-service)
        accountClient.withdraw(req.fromAccount(), req.amount(), initiatorUserId, role);

        // 3. CREDIT destination (internal). On failure, refund the source.
        try {
            accountClient.deposit(req.toAccount(), req.amount(), initiatorUserId, INTERNAL_ROLE);
        } catch (Exception creditError) {
            log.error("Credit to {} failed, compensating (refund to {}): {}",
                    req.toAccount(), req.fromAccount(), creditError.getMessage());
            try {
                accountClient.deposit(req.fromAccount(), req.amount(), initiatorUserId, INTERNAL_ROLE);
            } catch (Exception refundError) {
                log.error("CRITICAL: refund to {} failed: {}", req.fromAccount(), refundError.getMessage());
            }
            Transfer failed = save(req, initiatorUserId, TransferStatus.FAILED,
                    "Credit failed and amount was refunded to source");
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Transfer failed (credit step); amount refunded. transferId=" + failed.getTransferId());
        }

        // 4. success — record it, then notify both parties (best-effort)
        Transfer done = save(req, initiatorUserId, TransferStatus.COMPLETED, null);
        notificationClient.send(initiatorUserId, "TRANSFER", "Transfer sent",
                "You sent PKR " + req.amount() + " to account " + req.toAccount() + ".");
        // Skip the "received" note on a self-transfer between the user's own accounts.
        if (dest != null && dest.userId() != null && !dest.userId().equals(initiatorUserId)) {
            notificationClient.send(dest.userId(), "TRANSFER", "Transfer received",
                    "You received PKR " + req.amount() + " into account " + req.toAccount() + ".");
        }
        return TransferResponse.from(done);
    }

    public List<TransferResponse> getMyTransfers(String userId) {
        return repository.findByInitiatedByOrderByCreatedAtDesc(userId).stream()
                .map(TransferResponse::from).toList();
    }

    public TransferResponse getOne(String transferId, String userId, String role) {
        Transfer t = repository.findByTransferId(transferId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transfer not found"));
        boolean staff = role != null && STAFF_ROLES.contains(role.toUpperCase());
        if (!staff && !t.getInitiatedBy().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This transfer does not belong to you");
        }
        return TransferResponse.from(t);
    }

    public List<TransferResponse> getAll() {
        return repository.findAll().stream().map(TransferResponse::from).toList();
    }

    private Transfer save(TransferRequest req, String userId, TransferStatus status, String reason) {
        Transfer t = Transfer.builder()
                .transferId(UUID.randomUUID().toString())
                .fromAccount(req.fromAccount())
                .toAccount(req.toAccount())
                .amount(req.amount())
                .currency("PKR")
                .status(status)
                .initiatedBy(userId)
                .failureReason(reason)
                .build();
        return repository.save(t);
    }
}
