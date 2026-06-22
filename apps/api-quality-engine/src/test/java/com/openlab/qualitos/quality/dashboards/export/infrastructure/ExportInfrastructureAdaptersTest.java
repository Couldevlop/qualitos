package com.openlab.qualitos.quality.dashboards.export.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import com.openlab.qualitos.quality.dashboards.application.DashboardLayoutDto;
import com.openlab.qualitos.quality.dashboards.application.DashboardLayoutService;
import com.openlab.qualitos.quality.dashboards.export.domain.DashboardExport;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ExportInfrastructureAdaptersTest {

    @Test
    void beanConfiguration_wiresService() {
        var cfg = new DashboardExportBeanConfiguration();
        var service = cfg.dashboardExportService(
                id -> "n", (m, u) -> new byte[]{1},
                mock(com.openlab.qualitos.crypto.application.HybridSignatureService.class),
                (t, s) -> "tx", mock(com.openlab.qualitos.quality.dashboards.export.domain.DashboardExportRepository.class),
                (t, u, d, s, a) -> {}, code -> "url",
                new com.openlab.qualitos.quality.dashboards.export.application.ExportTenantProvider() {
                    public UUID requireTenantId() { return UUID.randomUUID(); }
                    public UUID requireUserId() { return UUID.randomUUID(); }
                },
                java.time.Clock.systemUTC());
        assertThat(service).isNotNull();
    }

    @Test
    void verifyUrlBuilder_buildsAbsoluteEncodedUrl() {
        var b = new ConfigurableVerifyUrlBuilder("https://app.qualitos.io/");
        assertThat(b.verifyUrl("abcDEF012345_-xy"))
                .isEqualTo("https://app.qualitos.io/api/v1/dashboards/public/exports/abcDEF012345_-xy/verify");
    }

    @Test
    void layoutLoader_delegatesToTenantScopedService() {
        DashboardLayoutService layoutService = mock(DashboardLayoutService.class);
        UUID id = UUID.randomUUID();
        when(layoutService.get(id)).thenReturn(new DashboardLayoutDto.View(
                id, UUID.randomUUID(), UUID.randomUUID(), "Exec", "d", "{}", false,
                "sig", 1, Instant.now(), Instant.now()));
        var adapter = new DashboardLayoutLoaderAdapter(layoutService);
        assertThat(adapter.requireVisibleName(id)).isEqualTo("Exec");
    }

    @Test
    void anchorAdapter_bridgesToBlockchainPort() {
        BlockchainAnchorPort port = mock(BlockchainAnchorPort.class);
        UUID tenant = UUID.randomUUID();
        when(port.submitRoot(tenant, "sha")).thenReturn("tx-9");
        assertThat(new DashboardAnchorAdapter(port).submitRoot(tenant, "sha")).isEqualTo("tx-9");
    }

    @Test
    void auditAdapter_recordsSignedExportEvent() {
        AuditEventService events = mock(AuditEventService.class);
        var adapter = new DashboardExportAuditAdapter(events);
        UUID tenant = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        UUID dash = UUID.randomUUID();

        adapter.recordExport(tenant, user, dash, "f".repeat(64), "tx-1");

        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(events).recordForTenant(eq(tenant), cap.capture());
        AuditEventDto.RecordEventRequest req = cap.getValue();
        assertThat(req.action()).isEqualTo("dashboard.export.signed");
        assertThat(req.resourceType()).isEqualTo("dashboard-export");
        assertThat(req.resourceId()).isEqualTo(dash);
        assertThat(req.actorType()).isEqualTo(ActorType.USER);
        assertThat(req.actorUserId()).isEqualTo(user);
        assertThat(req.payloadJson()).contains("f".repeat(64)).contains("tx-1");
    }

    @Test
    void repositoryAdapter_savesAndMapsBack() {
        DashboardExportJpaRepository jpa = mock(DashboardExportJpaRepository.class);
        when(jpa.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var adapter = new DashboardExportRepositoryAdapter(jpa);

        DashboardExport e = DashboardExport.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Exec",
                "abcDEF012345_-xy", "a".repeat(64), "env", "tx", Instant.now());
        DashboardExport saved = adapter.save(e);

        assertThat(e.getId()).isNotNull();         // id assigned back into the aggregate
        assertThat(saved.getVerificationCode()).isEqualTo("abcDEF012345_-xy");
        assertThat(saved.getSignatureEnvelope()).isEqualTo("env");
    }

    @Test
    void repositoryAdapter_findByCode_mapsDomain() {
        DashboardExportJpaRepository jpa = mock(DashboardExportJpaRepository.class);
        DashboardExportJpaEntity entity = new DashboardExportJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(UUID.randomUUID());
        entity.setUserId(UUID.randomUUID());
        entity.setDashboardId(UUID.randomUUID());
        entity.setDashboardName("Exec");
        entity.setVerificationCode("abcDEF012345_-xy");
        entity.setSha256Hex("a".repeat(64));
        entity.setSignatureEnvelope("env");
        entity.setAnchorTxRef("tx");
        entity.setCreatedAt(Instant.now());
        when(jpa.findByVerificationCode("abcDEF012345_-xy")).thenReturn(Optional.of(entity));

        var adapter = new DashboardExportRepositoryAdapter(jpa);
        Optional<DashboardExport> found = adapter.findByVerificationCode("abcDEF012345_-xy");
        assertThat(found).isPresent();
        assertThat(found.get().getDashboardName()).isEqualTo("Exec");
    }
}
