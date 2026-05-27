package com.openlab.qualitos.quality.blockchain.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Active le scheduler d'ancrage (ADR 0012) hors profil "test".
 */
@Configuration
@EnableScheduling
@Profile("!test")
public class AnchoringSchedulingConfig {
}
