package com.bank.auth.service;

import com.bank.auth.dto.AuthResponse;
import com.bank.auth.dto.LoginRequest;
import com.bank.auth.dto.RegisterRequest;
import com.bank.auth.dto.ValidateResponse;
import com.bank.auth.entity.AccountStatus;
import com.bank.auth.entity.Role;
import com.bank.auth.entity.UserCredential;
import com.bank.auth.exception.ApiException;
import com.bank.auth.repository.UserCredentialRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }

        // Public self-registration ALWAYS creates a CUSTOMER. Staff accounts
        // (MANAGER/ADMIN) are provisioned out-of-band (see AdminSeeder), never
        // through this open endpoint — otherwise anyone could grant themselves
        // ADMIN and bypass every staff-only gate.
        UserCredential user = UserCredential.builder()
                .userId(UUID.randomUUID().toString())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .build();

        repository.save(user);

        String token = jwtService.generateToken(user.getUserId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(user.getUserId(), user.getEmail(), user.getRole().name(), token, "Bearer");
    }

    public AuthResponse login(LoginRequest request) {
        UserCredential user = repository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is not active");
        }

        String token = jwtService.generateToken(user.getUserId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(user.getUserId(), user.getEmail(), user.getRole().name(), token, "Bearer");
    }

    public ValidateResponse validate(String token) {
        if (!jwtService.isValid(token)) {
            return new ValidateResponse(false, null, null, null);
        }
        Claims claims = jwtService.parse(token);
        return new ValidateResponse(
                true,
                claims.get("userId", String.class),
                claims.get("email", String.class),
                claims.get("role", String.class)
        );
    }
}
