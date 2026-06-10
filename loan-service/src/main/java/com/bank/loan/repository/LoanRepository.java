package com.bank.loan.repository;

import com.bank.loan.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Loan> findByLoanId(String loanId);
}
