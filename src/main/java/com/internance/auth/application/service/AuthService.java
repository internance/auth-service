package com.internance.auth.application.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.internance.auth.domain.exception.AuthErrorCode;
import com.internance.auth.domain.model.User;
import com.internance.auth.infrastructure.persistence.UserRepository;
import com.internance.auth.infrastructure.security.JwtTokenProvider;
import com.internance.common.exception.BusinessException;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Verifies credentials and issues an access/refresh token pair.
     *
     * @throws BusinessException {@link AuthErrorCode#INVALID_CREDENTIALS} if the
     *         username is unknown or the password does not match. The same error
     *         is used for both to avoid revealing which usernames exist.
     */
    @Transactional(readOnly = true)
    public TokenResult login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPassword()))
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));
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
        User user = userRepository.findById(userId)
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
