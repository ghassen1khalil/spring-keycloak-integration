package com.example.medicalapi.config;

import com.example.medicalapi.security.JwtAuthConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthConverter jwtAuthConverter) throws Exception {
        logger.info("=== CONFIGURATION DE LA CHAÎNE DE SÉCURITÉ ===");
        logger.info("Initialisation de la SecurityFilterChain avec JWT Auth Converter");

        http
                .csrf(csrf -> {
                    logger.debug("Désactivation de CSRF");
                    csrf.disable();
                })
                .sessionManagement(session -> {
                    logger.debug("Configuration du mode de session: STATELESS");
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                })
                .authorizeHttpRequests(auth -> {
                    logger.info("--- Configuration des règles d'autorisation HTTP ---");

                    // >>> SWAGGER: autoriser l'accès public à la documentation
                    auth.requestMatchers("/swagger-ui", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    logger.info("Règle: PUBLIC /swagger-ui (Swagger UI)");
                    logger.info("Règle: PUBLIC /swagger-ui/** (Swagger UI Resources)");
                    logger.info("Règle: PUBLIC /swagger-ui.html (Swagger UI legacy)");

                    // >>> API DOCS: autoriser l'accès public aux définitions OpenAPI
                    auth.requestMatchers("/v3/api-docs", "/v3/api-docs/**").permitAll();
                    logger.info("Règle: PUBLIC /v3/api-docs (OpenAPI docs)");
                    logger.info("Règle: PUBLIC /v3/api-docs/** (OpenAPI docs)");

                    // >>> WEBJARS: autoriser l'accès public aux ressources statiques Swagger
                    auth.requestMatchers("/webjars/**").permitAll();
                    logger.info("Règle: PUBLIC /webjars/** (Swagger UI assets)");

                    auth.requestMatchers(HttpMethod.POST, "/api/medical-ressources")
                            .hasAuthority("permission:medical:create");
                    logger.info("Règle: POST /api/medical-ressources → permission:medical:create");

                    auth.requestMatchers(HttpMethod.GET, "/api/medical-ressources/**")
                            .hasAuthority("permission:medical:read");
                    logger.info("Règle: GET /api/medical-ressources/** → permission:medical:read");

                    auth.requestMatchers(HttpMethod.PUT, "/api/medical-ressources/**")
                            .hasAuthority("permission:medical:update");
                    logger.info("Règle: PUT /api/medical-ressources/** → permission:medical:update");

                    auth.requestMatchers(HttpMethod.PATCH, "/api/medical-ressources/**")
                            .hasAuthority("permission:medical:update");
                    logger.info("Règle: PATCH /api/medical-ressources/** → permission:medical:update");

                    auth.requestMatchers(HttpMethod.DELETE, "/api/medical-ressources/**")
                            .hasAuthority("permission:medical:delete");
                    logger.info("Règle: DELETE /api/medical-ressources/** → permission:medical:delete");

                    logger.info("Toutes les autres requêtes nécessitent une authentification");
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> {
                    logger.debug("Configuration du serveur de ressources OAuth2");
                    oauth2.jwt(jwt -> {
                        logger.debug("Activation du JWT avec conversion personnalisée");
                        jwt.jwtAuthenticationConverter(jwtAuthConverter);
                    });
                });

        logger.info("=== FIN DE LA CONFIGURATION DE LA CHAÎNE DE SÉCURITÉ ===");
        return http.build();
    }
}
