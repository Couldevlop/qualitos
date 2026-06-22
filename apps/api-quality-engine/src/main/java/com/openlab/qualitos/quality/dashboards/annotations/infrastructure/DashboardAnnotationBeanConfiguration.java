package com.openlab.qualitos.quality.dashboards.annotations.infrastructure;

import com.openlab.qualitos.quality.dashboards.annotations.application.ActorRoles;
import com.openlab.qualitos.quality.dashboards.annotations.application.DashboardAnnotationService;
import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotationRepository;
import com.openlab.qualitos.quality.dashboards.application.TenantProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DashboardAnnotationBeanConfiguration {

    @Bean
    public DashboardAnnotationService dashboardAnnotationService(
            DashboardAnnotationRepository repo,
            @Qualifier("dashboardsTenantContextProvider") TenantProvider tenantProvider,
            @Qualifier("dashboardAnnotationActorRoles") ActorRoles actorRoles,
            Clock clock) {
        return new DashboardAnnotationService(repo, tenantProvider, actorRoles, clock);
    }
}
