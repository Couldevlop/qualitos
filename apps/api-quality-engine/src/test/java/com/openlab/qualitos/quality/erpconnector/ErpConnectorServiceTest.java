package com.openlab.qualitos.quality.erpconnector;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.itsm.SecretCipher;
import com.openlab.qualitos.quality.kpi.KpiDefinition;
import com.openlab.qualitos.quality.kpi.KpiDefinitionRepository;
import com.openlab.qualitos.quality.kpi.KpiMeasurement;
import com.openlab.qualitos.quality.kpi.KpiMeasurementRepository;
import com.openlab.qualitos.quality.kpi.MeasurementSource;
import com.openlab.qualitos.quality.supplier.Supplier;
import com.openlab.qualitos.quality.supplier.SupplierRepository;
import com.openlab.qualitos.quality.supplier.SupplierType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ErpConnectorServiceTest {

    @Mock ErpConnectionRepository connectionRepo;
    @Mock SupplierRepository supplierRepo;
    @Mock KpiDefinitionRepository kpiDefinitionRepo;
    @Mock KpiMeasurementRepository kpiMeasurementRepo;
    @Mock SecretCipher cipher;
    @Mock ErpProviderClient sap;
    @Mock ErpProviderClient oracle;

    ErpConnectorService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID CONN = UUID.randomUUID();

    @BeforeEach
    void setup() {
        when(sap.provider()).thenReturn(ErpProvider.SAP);
        when(oracle.provider()).thenReturn(ErpProvider.ORACLE_FUSION);
        service = new ErpConnectorService(connectionRepo, supplierRepo, kpiDefinitionRepo,
                kpiMeasurementRepo, cipher, List.of(sap, oracle));
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    // ---------- CRUD + encryption ----------

    @Test
    void createConnection_persistsWithEncryptedSecret() {
        when(cipher.encrypt("supersecret")).thenReturn("CIPHER==");
        when(connectionRepo.save(any())).thenAnswer(inv -> {
            ErpConnection c = inv.getArgument(0);
            c.setId(CONN);
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            return c;
        });
        ErpDto.CreateConnectionRequest req = new ErpDto.CreateConnectionRequest(
                "Prod SAP", ErpProvider.SAP, "https://sap.example.com", "RFC_USER",
                "supersecret", "1000", USER);

        ErpDto.ConnectionResponse out = service.createConnection(req);

        ArgumentCaptor<ErpConnection> cap = ArgumentCaptor.forClass(ErpConnection.class);
        verify(connectionRepo).save(cap.capture());
        assertThat(cap.getValue().getCredentialCipher()).isEqualTo("CIPHER==");
        assertThat(cap.getValue().getTenantId()).isEqualTo(TENANT);
        assertThat(out.tenantId()).isEqualTo(TENANT);
        assertThat(out.status()).isEqualTo(ErpConnectionStatus.ACTIVE);
        // le secret n'est jamais ré-exposé : la réponse ne contient pas de champ secret
    }

    @Test
    void createConnection_withoutTenant_throws() {
        TenantContext.clear();
        ErpDto.CreateConnectionRequest req = new ErpDto.CreateConnectionRequest(
                "n", ErpProvider.SAP, "https://x", "u", "supersecret", null, USER);
        assertThatThrownBy(() -> service.createConnection(req))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void getConnection_otherTenant_appearsNotFound() {
        ErpConnection c = connection(); c.setTenantId(UUID.randomUUID());
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.getConnection(CONN))
                .isInstanceOf(ErpConnectionNotFoundException.class);
    }

    @Test
    void getConnection_missing_throwsNotFound() {
        when(connectionRepo.findById(CONN)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getConnection(CONN))
                .isInstanceOf(ErpConnectionNotFoundException.class);
    }

    @Test
    void updateConnection_changesFieldsAndReEncryptsSecret() {
        ErpConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.encrypt("new-secret")).thenReturn("NEW-CIPHER");
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateConnection(CONN, new ErpDto.UpdateConnectionRequest(
                "renamed", "https://new.example.com", "user2", "new-secret", "2000",
                ErpConnectionStatus.DISABLED));

        assertThat(c.getName()).isEqualTo("renamed");
        assertThat(c.getBaseUrl()).isEqualTo("https://new.example.com");
        assertThat(c.getUsername()).isEqualTo("user2");
        assertThat(c.getCredentialCipher()).isEqualTo("NEW-CIPHER");
        assertThat(c.getExternalScope()).isEqualTo("2000");
        assertThat(c.getStatus()).isEqualTo(ErpConnectionStatus.DISABLED);
    }

    @Test
    void updateConnection_reactivatingResetsFailures() {
        ErpConnection c = connection();
        c.setStatus(ErpConnectionStatus.DISABLED_ON_ERRORS);
        c.setConsecutiveFailures(15);
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateConnection(CONN, new ErpDto.UpdateConnectionRequest(
                null, null, null, null, null, ErpConnectionStatus.ACTIVE));

        assertThat(c.getStatus()).isEqualTo(ErpConnectionStatus.ACTIVE);
        assertThat(c.getConsecutiveFailures()).isZero();
    }

    @Test
    void deleteConnection_removesEntity() {
        ErpConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        service.deleteConnection(CONN);
        verify(connectionRepo).delete(c);
    }

    @Test
    void listConnections_paginated() {
        when(connectionRepo.findByTenantId(eq(TENANT), any()))
                .thenReturn(new PageImpl<>(List.of(connection())));
        Page<ErpDto.ConnectionResponse> out = service.listConnections(PageRequest.of(0, 10));
        assertThat(out.getTotalElements()).isOne();
    }

    @Test
    void clientsView_returnsRegisteredProviders() {
        assertThat(service.clientsView())
                .containsKeys(ErpProvider.SAP, ErpProvider.ORACLE_FUSION);
    }

    // ---------- Sync: guards ----------

    @Test
    void sync_disabled_returnsEarlyReport() {
        ErpConnection c = connection(); c.setStatus(ErpConnectionStatus.DISABLED);
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ErpDto.SyncReport r = service.sync(CONN);

        assertThat(r.errorMessage()).contains("DISABLED");
        verify(sap, never()).fetchSuppliers(any(), any());
    }

    @Test
    void sync_noClientForProvider_returnsReport() {
        ErpConnection c = connection(); c.setProvider(ErpProvider.DYNAMICS); // pas de bean Dynamics injecté
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ErpDto.SyncReport r = service.sync(CONN);

        assertThat(r.errorMessage()).contains("No client registered");
    }

    @Test
    void sync_decryptionFails_recordsFailure() {
        ErpConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.decrypt(any())).thenThrow(new IllegalStateException("Decryption failure"));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ErpDto.SyncReport r = service.sync(CONN);

        assertThat(r.errorMessage()).contains("decryption");
        assertThat(c.getConsecutiveFailures()).isOne();
    }

    @Test
    void sync_providerNetworkFailure_recordsErrorReport() {
        ErpConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.decrypt(any())).thenReturn("plain");
        when(sap.fetchSuppliers(any(), any()))
                .thenThrow(new ErpSyncException("SAP fetch failed: Connection refused"));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ErpDto.SyncReport r = service.sync(CONN);

        assertThat(r.errorMessage()).contains("fetch failed");
        assertThat(r.suppliersImported()).isZero();
        assertThat(c.getConsecutiveFailures()).isOne();
        assertThat(c.getStatus()).isEqualTo(ErpConnectionStatus.ACTIVE); // < 10
    }

    @Test
    void sync_autoDisablesAfterMaxFailures() {
        ErpConnection c = connection();
        c.setConsecutiveFailures(ErpConnectorService.MAX_CONSECUTIVE_FAILURES_BEFORE_DISABLE - 1);
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.decrypt(any())).thenReturn("plain");
        when(sap.fetchSuppliers(any(), any())).thenThrow(new ErpSyncException("still broken"));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.sync(CONN);

        assertThat(c.getStatus()).isEqualTo(ErpConnectionStatus.DISABLED_ON_ERRORS);
    }

    // ---------- Sync: supplier import (upsert idempotent) ----------

    @Test
    void sync_importsSuppliers_createsAndUpserts() {
        ErpConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.decrypt(any())).thenReturn("plain");
        when(sap.fetchSuppliers(any(), any())).thenReturn(List.of(
                new ExternalSupplier("SUP-1", "Acme Raw", "RAW_MATERIAL", "FRANCE", "a@acme.io"),
                new ExternalSupplier("SUP-2", "Beta Services", "SERVICE", "DE", null),
                new ExternalSupplier("", "ignored", "X", null, null))); // code vide -> ignoré
        when(sap.fetchProductionKpis(any(), any())).thenReturn(List.of());

        // SUP-1 nouveau, SUP-2 existant (upsert)
        when(supplierRepo.findByTenantIdAndCode(TENANT, "SUP-1")).thenReturn(Optional.empty());
        Supplier existing = new Supplier();
        existing.setId(UUID.randomUUID());
        existing.setTenantId(TENANT);
        existing.setCode("SUP-2");
        existing.setName("old name");
        when(supplierRepo.findByTenantIdAndCode(TENANT, "SUP-2")).thenReturn(Optional.of(existing));
        when(supplierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ErpDto.SyncReport r = service.sync(CONN);

        assertThat(r.suppliersImported()).isEqualTo(2);
        assertThat(r.suppliersIgnored()).isEqualTo(1);
        assertThat(r.errorMessage()).isNull();

        ArgumentCaptor<Supplier> cap = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepo, times(2)).save(cap.capture());
        // upsert : SUP-2 garde son id, son nom est rafraîchi depuis l'ERP
        Supplier upserted = cap.getAllValues().stream()
                .filter(s -> "SUP-2".equals(s.getCode())).findFirst().orElseThrow();
        assertThat(upserted.getId()).isEqualTo(existing.getId());
        assertThat(upserted.getName()).isEqualTo("Beta Services");
        assertThat(upserted.getSupplierType()).isEqualTo(SupplierType.SERVICE);
        // SUP-1 : country normalisé sur 2 lettres, type mappé
        Supplier created = cap.getAllValues().stream()
                .filter(s -> "SUP-1".equals(s.getCode())).findFirst().orElseThrow();
        assertThat(created.getCountryCode()).isEqualTo("FR");
        assertThat(created.getSupplierType()).isEqualTo(SupplierType.RAW_MATERIAL);
        assertThat(created.getContactEmail()).isEqualTo("a@acme.io");
    }

    @Test
    void sync_supplierUpsert_isIdempotent_secondRunDoesNotDuplicate() {
        ErpConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.decrypt(any())).thenReturn("plain");
        when(sap.fetchSuppliers(any(), any())).thenReturn(List.of(
                new ExternalSupplier("SUP-1", "Acme", "SERVICE", null, null)));
        when(sap.fetchProductionKpis(any(), any())).thenReturn(List.of());
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Supplier persisted = new Supplier();
        persisted.setId(UUID.randomUUID());
        persisted.setTenantId(TENANT);
        persisted.setCode("SUP-1");
        when(supplierRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // run 1: not found -> create
        when(supplierRepo.findByTenantIdAndCode(TENANT, "SUP-1")).thenReturn(Optional.empty());
        ErpDto.SyncReport r1 = service.sync(CONN);
        // run 2: found -> update same entity
        when(supplierRepo.findByTenantIdAndCode(TENANT, "SUP-1")).thenReturn(Optional.of(persisted));
        ErpDto.SyncReport r2 = service.sync(CONN);

        assertThat(r1.suppliersImported()).isOne();
        assertThat(r2.suppliersImported()).isOne();
        // jamais d'insertion concurrente : c'est le repo (clé unique tenant+code) qui garantit
        // l'unicité ; on vérifie qu'on a bien fait un upsert (lookup avant save) à chaque run.
        verify(supplierRepo, times(2)).findByTenantIdAndCode(TENANT, "SUP-1");
        verify(supplierRepo, times(2)).save(any());
    }

    // ---------- Sync: KPI mapping (existing code + WARN on unknown) ----------

    @Test
    void sync_mapsKpis_toExistingDefinition_andUpserts() {
        ErpConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.decrypt(any())).thenReturn("plain");
        when(sap.fetchSuppliers(any(), any())).thenReturn(List.of());
        Instant p1 = Instant.parse("2026-01-01T00:00:00Z");
        when(sap.fetchProductionKpis(any(), any())).thenReturn(List.of(
                new ExternalProductionKpi("OEE", new BigDecimal("82.5"), "%", p1,
                        Instant.parse("2026-02-01T00:00:00Z")),
                new ExternalProductionKpi("UNKNOWN_CODE", new BigDecimal("1"), null, p1, null)));

        KpiDefinition oee = new KpiDefinition();
        oee.setId(UUID.randomUUID());
        oee.setTenantId(TENANT);
        oee.setCode("OEE");
        oee.setUnit("%");
        when(kpiDefinitionRepo.findByTenantIdAndCode(TENANT, "OEE")).thenReturn(Optional.of(oee));
        when(kpiDefinitionRepo.findByTenantIdAndCode(TENANT, "UNKNOWN_CODE")).thenReturn(Optional.empty());
        when(kpiMeasurementRepo.findByKpiIdAndPeriodStart(oee.getId(), p1)).thenReturn(Optional.empty());
        when(kpiMeasurementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ErpDto.SyncReport r = service.sync(CONN);

        assertThat(r.kpisImported()).isOne();          // OEE
        assertThat(r.kpisIgnored()).isOne();           // UNKNOWN_CODE -> WARN, ignoré
        assertThat(r.errorMessage()).isNull();

        ArgumentCaptor<KpiMeasurement> cap = ArgumentCaptor.forClass(KpiMeasurement.class);
        verify(kpiMeasurementRepo).save(cap.capture());
        KpiMeasurement m = cap.getValue();
        assertThat(m.getKpiId()).isEqualTo(oee.getId());
        assertThat(m.getValue()).isEqualByComparingTo("82.5");
        assertThat(m.getSource()).isEqualTo(MeasurementSource.IMPORT);
        assertThat(m.getTenantId()).isEqualTo(TENANT);
        // jamais de création de KpiDefinition
        verify(kpiDefinitionRepo, never()).save(any());
    }

    @Test
    void sync_kpiUpsert_updatesExistingMeasurement() {
        ErpConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.decrypt(any())).thenReturn("plain");
        when(sap.fetchSuppliers(any(), any())).thenReturn(List.of());
        Instant p1 = Instant.parse("2026-01-01T00:00:00Z");
        when(sap.fetchProductionKpis(any(), any())).thenReturn(List.of(
                new ExternalProductionKpi("SCRAP", new BigDecimal("3.2"), null, p1, null)));

        KpiDefinition def = new KpiDefinition();
        def.setId(UUID.randomUUID());
        def.setTenantId(TENANT);
        def.setCode("SCRAP");
        def.setUnit("%");
        when(kpiDefinitionRepo.findByTenantIdAndCode(TENANT, "SCRAP")).thenReturn(Optional.of(def));

        KpiMeasurement existing = new KpiMeasurement();
        existing.setId(UUID.randomUUID());
        existing.setKpiId(def.getId());
        existing.setPeriodStart(p1);
        existing.setValue(new BigDecimal("9.9"));
        when(kpiMeasurementRepo.findByKpiIdAndPeriodStart(def.getId(), p1))
                .thenReturn(Optional.of(existing));
        when(kpiMeasurementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ErpDto.SyncReport r = service.sync(CONN);

        assertThat(r.kpisImported()).isOne();
        // même entité mise à jour (idempotence), valeur rafraîchie, unité héritée de la def
        assertThat(existing.getValue()).isEqualByComparingTo("3.2");
        assertThat(existing.getUnit()).isEqualTo("%");
        assertThat(existing.getPeriodEnd()).isNotNull(); // dérivée car non fournie
    }

    @Test
    void sync_ignoresKpiWithMissingValueOrPeriod() {
        ErpConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.decrypt(any())).thenReturn("plain");
        when(sap.fetchSuppliers(any(), any())).thenReturn(List.of());
        Instant p1 = Instant.parse("2026-01-01T00:00:00Z");
        when(sap.fetchProductionKpis(any(), any())).thenReturn(List.of(
                new ExternalProductionKpi("OEE", null, null, p1, null),       // value null
                new ExternalProductionKpi("OEE", BigDecimal.ONE, null, null, null), // period null
                new ExternalProductionKpi(null, BigDecimal.ONE, null, p1, null)));  // code null
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ErpDto.SyncReport r = service.sync(CONN);

        assertThat(r.kpisImported()).isZero();
        assertThat(r.kpisIgnored()).isEqualTo(3);
        verify(kpiDefinitionRepo, never()).findByTenantIdAndCode(any(), any());
    }

    @Test
    void mapSupplierType_coversAllBranches() {
        assertThat(ErpConnectorService.mapSupplierType(null)).isEqualTo(SupplierType.OTHER);
        assertThat(ErpConnectorService.mapSupplierType("raw material")).isEqualTo(SupplierType.RAW_MATERIAL);
        assertThat(ErpConnectorService.mapSupplierType("Composant")).isEqualTo(SupplierType.COMPONENT);
        assertThat(ErpConnectorService.mapSupplierType("Prestation")).isEqualTo(SupplierType.SERVICE);
        assertThat(ErpConnectorService.mapSupplierType("Contract Manuf")).isEqualTo(SupplierType.CONTRACT_MANUFACTURER);
        assertThat(ErpConnectorService.mapSupplierType("SaaS license")).isEqualTo(SupplierType.SOFTWARE);
        assertThat(ErpConnectorService.mapSupplierType("Logistique")).isEqualTo(SupplierType.LOGISTICS);
        assertThat(ErpConnectorService.mapSupplierType("zzz")).isEqualTo(SupplierType.OTHER);
    }

    private ErpConnection connection() {
        ErpConnection c = new ErpConnection();
        c.setId(CONN);
        c.setTenantId(TENANT);
        c.setProvider(ErpProvider.SAP);
        c.setBaseUrl("https://sap.example.com");
        c.setUsername("RFC_USER");
        c.setCredentialCipher("CIPHER");
        c.setStatus(ErpConnectionStatus.ACTIVE);
        c.setCreatedBy(USER);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return c;
    }
}
