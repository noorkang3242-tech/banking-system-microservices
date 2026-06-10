package com.bank.customer.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * All fields optional — only the non-null ones are applied (partial update).
 */
public record UpdateCustomerRequest(

        @Size(max = 80)
        String firstName,

        @Size(max = 80)
        String lastName,

        @Size(max = 20)
        String phone,

        @Size(max = 255)
        String address,

        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth
) {}
