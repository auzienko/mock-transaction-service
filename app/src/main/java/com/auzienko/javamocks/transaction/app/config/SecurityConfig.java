package com.auzienko.javamocks.transaction.app.config;

import com.auzienko.javamocks.transaction.app.config.props.IdempotencyFilterProperties;
import com.auzienko.javamocks.transaction.app.config.props.RequestLoggingFilterProperties;
import com.auzienko.javamocks.transaction.app.filter.ApiKeyAuthFilter;
import com.auzienko.javamocks.transaction.app.filter.IdempotencyFilter;
import com.auzienko.javamocks.transaction.app.filter.RequestLoggingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties({RequestLoggingFilterProperties.class, IdempotencyFilterProperties.class})
public class SecurityConfig {

    private final ApiKeyAuthFilter apiKeyAuthFilter;
    private final RequestLoggingFilter requestLoggingFilter;
    private final IdempotencyFilter idempotencyFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()
                        .anyRequest().hasAnyRole("USER", "INTERNAL_SERVICE")
                )
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(requestLoggingFilter, ApiKeyAuthFilter.class)
                .addFilterBefore(idempotencyFilter, RequestLoggingFilter.class);


        return http.build();
    }
}
