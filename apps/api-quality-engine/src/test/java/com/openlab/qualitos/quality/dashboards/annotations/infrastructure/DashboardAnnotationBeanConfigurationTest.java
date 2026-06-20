package com.openlab.qualitos.quality.dashboards.annotations.infrastructure;

import com.openlab.qualitos.quality.dashboards.annotations.application.ActorRoles;
import com.openlab.qualitos.quality.dashboards.annotations.application.DashboardAnnotationService;
import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotationRepository;
import com.openlab.qualitos.quality.dashboards.application.TenantProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardAnnotationBeanConfigurationTest {

    @Test
    void wiresService() {
        DashboardAnnotationService svc = new DashboardAnnotationBeanConfiguration()
                .dashboardAnnotationService(
                        Mockito.mock(DashboardAnnotationRepository.class),
                        Mockito.mock(TenantProvider.class),
                        Mockito.mock(ActorRoles.class),
                        Clock.systemUTC());
        assertThat(svc).isNotNull();
    }
}
