package com.bank.loan.dto;

import jakarta.validation.constraints.Size;

/** Optional reason when a staff member rejects a loan. */
public record RejectRequest(
        @Size(max = 255, message = "reason cannot exceed 255 characters")
        String reason
) {}
