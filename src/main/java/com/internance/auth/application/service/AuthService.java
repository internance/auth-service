package com.internance.auth.application.service;

import com.internance.auth.domain.exception.AuthErrorCode;
import com.internance.auth.domain.model.User;
import com.internance.auth.infrastructure.persistence.UserRepository;
import com.internance.auth.infrastructure.security.JwtTokenProvider;
import com.internance.common.exception.BusinessException;
import io.jsonwebtoken.JwtException;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    /**
     * Encoded once at startup so the "user not found" path performs the same
     * bcrypt work as a real password check, closing the user-enumeration timing
     * side channel. Computed via the injected encoder so it always matches the
     * configured algorithm/strength.
     */
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
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
     * Validates a refresh token and issues a new token pair (refresh rotation).
     *
     * @throws BusinessException {@link AuthErrorCode#INVALID_TOKEN} if the token
     *         is invalid/expired or the user no longer exists.
     */
    @Transactional(readOnly = true)
    public TokenResult refresh(String refreshToken) {
        UUID userId;
        try {
            userId = jwtTokenProvider.parseRefreshSubject(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN, e);
        }
        User user = userRepository
                .findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_TOKEN));
        return issueTokens(user);
    }

    private TokenResult issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        return new TokenResult(accessToken, refreshToken, jwtTokenProvider.getAccessTokenValiditySeconds());
    }
}
