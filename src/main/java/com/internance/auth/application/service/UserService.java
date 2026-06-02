package com.internance.auth.application.service;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.internance.auth.domain.exception.AuthErrorCode;
import com.internance.auth.domain.model.User;
import com.internance.auth.infrastructure.persistence.UserRepository;
import com.internance.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    /** Public sign-up always creates a plain user; privileged roles are assigned elsewhere. */
    private static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new user. The raw password is encoded before storage; the
     * username must be unique across all rows, including soft-deleted ones.
     *
     * @return the id of the newly created user
     * @throws BusinessException with {@link AuthErrorCode#DUPLICATE_USERNAME} if the username is taken
     */
    @Transactional
    public UUID signUp(String username, String rawPassword) {
        if (userRepository.existsByUsernameIncludingDeleted(username)) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_USERNAME);
        }

        User user = User.create(username, passwordEncoder.encode(rawPassword), DEFAULT_ROLE);
        try {
            // saveAndFlush so a concurrent insert that slipped past the check
            // above surfaces the unique-constraint violation here, not at commit.
            return userRepository.saveAndFlush(user).getId();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_USERNAME, e);
        }
    }
}
