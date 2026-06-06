package com.openlab.qualitos.quality.common;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Active la sécurité de méthode ({@code @PreAuthorize}) dans les tranches
 * {@code @WebMvcTest}, qui ne chargent pas la {@code SecurityConfig} de production
 * (laquelle porte {@code @EnableMethodSecurity}). À importer dans les tests qui
 * vérifient l'autorisation par rôle au niveau contrôleur (C1, H1).
 */
@TestConfiguration
@EnableMethodSecurity
public class MethodSecurityTestConfig {
}
