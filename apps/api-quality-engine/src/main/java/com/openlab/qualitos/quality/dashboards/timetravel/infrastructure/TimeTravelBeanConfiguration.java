package com.openlab.qualitos.quality.dashboards.timetravel.infrastructure;

import com.openlab.qualitos.quality.dashboards.application.TenantProvider;
import com.openlab.qualitos.quality.dashboards.timetravel.application.TimeTravelService;
import com.openlab.qualitos.quality.dashboards.timetravel.domain.KpiAsOfRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeTravelBeanConfiguration {

    @Bean
    public TimeTravelService timeTravelService(
            KpiAsOfRepository repo,
            @Qualifier("dashboardsTenantContextProvider") TenantProvider tenantProvider,
            Clock clock) {
        return new TimeTravelService(repo, tenantProvider, clock);
    }
}
