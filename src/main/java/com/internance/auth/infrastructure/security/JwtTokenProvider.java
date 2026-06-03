package com.internance.auth.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Issues and verifies HS256 JWTs. Access tokens carry the user's role for
 * downstream authorization; both token types use the user id (UUID) as subject.
 */
@Component
public class JwtTokenProvider {

    private static final String TYPE_CLAIM = "type";
    private static final String ROLE_CLAIM = "role";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

    private final SecretKey key;
    private final Duration accessTokenValidity;
    private final Duration refreshTokenValidity;

    public JwtTokenProvider(JwtProperties properties) {
        if (properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException("jwt.secret must be configured");
        }
        // Throws if the secret is shorter than 256 bits, failing fast at startup.
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = properties.accessTokenValidity();
        this.refreshTokenValidity = properties.refreshTokenValidity();
    }

    public String createAccessToken(UUID userId, String role) {
        return build(userId, accessTokenValidity, Map.of(TYPE_CLAIM, ACCESS, ROLE_CLAIM, role));
    }

    public String createRefreshToken(UUID userId) {
        return build(userId, refreshTokenValidity, Map.of(TYPE_CLAIM, REFRESH));
    }

    public long getAccessTokenValiditySeconds() {
        return accessTokenValidity.toSeconds();
    }

    /**
     * Verifies a refresh token and returns its subject (user id).
     *
     * @throws JwtException if the token is malformed, expired, tampered with, or
     *                      is not a refresh token
     */
    public UUID parseRefreshSubject(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        if (!REFRESH.equals(claims.get(TYPE_CLAIM, String.class))) {
            throw new JwtException("Not a refresh token");
        }
        return UUID.fromString(claims.getSubject());
    }

    private String build(UUID userId, Duration ttl, Map<String, ?> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }
}
