package com.bank.transaction.service;

import com.bank.transaction.dto.RecordTransactionRequest;
import com.bank.transaction.dto.TransactionResponse;
import com.bank.transaction.entity.Transaction;
import com.bank.transaction.entity.TransactionStatus;
import com.bank.transaction.exception.ApiException;
import com.bank.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository repository;

    private static final Set<String> STAFF_ROLES = Set.of("MANAGER", "ADMIN");

    public TransactionResponse record(String userId, RecordTransactionRequest request) {
        Transaction tx = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .accountNumber(request.accountNumber())
                .userId(userId)
                .type(request.type())
                .amount(request.amount())
                .currency("PKR")
                .balanceAfter(request.balanceAfter())
                .counterpartyAccount(request.counterpartyAccount())
                .description(request.description())
                .status(TransactionStatus.SUCCESS)
                .build();
        return TransactionResponse.from(repository.save(tx));
    }

    public List<TransactionResponse> getMyTransactions(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(TransactionResponse::from).toList();
    }

    /** Staff see every transaction for the account; a customer sees only their own. */
    public List<TransactionResponse> getByAccount(String accountNumber, String userId, String role) {
        boolean staff = isStaff(role);
        return repository.findByAccountNumberOrderByCreatedAtDesc(accountNumber).stream()
                .filter(t -> staff || t.getUserId().equals(userId))
                .map(TransactionResponse::from).toList();
    }

    public TransactionResponse getOne(String transactionId, String userId, String role) {
        Transaction tx = repository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));
        if (!isStaff(role) && !tx.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This transaction does not belong to you");
        }
        return TransactionResponse.from(tx);
    }

    public List<TransactionResponse> getAll() {
        return repository.findAll().stream().map(TransactionResponse::from).toList();
    }

    private boolean isStaff(String role) {
        return role != null && STAFF_ROLES.contains(role.toUpperCase());
    }
}
