package com.internance.auth.presentation.controller;

import com.internance.auth.application.service.AuthService;
import com.internance.auth.application.service.TokenResult;
import com.internance.auth.presentation.dto.LoginRequest;
import com.internance.auth.presentation.dto.RefreshRequest;
import com.internance.auth.presentation.dto.TokenResponse;
import com.internance.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResult result = authService.login(request.username(), request.password());
        return ApiResponse.success(TokenResponse.from(result));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenResult result = authService.refresh(request.refreshToken());
        return ApiResponse.success(TokenResponse.from(result));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.success();
    }
}
