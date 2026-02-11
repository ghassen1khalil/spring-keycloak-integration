package com.example.medicalapi.config;

import com.example.medicalapi.security.JwtAuthConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthConverter jwtAuthConverter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/medical-ressources").hasAuthority("permission:medical:create")
                .requestMatchers(HttpMethod.GET, "/api/medical-ressources/**").hasAuthority("permission:medical:read")
                .requestMatchers(HttpMethod.PUT, "/api/medical-ressources/**").hasAuthority("permission:medical:update")
                .requestMatchers(HttpMethod.PATCH, "/api/medical-ressources/**").hasAuthority("permission:medical:update")
                .requestMatchers(HttpMethod.DELETE, "/api/medical-ressources/**").hasAuthority("permission:medical:delete")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)));

        return http.build();
    }
}
