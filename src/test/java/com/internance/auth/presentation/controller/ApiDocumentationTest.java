package com.internance.auth.presentation.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internance.auth.TestcontainersConfiguration;
import com.internance.auth.domain.model.User;
import com.internance.auth.infrastructure.persistence.UserRepository;
import com.internance.auth.presentation.dto.LoginRequest;
import com.internance.auth.presentation.dto.RefreshRequest;
import com.internance.auth.presentation.dto.SignUpRequest;
import com.internance.common.apispec.ApiDocSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates the OpenAPI spec via Spring REST Docs + restdocs-api-spec (ePages),
 * using the shared {@link ApiDocSupport} preprocessors. Each endpoint documents
 * its success and error responses (so the error codes show up in Swagger);
 * snippets for the same path+method are merged into one operation. Tags are
 * numbered so Swagger UI (tags-sorter=alpha) lists the flow from sign-up first.
 *
 * <p>Run {@code ./gradlew openapi3} to aggregate the snippets into the spec.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Transactional
class ApiDocumentationTest {

    private static final String SIGNUP_TAG = "1. Sign up";
    private static final String AUTH_TAG = "2. Authentication";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void seedUser() {
        userRepository.saveAndFlush(User.create("docuser", passwordEncoder.encode("password123"), "USER"));
    }

    @AfterEach
    void flushRedis() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    // --- Sign up -----------------------------------------------------------

