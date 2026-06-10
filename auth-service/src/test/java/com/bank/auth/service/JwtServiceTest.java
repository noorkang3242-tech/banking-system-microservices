package com.bank.auth.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService("ChangeThisSecretToA64CharacterLongRandomStringForProductionUse12345", 86400000L);

    @Test
    void generatesAndParsesToken() {
        String token = jwtService.generateToken("user-123", "noor@example.com", "CUSTOMER");

        assertNotNull(token);
        assertTrue(jwtService.isValid(token));

        Claims claims = jwtService.parse(token);
        assertEquals("user-123", claims.get("userId", String.class));
        assertEquals("noor@example.com", claims.get("email", String.class));
        assertEquals("CUSTOMER", claims.get("role", String.class));
    }

    @Test
    void rejectsGarbageToken() {
        assertFalse(jwtService.isValid("not-a-real-token"));
    }
}
