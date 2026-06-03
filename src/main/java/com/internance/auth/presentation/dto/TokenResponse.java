package com.internance.auth.presentation.dto;

import com.internance.auth.application.service.TokenResult;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn) {

    public static TokenResponse from(TokenResult result) {
        return new TokenResponse(result.accessToken(), result.refreshToken(), "Bearer", result.expiresIn());
    }
}
