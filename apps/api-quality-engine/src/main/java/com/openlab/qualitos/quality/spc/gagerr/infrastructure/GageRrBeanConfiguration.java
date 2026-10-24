package com.openlab.qualitos.quality.spc.gagerr.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.spc.capability.infrastructure.JwtSpcContext;
import com.openlab.qualitos.quality.spc.gagerr.application.GageRrStudyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Câblage Spring des études Gage R&amp;R. Le {@code Clock} est celui de
 * {@code CommonBeansConfiguration} ; le contexte du jeton est celui des études
 * de capabilité.
 */
@Configuration
public class GageRrBeanConfiguration {

    @Bean
    public GageRrStudyService gageRrStudyService(GageRrStudyJpaRepository studies,
                                                 AuditEventService auditEvents,
                                                 ObjectMapper objectMapper,
                                                 Clock clock) {
        return new GageRrStudyService(
                new GageRrRepositoryAdapter(studies, objectMapper),
                new JwtSpcContext(),
                new AuditLogGageRrPublisher(auditEvents),
                clock);
    }
}
