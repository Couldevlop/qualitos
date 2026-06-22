package com.openlab.qualitos.quality.dashboards.export.infrastructure;

import com.openlab.qualitos.crypto.application.HybridSignatureService;
import com.openlab.qualitos.quality.dashboards.export.application.DashboardAnchorPort;
import com.openlab.qualitos.quality.dashboards.export.application.DashboardExportAuditPort;
import com.openlab.qualitos.quality.dashboards.export.application.DashboardExportService;
import com.openlab.qualitos.quality.dashboards.export.application.DashboardLayoutLoaderPort;
import com.openlab.qualitos.quality.dashboards.export.application.ExportTenantProvider;
import com.openlab.qualitos.quality.dashboards.export.application.VerifyUrlBuilder;
import com.openlab.qualitos.quality.dashboards.export.domain.DashboardExportRepository;
import com.openlab.qualitos.quality.dashboards.export.domain.DashboardPdfRenderPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Clock;

@Configuration
public class DashboardExportBeanConfiguration {

    @Bean
    public DashboardExportService dashboardExportService(
            DashboardLayoutLoaderPort layoutLoader,
            DashboardPdfRenderPort renderer,
            HybridSignatureService signer,
            DashboardAnchorPort anchor,
            DashboardExportRepository repository,
            DashboardExportAuditPort audit,
            VerifyUrlBuilder verifyUrlBuilder,
            @Qualifier("dashboardExportTenantProvider") ExportTenantProvider tenantProvider,
            Clock clock) {
        return new DashboardExportService(
                layoutLoader, renderer, signer, anchor, repository, audit,
                verifyUrlBuilder, tenantProvider, new SecureRandom(), clock);
    }
}
