package com.openlab.qualitos.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Bean {@code Clock} unique du module.
 *
 * <p>Sans lui, tout service qui a besoin de l'heure courante (et veut rester
 * testable, donc l'obtenir par injection plutôt que par
 * {@code Instant.now()} en dur) doit soit exposer deux constructeurs — un vu
 * de Spring, un vu des tests — soit poser lui-même un bean {@code Clock}
 * quelque part au hasard. Les deux se sont déjà produits dans ce module ; le
 * bean central élimine le choix.
 *
 * <p>UTC et non {@code Clock.systemDefaultZone()} : c'est le fuseau dans
 * lequel {@code TIMESTAMPTZ} est comparé en base, quel que soit le fuseau du
 * serveur qui exécute le code.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
