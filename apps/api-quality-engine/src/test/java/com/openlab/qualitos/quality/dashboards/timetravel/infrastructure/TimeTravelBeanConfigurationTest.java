package com.openlab.qualitos.quality.dashboards.timetravel.infrastructure;

import com.openlab.qualitos.quality.dashboards.application.TenantProvider;
import com.openlab.qualitos.quality.dashboards.timetravel.application.TimeTravelService;
import com.openlab.qualitos.quality.dashboards.timetravel.domain.KpiAsOfRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class TimeTravelBeanConfigurationTest {

    @Test
    void wiresService() {
        TimeTravelService svc = new TimeTravelBeanConfiguration()
                .timeTravelService(
                        Mockito.mock(KpiAsOfRepository.class),
                        Mockito.mock(TenantProvider.class),
                        Clock.systemUTC());
        assertThat(svc).isNotNull();
    }
}
