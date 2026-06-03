package com.internance.auth.domain.exception;

import com.internance.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Auth-service-specific error codes. Codes are prefixed with {@code A} to
 * distinguish them from the shared {@code G*} codes in
 * {@link com.internance.common.exception.GlobalErrorCode}.
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    DUPLICATE_USERNAME("A409", "Username already exists", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("A401", "Invalid username or password", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("A401_1", "Invalid or expired token", HttpStatus.UNAUTHORIZED);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
