package com.openlab.qualitos.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration de l'auditing JPA isolée dans sa propre classe.
 * L'isolation permet aux tests @WebMvcTest de ne pas charger le contexte JPA.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
