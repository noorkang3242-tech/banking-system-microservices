package com.bank.auth.dto;

public record AuthResponse(
        String userId,
        String email,
        String role,
        String accessToken,
        String tokenType
) {}
