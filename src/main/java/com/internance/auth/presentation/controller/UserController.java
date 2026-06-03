package com.internance.auth.presentation.controller;

import com.internance.auth.application.service.UserService;
import com.internance.auth.presentation.dto.SignUpRequest;
import com.internance.auth.presentation.dto.SignUpResponse;
import com.internance.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignUpResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        UUID userId = userService.signUp(request.username(), request.password());
        return ResponseEntity.created(URI.create("/api/v1/users/" + userId))
                .body(ApiResponse.success(new SignUpResponse(userId)));
    }
}
