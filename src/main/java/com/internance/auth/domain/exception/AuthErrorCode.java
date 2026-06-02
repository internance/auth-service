package com.internance.auth.domain.exception;

import org.springframework.http.HttpStatus;

import com.internance.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Auth-service-specific error codes. Codes are prefixed with {@code A} to
 * distinguish them from the shared {@code G*} codes in
 * {@link com.internance.common.exception.GlobalErrorCode}.
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    DUPLICATE_USERNAME("A409", "Username already exists", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
