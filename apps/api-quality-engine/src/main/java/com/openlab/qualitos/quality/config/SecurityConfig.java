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
                // On cible les actions d'ADMINISTRATION (plateforme/tenant), d'INTÉGRITÉ
                // et les SUPPRESSIONS, sans casser l'accès qualité courant : les écritures
                // métier (POST/PUT/PATCH de NC, CAPA, 5S, audits, ishikawa, pdca, dmaic…)
                // restent ouvertes à tout authentifié (quality_manager ET user terrain),
                // et la lecture (GET) reste authentifiée simple.
                //
                // Mapping des rôles (CLAUDE.md §16 → rôles realm Keycloak → ROLE_<UPPER>,
                // cf. jwtAuthenticationConverter + infra/keycloak/realm-export.json) :
                //   Super Admin   → super_admin   → ROLE_SUPER_ADMIN
                //   Admin Tenant  → admin_tenant  → ROLE_ADMIN_TENANT
                //   Manager Qualité → quality_manager → ROLE_QUALITY_MANAGER
                // NB : le realm n'a PAS de rôle "admin" nu ; on conserve ROLE_ADMIN par
                // compat (api-core / tokens legacy) MAIS on ajoute ROLE_ADMIN_TENANT pour
                // ne pas verrouiller l'admin de tenant réel (sinon lock-out — cf. ADR 0020).

                // Activation/désactivation des Industry Packs : Admin Tenant / Super Admin.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/industry-packs/*/activate")
                    .hasAnyRole("ADMIN", "ADMIN_TENANT", "SUPER_ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/industry-packs/*/activate")
                    .hasAnyRole("ADMIN", "ADMIN_TENANT", "SUPER_ADMIN")

                // Activation des modules tenant (transitions d'abonnement) : Admin Tenant / Super Admin.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/tenant-modules/**")
                    .hasAnyRole("ADMIN", "ADMIN_TENANT", "SUPER_ADMIN")

                // Clés API (création/révocation) : Admin Tenant / Super Admin.
                .requestMatchers("/api/v1/api-keys/**").hasAnyRole("ADMIN", "ADMIN_TENANT", "SUPER_ADMIN")

                // Configuration des webhooks sortants : Admin Tenant / Super Admin.
                .requestMatchers("/api/v1/webhooks/**").hasAnyRole("ADMIN", "ADMIN_TENANT", "SUPER_ADMIN")

                // Connecteur EHR / HL7 FHIR (§13.3) : gestion des connexions + sync = action
                // d'intégration sensible (secrets, import de signaux patient-safety).
                // Réservé Admin / Admin Tenant / Super Admin.
                .requestMatchers("/api/v1/ehr/**").hasAnyRole("ADMIN", "ADMIN_TENANT", "SUPER_ADMIN")

                // Connecteur ERP (SAP / Oracle Fusion / Dynamics, §13.3) : connexions + sync =
                // administration d'intégration tenant (manipule des credentials chiffrés et
                // importe fournisseurs/KPIs). Réservé Admin / Admin Tenant / Super Admin.
                .requestMatchers("/api/v1/erp/**").hasAnyRole("ADMIN", "ADMIN_TENANT", "SUPER_ADMIN")

                // Configuration des connecteurs de communication (Teams/Slack/Mattermost) :
                // intégration sortante = administration. Admin Tenant / Super Admin.
                .requestMatchers("/api/v1/comm/**").hasAnyRole("ADMIN", "ADMIN_TENANT", "SUPER_ADMIN")

                // Marketplace de packs normatifs (§8.11) : publication/retrait = administration
                // plateforme. Réservé Admin Tenant / Super Admin (la lecture du catalogue passe
                // par GET, gardé ci-dessous au niveau méthode pour rester consultable).
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/marketplace/**")
                    .hasAnyRole("ADMIN", "ADMIN_TENANT", "SUPER_ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/v1/marketplace/**")
                    .hasAnyRole("ADMIN", "ADMIN_TENANT", "SUPER_ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/marketplace/**")
                    .hasAnyRole("ADMIN", "ADMIN_TENANT", "SUPER_ADMIN")

                // Designer de workflow BPMN no-code (§5.4) : l'édition de processus
                // (création / mise à jour / publication / archivage) est une action de
                // pilotage qualité → Admin / Admin Tenant / Super Admin / Manager Qualité.
                // La lecture (GET) reste ouverte aux authentifiés ; la suppression (DELETE)
                // est couverte par la règle DELETE générique plus bas (mêmes rôles).
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/workflow/**")
                    .hasAnyRole("ADMIN", "ADMIN_TENANT", "SUPER_ADMIN", "QUALITY_MANAGER")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/v1/workflow/**")
                    .hasAnyRole("ADMIN", "ADMIN_TENANT", "SUPER_ADMIN", "QUALITY_MANAGER")

                // Déclenchement d'un batch d'ancrage blockchain : Admin + Manager Qualité
                // (action d'intégrité légitime côté pilotage qualité). La vérification (GET)
                // reste ouverte aux authentifiés.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/blockchain/anchor/run")
                    .hasAnyRole("ADMIN", "ADMIN_TENANT", "SUPER_ADMIN", "QUALITY_MANAGER")

                // NB : l'écriture directe du journal d'audit (POST /api/v1/audit/events) est
                // verrouillée par @PreAuthorize sur le contrôleur (C1) en plus de la couche URL.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/audit/events")
                    .hasAnyRole("ADMIN", "ADMIN_TENANT", "SUPER_ADMIN")

                // --- Suppressions (DELETE) sur les ressources qualité : action sensible ---
                // Réservées Admin / Super Admin / Manager Qualité (CLAUDE.md §16 : un manager
                // qualité supprime dans son périmètre ; un simple "user" non).
                // CARVE-OUTS terrain : on NE gate PAS les suppressions de capture terrain
                // (NC/5S/audit), explicitement laissées ouvertes aux opérateurs authentifiés
                // (cf. consigne "ne jamais gater les écritures terrain NC/5S/audit").
                // Ces matchers doivent précéder la règle DELETE générique (premier match gagne).
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/nc/*/photos/**")
                    .authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/fives/**")
                    .authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/audit/**")
                    .authenticated()
                // Annotations collaboratives de dashboard (§7.3) : la suppression est
                // gouvernée au niveau use-case (auteur OU admin tenant). On laisse donc
                // passer tout authentifié ici pour ne pas verrouiller l'auteur "user"
                // simple de supprimer SON propre commentaire (l'autorisation fine 403
                // est appliquée dans DashboardAnnotationService).
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/dashboards/annotations/**")
                    .authenticated()
                // Suppression générique de ressource qualité : Manager Qualité ou plus.
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/**")
                    .hasAnyRole("ADMIN", "ADMIN_TENANT", "SUPER_ADMIN", "QUALITY_MANAGER")

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
