package com.openlab.qualitos.quality.costofquality.infrastructure;

import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.costofquality.application.CostOfQualityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Câblage Spring du coût de la qualité.
 *
 * <p>Le {@code Clock} vient de {@code CommonBeansConfiguration}, unique et
 * {@code @Primary} ; en déclarer un second ici rendrait l'injection ambiguë
 * pour les autres modules.
 */
@Configuration
public class CostOfQualityBeanConfiguration {

    @Bean
    public CostOfQualityService costOfQualityService(CoqEntryJpaRepository entries,
                                                     CoqLabelJpaRepository labels,
                                                     CoqSettingsJpaRepository settings,
                                                     AuditEventService auditEvents,
                                                     Clock clock) {
        return new CostOfQualityService(
                new CoqRepositoryAdapters.Entries(entries),
                new CoqRepositoryAdapters.Labels(labels, clock),
                new CoqRepositoryAdapters.Settings(settings, clock),
                new JwtCoqContext(),
                new AuditLogCoqPublisher(auditEvents),
                clock);
    }
}
