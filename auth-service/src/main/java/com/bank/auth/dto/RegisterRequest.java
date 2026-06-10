package com.bank.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Public registration input. There is deliberately NO role field: self-registration
 * always yields a CUSTOMER. Staff accounts are seeded out-of-band (AdminSeeder).
 */
public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password
) {}
