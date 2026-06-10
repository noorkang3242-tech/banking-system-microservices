package com.bank.customer.repository;

import com.bank.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByUserId(String userId);

    boolean existsByUserId(String userId);
}
