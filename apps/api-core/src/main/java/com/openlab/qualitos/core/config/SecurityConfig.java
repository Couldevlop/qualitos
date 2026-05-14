package com.openlab.qualitos.core.config;

import com.openlab.qualitos.core.security.TenantJwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Configuration Spring Security.
 *
 * <ul>
 *   <li>API stateless — CSRF désactivé, sessions STATELESS.</li>
 *   <li>OAuth2 Resource Server avec JWT Keycloak.</li>
 *   <li>/api/v1/tenants réservé ROLE_SUPER_ADMIN.</li>
 *   <li>/api/v1/users réservé ROLE_ADMIN + ROLE_SUPER_ADMIN.</li>
 *   <li>/actuator/health public.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Déclare TenantJwtFilter comme bean Spring pour l'injection dans la chaîne de sécurité.
     * Ne pas annoter TenantJwtFilter lui-même avec @Component pour éviter la double-registration
     * automatique par Spring Boot (OncePerRequestFilter est détecté automatiquement).
     */
    @Bean
    public TenantJwtFilter tenantJwtFilter(JwtDecoder jwtDecoder) {
        return new TenantJwtFilter(jwtDecoder);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   TenantJwtFilter tenantJwtFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Endpoints publics
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // OpenAPI / Swagger UI (dev)
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Gestion des tenants : Super Admin uniquement
                .requestMatchers("/api/v1/tenants/**").hasRole("SUPER_ADMIN")
                // Gestion des utilisateurs : Admin + Super Admin
                .requestMatchers("/api/v1/users/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                // Tout le reste nécessite une authentification
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            // TenantJwtFilter s'exécute AVANT l'authentification Spring Security
            // pour peupler TenantContext avant que les services ne soient appelés
            .addFilterBefore(tenantJwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Convertit les rôles Keycloak (realm_access.roles) en GrantedAuthority Spring Security.
     * Keycloak place les rôles dans le claim {@code realm_access.roles} par défaut.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return List.of();
            }
            @SuppressWarnings("unchecked")
            Collection<String> roles = (Collection<String>) realmAccess.get("roles");
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList());
        });
        return converter;
    }
}
