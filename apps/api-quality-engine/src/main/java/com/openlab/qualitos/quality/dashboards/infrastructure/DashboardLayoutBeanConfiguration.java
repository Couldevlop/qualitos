package com.openlab.qualitos.quality.dashboards.infrastructure;

import com.openlab.qualitos.quality.dashboards.application.DashboardLayoutService;
import com.openlab.qualitos.quality.dashboards.application.TenantProvider;
import com.openlab.qualitos.quality.dashboards.domain.DashboardLayoutRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DashboardLayoutBeanConfiguration {

    @Bean
    public DashboardLayoutService dashboardLayoutService(
            DashboardLayoutRepository repo,
            @Qualifier("dashboardsTenantContextProvider") TenantProvider tenantProvider,
            Clock clock) {
        return new DashboardLayoutService(repo, tenantProvider, clock);
    }
}
