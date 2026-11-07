package com.openlab.qualitos.quality.capa.effectiveness.infrastructure;

import com.openlab.qualitos.quality.capa.effectiveness.application.CapaEffectivenessService;
import com.openlab.qualitos.quality.capa.effectiveness.application.EffectivenessBoardService;
import com.openlab.qualitos.quality.capa.effectiveness.application.EffectivenessReviewPort;
import com.openlab.qualitos.quality.capa.effectiveness.application.EvaluationCasePort;
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

    /**
     * Le plan de travail des évaluations (ADR 0108).
     *
     * <p>Déclaré ici et non annoté {@code @Service} : la couche application ne
     * connaît pas Spring, c'est l'invariant que l'ArchUnit du dépôt tient pour
     * ce module. Le même choix que pour le service de mesure juste au-dessus.
     */
    @Bean
    public EffectivenessBoardService effectivenessBoardService(
            EvaluationCasePort evaluationCases,
            EffectivenessReviewPort reviews,
            @Qualifier("capaEffectivenessTenantContextProvider") TenantProvider tenantProvider) {
        return new EffectivenessBoardService(evaluationCases, reviews, tenantProvider);
    }
}
