package com.internance.auth.infrastructure.config;

import com.internance.auth.infrastructure.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Stateless token-based API: no server-side session, no CSRF tokens.
        // Actuator endpoints (health, prometheus) are scraped by the monitoring
        // stack, and sign-up (POST /users/signup) plus login/refresh must be reachable
        // unauthenticated; everything else still requires authentication until
        // the JWT filter is wired in.
        http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.requestMatchers(
                        "/actuator/health", "/actuator/health/**", "/actuator/prometheus")
                .permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/docs/**")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/users/signup")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout")
                .permitAll()
                .anyRequest()
                .authenticated());
        return http.build();
    }
}
