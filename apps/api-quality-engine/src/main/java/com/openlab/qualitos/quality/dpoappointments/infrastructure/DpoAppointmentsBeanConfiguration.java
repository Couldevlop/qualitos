package com.openlab.qualitos.quality.dpoappointments.infrastructure;

import com.openlab.qualitos.quality.dpoappointments.application.DpoAppointmentEventPublisher;
import com.openlab.qualitos.quality.dpoappointments.application.DpoAppointmentService;
import com.openlab.qualitos.quality.dpoappointments.application.TenantProvider;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointmentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DpoAppointmentsBeanConfiguration {

    @Bean
    public DpoAppointmentService dpoAppointmentService(
            DpoAppointmentRepository repo,
            @Qualifier("dpoTenantContextProvider") TenantProvider tenantProvider,
            DpoAppointmentEventPublisher events,
            Clock clock) {
        return new DpoAppointmentService(repo, tenantProvider, events, clock);
    }
}
