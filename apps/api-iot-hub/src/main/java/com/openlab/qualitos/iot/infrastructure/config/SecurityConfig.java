package com.openlab.qualitos.iot.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
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
            .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(o -> o.jwt(j -> {}));
    return http.build();
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
