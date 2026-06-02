package com.internance.auth.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		// Stateless token-based API: no server-side session, no CSRF tokens.
		// Actuator endpoints (health, prometheus) are scraped by the monitoring
		// stack and public sign-up must be reachable unauthenticated; everything
		// else still requires authentication until the JWT filter is wired in.
		http
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/signup").permitAll()
						.anyRequest().authenticated());
		return http.build();
	}

}