    @Test
    void signUp() throws Exception {
        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignUpRequest("newuser", "password123"))))
                .andExpect(status().isCreated())
                .andDo(document(
                        "users-signup",
                        ApiDocSupport.requestPreprocessor(),
                        ApiDocSupport.responsePreprocessor(),
                        resource(ResourceSnippetParameters.builder()
                                .tag(SIGNUP_TAG)
                                .summary("Sign up")
                                .description("Registers a new user and returns the created id.")
                                .requestFields(signUpRequestFields())
                                .responseFields(
                                        fieldWithPath("success").description("Whether the request succeeded"),
                                        fieldWithPath("data.userId").description("Id of the created user"))
                                .build())));
    }

    @Test
    void signUp_duplicateUsername_conflict() throws Exception {
        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignUpRequest("docuser", "password123"))))
                .andExpect(status().isConflict())
                .andDo(document(
                        "users-signup-conflict",
                        ApiDocSupport.requestPreprocessor(),
                        ApiDocSupport.responsePreprocessor(),
                        resource(ResourceSnippetParameters.builder()
                                .tag(SIGNUP_TAG)
                                .summary("Sign up")
                                .description("A409 when the username is already taken.")
                                .requestFields(signUpRequestFields())
                                .responseFields(errorResponseFields())
                                .build())));
    }

    @Test
    void signUp_invalidPayload_badRequest() throws Exception {
        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignUpRequest("ab", "short"))))
                .andExpect(status().isBadRequest())
                .andDo(document(
                        "users-signup-invalid",
                        ApiDocSupport.requestPreprocessor(),
                        ApiDocSupport.responsePreprocessor(),
                        resource(ResourceSnippetParameters.builder()
                                .tag(SIGNUP_TAG)
                                .summary("Sign up")
                                .description("G400_1 when the payload fails validation.")
                                .requestFields(signUpRequestFields())
                                .responseFields(errorResponseFields())
                                .build())));
    }

    // --- Login -------------------------------------------------------------

    @Test
    void login() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("docuser", "password123"))))
                .andExpect(status().isOk())
                .andDo(document(
                        "auth-login",
                        ApiDocSupport.requestPreprocessor(),
                        ApiDocSupport.responsePreprocessor(),
                        resource(ResourceSnippetParameters.builder()
                                .tag(AUTH_TAG)
                                .summary("Login")
                                .description("Verifies credentials and issues an access/refresh token pair.")
                                .requestFields(credentialsRequestFields())
                                .responseFields(tokenResponseFields())
                                .build())));
    }

    @Test
    void login_invalidCredentials_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("docuser", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andDo(document(
                        "auth-login-unauthorized",
                        ApiDocSupport.requestPreprocessor(),
                        ApiDocSupport.responsePreprocessor(),
                        resource(ResourceSnippetParameters.builder()
                                .tag(AUTH_TAG)
                                .summary("Login")
                                .description("A401 when the username or password is wrong.")
                                .requestFields(credentialsRequestFields())
                                .responseFields(errorResponseFields())
                                .build())));
    }

    // --- Refresh -----------------------------------------------------------

    @Test
    void refresh() throws Exception {
        String refreshToken = loginResponse().at("/data/refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isOk())
                .andDo(document(
                        "auth-refresh",
                        ApiDocSupport.requestPreprocessor(),
                        ApiDocSupport.responsePreprocessor(),
                        resource(ResourceSnippetParameters.builder()
                                .tag(AUTH_TAG)
                                .summary("Refresh tokens")
                                .description("Rotates the refresh token and issues a new token pair.")
                                .requestFields(refreshRequestFields())
                                .responseFields(tokenResponseFields())
                                .build())));
    }

    @Test
    void refresh_invalidToken_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("not-a-jwt"))))
                .andExpect(status().isUnauthorized())
                .andDo(document(
                        "auth-refresh-unauthorized",
                        ApiDocSupport.requestPreprocessor(),
                        ApiDocSupport.responsePreprocessor(),
                        resource(ResourceSnippetParameters.builder()
                                .tag(AUTH_TAG)
                                .summary("Refresh tokens")
                                .description("A401_1 when the refresh token is invalid, expired or already rotated.")
                                .requestFields(refreshRequestFields())
                                .responseFields(errorResponseFields())
                                .build())));
    }

    // --- Logout ------------------------------------------------------------

    @Test
    void logout() throws Exception {
        String refreshToken = loginResponse().at("/data/refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isOk())
                .andDo(document(
                        "auth-logout",
                        ApiDocSupport.requestPreprocessor(),
                        ApiDocSupport.responsePreprocessor(),
                        resource(ResourceSnippetParameters.builder()
                                .tag(AUTH_TAG)
                                .summary("Logout")
                                .description("Revokes the refresh token so the session can no longer be refreshed.")
                                .requestFields(refreshRequestFields())
                                .responseFields(fieldWithPath("success").description("Whether the request succeeded"))
                                .build())));
    }

    @Test
    void logout_invalidToken_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("not-a-jwt"))))
                .andExpect(status().isUnauthorized())
                .andDo(document(
                        "auth-logout-unauthorized",
                        ApiDocSupport.requestPreprocessor(),
                        ApiDocSupport.responsePreprocessor(),
                        resource(ResourceSnippetParameters.builder()
                                .tag(AUTH_TAG)
                                .summary("Logout")
                                .description("A401_1 when the refresh token cannot be parsed.")
                                .requestFields(refreshRequestFields())
                                .responseFields(errorResponseFields())
                                .build())));
    }

    // --- Shared field descriptors -----------------------------------------

    private static FieldDescriptor[] signUpRequestFields() {
        return new FieldDescriptor[] {
            fieldWithPath("username").description("Desired username (4-20 chars)"),
            fieldWithPath("password").description("Raw password (8-20 chars)")
        };
    }

    private static FieldDescriptor[] credentialsRequestFields() {
        return new FieldDescriptor[] {
            fieldWithPath("username").description("Username"),
            fieldWithPath("password").description("Password")
        };
    }

    private static FieldDescriptor[] refreshRequestFields() {
        return new FieldDescriptor[] {fieldWithPath("refreshToken").description("A refresh token")};
    }

    private static FieldDescriptor[] tokenResponseFields() {
        return new FieldDescriptor[] {
            fieldWithPath("success").description("Whether the request succeeded"),
            fieldWithPath("data.accessToken").description("Short-lived JWT access token"),
            fieldWithPath("data.refreshToken").description("Refresh token used to obtain new access tokens"),
            fieldWithPath("data.tokenType").description("Token type (always \"Bearer\")"),
            fieldWithPath("data.expiresIn").description("Access token lifetime in seconds")
        };
    }

    private static FieldDescriptor[] errorResponseFields() {
        return new FieldDescriptor[] {
            fieldWithPath("success").description("Always false for errors"),
            fieldWithPath("error.code").description("Machine-readable error code (e.g. A401, A409, G400_1)"),
            fieldWithPath("error.message").description("Human-readable error message")
        };
    }

    private JsonNode loginResponse() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("docuser", "password123"))))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body);
    }
}
