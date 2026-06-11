package com.bank.account.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Minimal view of a customer profile, used only to show a beneficiary's name. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerInfo(String firstName, String lastName, String email) {}
