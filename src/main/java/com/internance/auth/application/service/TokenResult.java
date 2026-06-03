package com.internance.auth.application.service;

/** Issued token pair plus the access token lifetime (seconds). */
public record TokenResult(String accessToken, String refreshToken, long expiresIn) {
}
