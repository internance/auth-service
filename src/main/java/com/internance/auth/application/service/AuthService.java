package com.internance.auth.application.service;

import com.internance.auth.domain.exception.AuthErrorCode;
import com.internance.auth.domain.model.User;
import com.internance.auth.infrastructure.persistence.UserRepository;
import com.internance.auth.infrastructure.security.JwtTokenProvider;
import com.internance.auth.infrastructure.security.RefreshTokenStore;
import com.internance.common.exception.BusinessException;
import com.internance.common.utils.IdGenerator;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    /**
     * Encoded once at startup so the "user not found" path performs the same
     * bcrypt work as a real password check, closing the user-enumeration timing
     * side channel. Computed via the injected encoder so it always matches the
     * configured algorithm/strength.
     */
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenStore refreshTokenStore) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenStore = refreshTokenStore;
        this.dummyPasswordHash = passwordEncoder.encode("invalid-password-placeholder");
    }

    /**
     * Verifies credentials and issues an access/refresh token pair.
     *
     * @throws BusinessException {@link AuthErrorCode#INVALID_CREDENTIALS} if the
     *         username is unknown or the password does not match. The same error
     *         and the same amount of work are used for both, so neither the
     *         response nor its timing reveals which usernames exist.
     */
    @Transactional(readOnly = true)
    public TokenResult login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username).orElse(null);
        String encodedPassword = (user != null) ? user.getPassword() : dummyPasswordHash;
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
        if (user == null || !matches) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        return issueTokens(user);
    }

    /**
     * Validates a refresh token and issues a new token pair, rotating the
     * refresh token: the presented one is revoked and a fresh one is stored.
     *
     * @throws BusinessException {@link AuthErrorCode#INVALID_TOKEN} if the token
     *         is invalid/expired, has already been rotated or revoked (not in the
     *         store), or the user no longer exists.
     */
    @Transactional(readOnly = true)
    public TokenResult refresh(String refreshToken) {
        JwtTokenProvider.RefreshToken parsed = parse(refreshToken);
        if (!refreshTokenStore.isValid(parsed.jti(), parsed.userId())) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
        User user = userRepository
                .findById(parsed.userId())
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_TOKEN));
        refreshTokenStore.revoke(parsed.jti());
        return issueTokens(user);
    }

    /**
     * Revokes the session behind the given refresh token. Idempotent for a valid
     * token whose session is already gone.
     *
     * @throws BusinessException {@link AuthErrorCode#INVALID_TOKEN} if the token
     *         cannot be parsed as a refresh token.
     */
    public void logout(String refreshToken) {
        JwtTokenProvider.RefreshToken parsed = parse(refreshToken);
        refreshTokenStore.revoke(parsed.jti());
    }

    private JwtTokenProvider.RefreshToken parse(String refreshToken) {
        try {
            return jwtTokenProvider.parseRefresh(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN, e);
        }
    }

    private TokenResult issueTokens(User user) {
        String jti = IdGenerator.generateString();
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), jti);
        refreshTokenStore.store(jti, user.getId(), jwtTokenProvider.getRefreshTokenValidity());
        return new TokenResult(accessToken, refreshToken, jwtTokenProvider.getAccessTokenValiditySeconds());
    }
}
