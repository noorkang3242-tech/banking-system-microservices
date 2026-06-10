package com.bank.customer.dto;

import com.bank.customer.entity.Customer;
import com.bank.customer.entity.KycStatus;

import java.time.Instant;
import java.time.LocalDate;

public record CustomerResponse(
        String userId,
        String email,
        String firstName,
        String lastName,
        String phone,
        String address,
        LocalDate dateOfBirth,
        KycStatus kycStatus,
        Instant createdAt,
        Instant updatedAt
) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(
                c.getUserId(),
                c.getEmail(),
                c.getFirstName(),
                c.getLastName(),
                c.getPhone(),
                c.getAddress(),
                c.getDateOfBirth(),
                c.getKycStatus(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
