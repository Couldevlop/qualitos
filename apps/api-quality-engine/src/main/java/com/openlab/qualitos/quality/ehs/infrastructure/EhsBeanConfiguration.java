package com.openlab.qualitos.quality.ehs.infrastructure;

import com.openlab.qualitos.quality.ehs.application.IncidentService;
import com.openlab.qualitos.quality.ehs.application.TenantProvider;
import com.openlab.qualitos.quality.ehs.domain.IncidentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Câblage Spring : expose le service application en bean en lui injectant
 * les ports résolus depuis l'infrastructure. Le service reste un POJO pur.
 */
@Configuration
public class EhsBeanConfiguration {

    @Bean
    public IncidentService ehsIncidentService(IncidentRepository repo,
                                              TenantProvider tenantProvider,
                                              Clock clock) {
        return new IncidentService(repo, tenantProvider, clock);
    }

    /**
     * Clock par défaut si aucun autre n'est défini dans le contexte.
     * Marqué non-primary pour ne pas écraser un éventuel Clock de test.
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
    public Clock systemClock() { return Clock.systemUTC(); }
}
