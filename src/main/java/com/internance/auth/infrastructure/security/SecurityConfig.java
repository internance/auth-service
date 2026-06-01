package com.internance.auth.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		// Actuator endpoints (health, prometheus) are scraped by the monitoring
		// stack, so they must bypass authentication. Everything else still
		// requires it until the JWT filter chain is wired in.
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/**").permitAll()
						.anyRequest().authenticated());
		return http.build();
	}

}
