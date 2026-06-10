package com.bank.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * The caller's identity (userId, email) is NOT taken from this body — it comes
 * from the gateway-injected X-User-Id / X-User-Email headers. This keeps a user
 * from creating a profile for someone else.
 */
public record CreateCustomerRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 80)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 80)
        String lastName,

        @Size(max = 20)
        String phone,

        @Size(max = 255)
        String address,

        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth
) {}
