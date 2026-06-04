package com.internance.auth.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

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
        return build(userId, null, accessTokenValidity, Map.of(TYPE_CLAIM, ACCESS, ROLE_CLAIM, role));
    }

    /**
     * Creates a refresh token carrying {@code jti} as its id, so the server-side
     * store can track and revoke it.
     */
    public String createRefreshToken(UUID userId, String jti) {
        return build(userId, jti, refreshTokenValidity, Map.of(TYPE_CLAIM, REFRESH));
    }

    public long getAccessTokenValiditySeconds() {
        return accessTokenValidity.toSeconds();
    }

    public Duration getRefreshTokenValidity() {
        return refreshTokenValidity;
    }

    /**
     * Verifies a refresh token and returns its subject (user id) and id (jti).
     *
     * @throws JwtException if the token is malformed, expired, tampered with, or
     *                      is not a refresh token
     */
    public RefreshToken parseRefresh(String token) {
        Claims claims =
                Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (!REFRESH.equals(claims.get(TYPE_CLAIM, String.class))) {
            throw new JwtException("Not a refresh token");
        }
        return new RefreshToken(UUID.fromString(claims.getSubject()), claims.getId());
    }

    private String build(UUID userId, String jti, Duration ttl, Map<String, ?> claims) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(userId.toString())
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key);
        if (jti != null) {
            builder.id(jti);
        }
        return builder.compact();
    }

    /** Verified refresh-token identity: the subject (user id) and id (jti). */
    public record RefreshToken(UUID userId, String jti) {}
}
