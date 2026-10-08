package com.openlab.qualitos.quality.circuit.infrastructure;

import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.quality.circuit.application.CircuitService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Câblage des circuits de validation : un service sans Spring, des adaptateurs. */
@Configuration
public class CircuitBeanConfiguration {

    @Bean
    public CircuitService circuitService(CircuitStepRepository steps, CircuitRunRepository runs,
                                         AuthorizationService authorization, AuditEventService auditEvents,
                                         Clock clock) {
        return new CircuitService(
                new CircuitAdapters.Circuits(steps, clock),
                new CircuitAdapters.Runs(runs),
                new CircuitAdapters.Context(authorization),
                new CircuitAdapters.Audit(auditEvents),
                clock);
    }
}
