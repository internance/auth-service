package com.internance.auth.infrastructure.security;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Server-side registry of currently-valid refresh tokens, keyed by token id
 * (jti) in Redis. Making refresh tokens stateful is what lets the service
 * revoke a session on logout and reject a refresh token that has already been
 * rotated.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh_token:";

    private final StringRedisTemplate redisTemplate;

    /** Marks a refresh token (jti) as valid for the given user until {@code ttl} elapses. */
    public void store(String jti, UUID userId, Duration ttl) {
        redisTemplate.opsForValue().set(key(jti), userId.toString(), ttl);
    }

    /**
     * Atomically validates and consumes a refresh token (jti): it is deleted and
     * {@code true} is returned only if it existed for the given user. Backed by a
     * single Redis {@code GETDEL}, so under concurrent refreshes of the same
     * token exactly one caller wins — preventing double-refresh races.
     *
     * @return {@code true} if the token was valid and is now consumed; {@code false}
     *         if it was missing, already consumed, or belonged to another user
     */
    public boolean consume(String jti, UUID userId) {
        String stored = redisTemplate.opsForValue().getAndDelete(key(jti));
        return stored != null && stored.equals(userId.toString());
    }

    /** Revokes a refresh token (idempotent: revoking an unknown jti is a no-op). */
    public void revoke(String jti) {
        redisTemplate.delete(key(jti));
    }

    private String key(String jti) {
        return KEY_PREFIX + jti;
    }
}
