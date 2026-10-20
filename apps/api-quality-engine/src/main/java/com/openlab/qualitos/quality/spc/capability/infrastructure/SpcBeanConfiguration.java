package com.openlab.qualitos.quality.spc.capability.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.spc.capability.application.SpcStudyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Câblage Spring des études SPC.
 *
 * <p>Le {@code Clock} vient de {@code CommonBeansConfiguration}, unique et
 * {@code @Primary} ; en déclarer un second ici rendrait l'injection ambiguë
 * pour les autres modules.
 */
@Configuration
public class SpcBeanConfiguration {

    @Bean
    public SpcStudyService spcStudyService(SpcStudyJpaRepository studies,
                                           SpcSettingsJpaRepository settings,
                                           AuditEventService auditEvents,
                                           ObjectMapper objectMapper,
                                           Clock clock) {
        return new SpcStudyService(
                new SpcRepositoryAdapters.Studies(studies, objectMapper),
                new SpcRepositoryAdapters.Settings(settings, clock),
                new JwtSpcContext(),
                new AuditLogSpcPublisher(auditEvents),
                clock);
    }
}
