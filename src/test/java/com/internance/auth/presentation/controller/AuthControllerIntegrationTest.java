package com.internance.auth.presentation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internance.auth.TestcontainersConfiguration;
import com.internance.auth.domain.model.User;
import com.internance.auth.infrastructure.persistence.UserRepository;
import com.internance.auth.presentation.dto.LoginRequest;
import com.internance.auth.presentation.dto.RefreshRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUser() {
        userRepository.saveAndFlush(User.create("frank", passwordEncoder.encode("password123"), "USER"));
    }

    @Test
    void login_withValidCredentials_returnsTokenPair() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("frank", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").isNumber());
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("frank", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("A401"));
    }

    @Test
    void login_withUnknownUser_returns401() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ghost", "password123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("A401"));
    }

    @Test
    void refresh_withValidToken_returnsNewTokenPair() throws Exception {
        String loginBody = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("frank", "password123"))))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String refreshToken =
                objectMapper.readTree(loginBody).at("/data/refreshToken").asText();

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void refresh_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("not-a-jwt"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("A401_1"));
    }

    @Test
    void refresh_rejectsAccessTokenUsedAsRefresh_returns401() throws Exception {
        String loginBody = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("frank", "password123"))))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String accessToken =
                objectMapper.readTree(loginBody).at("/data/accessToken").asText();

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(accessToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("A401_1"));
    }
}
