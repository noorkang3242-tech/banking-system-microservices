package com.bank.transfer.repository;

import com.bank.transfer.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    List<Transfer> findByInitiatedByOrderByCreatedAtDesc(String initiatedBy);

    Optional<Transfer> findByTransferId(String transferId);
}
