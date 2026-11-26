package com.openlab.qualitos.iot.infrastructure.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * OWASP security baseline for api-iot-hub.
 *
 * <ul>
 *   <li>A01 — deny-by-default; only /actuator/health and OpenAPI exposed unauthenticated.</li>
 *   <li>A02 — stateless JWT (resource server) — no session cookies.</li>
 *   <li>A05 — secure headers (X-Frame-Options DENY, X-CTO nosniff, Referrer-Policy, HSTS, CSP).</li>
 *   <li>A07 — JWT validated against Keycloak JWKS — issuer + audience + expiration enforced
 *           by NimbusJwtDecoder defaults (iss + exp). Audience enforced by config.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(h -> h
            .frameOptions(f -> f.deny())
            .contentSecurityPolicy(csp ->
                csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
            .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
            .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000).includeSubDomains(true)))
        .authorizeHttpRequests(a -> a
            // Les sondes Kubernetes interrogent /actuator/health/readiness et
            // /actuator/health/liveness, des SOUS-CHEMINS que le motif exact ne
            // couvrait pas : elles recevaient 401 et le pod ne passait jamais Ready.
            .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(rolesDuRealm())));
    return http.build();
  }

  /**
   * LES ROLES SE LISENT DANS {@code realm_access.roles}, et nulle part ailleurs.
   *
   * <p>Sans ce convertisseur, Spring applique le sien : il lit la revendication
   * {@code scope} et fabrique des autorites {@code SCOPE_*}. Aucune autorite
   * {@code ROLE_*} n'existait alors, donc AUCUN {@code @PreAuthorize} de ce
   * service ne pouvait passer -- registre d'equipements, telemetrie, FHIR,
   * LoRaWAN, Modbus, Sparkplug : tout chemin d'ecriture repondait 403, pour
   * tout le monde, y compris l'administrateur du client.
   *
   * <p>Le defaut a survecu parce que les bancs posaient eux-memes l'autorite
   * qu'ils verifiaient ({@code .authorities(...)}), ce qui saute le
   * convertisseur : ils restaient verts sur un service inaccessible. Le banc
   * {@code rolesDuRealm_*} passe desormais par le jeton.
   *
   * <p>Meme lecture que le moteur qualite, pour qu'un jeton ouvre les memes
   * portes des deux cotes.
   */
  @Bean
  public JwtAuthenticationConverter rolesDuRealm() {
    JwtAuthenticationConverter converteur = new JwtAuthenticationConverter();
    converteur.setJwtGrantedAuthoritiesConverter(jwt -> {
      Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
      if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
        return List.of();
      }
      List<GrantedAuthority> autorites = new ArrayList<>();
      for (Object role : roles) {
        if (role != null) {
          autorites.add(new SimpleGrantedAuthority(
              "ROLE_" + role.toString().toUpperCase(Locale.ROOT)));
        }
      }
      return autorites;
    });
    return converteur;
  }

  /**
   * Test-only decoder so {@code @WebMvcTest} / Testcontainers integration tests
   * don't have to bring up Keycloak. Production uses the auto-configured
   * NimbusJwtDecoder driven by {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}.
   */
  @Bean
  @Profile("test")
  public JwtDecoder testJwtDecoder() {
    // 256-bit symmetric secret — TEST ONLY, never used in prod profile
    byte[] secret = "test-only-symmetric-secret-32bytes!!".getBytes();
    return NimbusJwtDecoder.withSecretKey(
        new javax.crypto.spec.SecretKeySpec(secret, "HmacSHA256")).build();
  }
}
