package com.openlab.qualitos.quality.capa.effectiveness.infrastructure;

import com.openlab.qualitos.quality.capa.effectiveness.application.CapaEffectivenessService;
import com.openlab.qualitos.quality.capa.effectiveness.application.ClosedCapaPort;
import com.openlab.qualitos.quality.capa.effectiveness.application.NcOccurrencePort;
import com.openlab.qualitos.quality.capa.effectiveness.application.TenantProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CapaEffectivenessBeanConfiguration {

    @Bean
    public CapaEffectivenessService capaEffectivenessService(
            ClosedCapaPort closedCapas,
            NcOccurrencePort occurrences,
            @Qualifier("capaEffectivenessTenantContextProvider") TenantProvider tenantProvider,
            Clock clock) {
        return new CapaEffectivenessService(closedCapas, occurrences, tenantProvider, clock);
    }
}
