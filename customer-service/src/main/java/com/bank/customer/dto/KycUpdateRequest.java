package com.bank.customer.dto;

import com.bank.customer.entity.KycStatus;
import jakarta.validation.constraints.NotNull;

public record KycUpdateRequest(

        @NotNull(message = "kycStatus is required (PENDING, VERIFIED, or REJECTED)")
        KycStatus kycStatus
) {}
