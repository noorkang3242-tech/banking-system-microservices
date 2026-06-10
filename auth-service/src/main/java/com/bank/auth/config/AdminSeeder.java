package com.bank.auth.config;

import com.bank.auth.entity.AccountStatus;
import com.bank.auth.entity.Role;
import com.bank.auth.entity.UserCredential;
import com.bank.auth.repository.UserCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Seeds a single ADMIN account at startup from ADMIN_EMAIL / ADMIN_PASSWORD.
 * This is the ONLY way staff are created now that /register is locked to CUSTOMER.
 * If the env vars are absent, seeding is skipped — we never create a default
 * admin with a guessable password.
 */
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.seed.email:}")
    private String adminEmail;

    @Value("${admin.seed.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.warn("Admin seeding skipped: ADMIN_EMAIL / ADMIN_PASSWORD not configured.");
            return;
        }
        if (repository.existsByEmail(adminEmail)) {
            log.info("Admin '{}' already exists; not re-seeding.", adminEmail);
            return;
        }
        UserCredential admin = UserCredential.builder()
                .userId(UUID.randomUUID().toString())
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();
        repository.save(admin);
        log.info("Seeded ADMIN account '{}'.", adminEmail);
    }
}
