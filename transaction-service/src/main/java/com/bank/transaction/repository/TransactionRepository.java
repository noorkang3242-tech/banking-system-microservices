package com.bank.transaction.repository;

import com.bank.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Transaction> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);

    Optional<Transaction> findByTransactionId(String transactionId);
}
