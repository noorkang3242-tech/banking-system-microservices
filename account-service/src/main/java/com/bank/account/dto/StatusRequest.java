package com.bank.account.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for an admin/manager changing an account's status (ACTIVE / FROZEN / CLOSED). */
public record StatusRequest(@NotBlank String status) {}
