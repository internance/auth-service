package com.internance.auth.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * JWT signing configuration. {@code secret} must be supplied (env or config
 * server) and be at least 32 bytes for HS256.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("30m") Duration accessTokenValidity,
        @DefaultValue("14d") Duration refreshTokenValidity) {}
