package com.openlab.qualitos.quality.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;

/**
 * Beans partages transverses au module quality-engine. Volontairement minimal
 * (un Clock global) — eviter d'y mettre quoi que ce soit qui appartient
 * legitimement a un module fonctionnel.
 */
@Configuration
public class CommonBeansConfiguration {

    /**
     * Horloge systeme UTC. Permet aux services qui injectent Clock (rate-limit,
     * tenant-modules, audit-event, etc.) d'avoir une horloge mockable en test
     * via @MockBean(Clock.class).
     */
    @Bean
    @Primary
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
