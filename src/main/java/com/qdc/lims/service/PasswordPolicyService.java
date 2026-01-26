package com.qdc.lims.service;

import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Centralized password policy for the desktop application.
 */
@Service
public class PasswordPolicyService {

    // At least 8 chars, with upper, lower, digit, and special character.
    private static final String STRONG_PASSWORD_REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$";

    public Optional<String> validate(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return Optional.of("Password is required.");
        }
        if (!rawPassword.matches(STRONG_PASSWORD_REGEX)) {
            return Optional.of(
                    "Password must be at least 8 characters and include upper, lower, number, and symbol.");
        }
        return Optional.empty();
    }

    public String getPolicyHint() {
        return "Password must be at least 8 characters and include upper, lower, number, and symbol.";
    }
}
