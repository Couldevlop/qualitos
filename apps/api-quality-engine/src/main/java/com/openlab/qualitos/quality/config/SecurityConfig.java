package com.openlab.qualitos.quality.config;

import com.openlab.qualitos.quality.common.TenantJwtFilter;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public TenantJwtFilter tenantJwtFilter(JwtDecoder jwtDecoder) {
        return new TenantJwtFilter(jwtDecoder);
    }

    @Value("${qualitos.cors.allowed-origins:http://localhost:4200}")
    private String allowedOriginsCsv;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.asList(allowedOriginsCsv.split(",")));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","Accept","X-Requested-With","X-Tenant-Id","X-Correlation-Id"));
        cfg.setExposedHeaders(List.of("Location","X-Correlation-Id"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /**
     * CorsFilter enregistre en HIGHEST_PRECEDENCE : tourne AVANT la chaine
     * Spring Security, donc le preflight OPTIONS recoit ses headers CORS et
     * ne hit jamais l'auth. Solution officielle Spring Boot pour les API
     * stateless qui doivent repondre aux preflights cross-origin.
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration(
            @org.springframework.beans.factory.annotation.Qualifier("corsConfigurationSource")
            CorsConfigurationSource source) {
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   TenantJwtFilter tenantJwtFilter,
                                                   @org.springframework.beans.factory.annotation.Qualifier("corsConfigurationSource")
                                                   CorsConfigurationSource corsSource) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsSource))
            .csrf(AbstractHttpConfigurer::disable)
            // En-têtes de sécurité (OWASP A05 — Security Misconfiguration). API JSON
            // stateless : CSP très restrictive, pas d'embarquement en iframe.
            .headers(headers -> headers
                .contentSecurityPolicy(csp ->
                    csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(ref -> ref.policy(ReferrerPolicy.NO_REFERRER))
                .permissionsPolicyHeader(pp ->
                    pp.policy("geolocation=(), microphone=(), camera=()")))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // CORS preflight: doit passer sans token.
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // --- H1 (OWASP A01) : durcissement des endpoints SENSIBLES ---
                // On cible UNIQUEMENT les actions d'administration / d'intégrité, pour ne
                // pas casser l'accès qualité courant (NC, CAPA, audits, 5S restent ouverts
                // à tout utilisateur authentifié — quality_manager / user). La matrice de
                // rôles complète (CLAUDE.md §16) reste à propager par endpoint (DETTE/SUIVI).
                // Noms de rôles = realm_access.roles Keycloak mappés en ROLE_<UPPER>
                // (cf. jwtAuthenticationConverter). On s'aligne sur api-core (ADMIN / SUPER_ADMIN).

                // Activation/désactivation des Industry Packs : admin.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/industry-packs/*/activate")
                    .hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/industry-packs/*/activate")
                    .hasAnyRole("ADMIN", "SUPER_ADMIN")

                // Activation des modules tenant (toutes les transitions d'état d'abonnement) : admin.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/tenant-modules/**")
                    .hasAnyRole("ADMIN", "SUPER_ADMIN")

                // Clés API (création/révocation) : admin.
                .requestMatchers("/api/v1/api-keys/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                // Configuration des webhooks sortants : admin.
                .requestMatchers("/api/v1/webhooks/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                // Déclenchement d'un batch d'ancrage blockchain : admin + responsable qualité
                // (action d'intégrité légitime côté pilotage qualité). La vérification (GET)
                // reste ouverte aux authentifiés.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/blockchain/anchor/run")
                    .hasAnyRole("ADMIN", "SUPER_ADMIN", "QUALITY_MANAGER")

                // NB : l'écriture directe du journal d'audit (POST /api/v1/audit/events) est
                // verrouillée par @PreAuthorize sur le contrôleur (C1) en plus de la couche URL.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/audit/events")
                    .hasAnyRole("ADMIN", "SUPER_ADMIN")

                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .addFilterBefore(tenantJwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

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
