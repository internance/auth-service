package com.internance.auth.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Sign-up payload. {@code role} is intentionally absent: the server always
 * assigns the default role so clients cannot self-escalate privileges.
 */
public record SignUpRequest(
        @NotBlank @Size(min = 4, max = 20) String username, @NotBlank @Size(min = 8, max = 20) String password) {}
