package com.openlab.qualitos.quality.ehrconnector;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.itsm.ConnectionStatus;
import com.openlab.qualitos.quality.itsm.SecretCipher;
import com.openlab.qualitos.quality.nonconformity.NcCategory;
import com.openlab.qualitos.quality.nonconformity.NcDto;
import com.openlab.qualitos.quality.nonconformity.NcService;
import com.openlab.qualitos.quality.nonconformity.NcSeverity;
import com.openlab.qualitos.quality.nonconformity.NcStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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
class EhrConnectorServiceTest {

    @Mock EhrConnectionRepository connectionRepo;
    @Mock EhrImportedResourceRepository importedRepo;
    @Mock SecretCipher cipher;
    @Mock FhirClient fhirClient;
    @Mock NcService ncService;

    EhrConnectorService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID CONN = UUID.randomUUID();

    @BeforeEach
    void setup() {
        service = new EhrConnectorService(connectionRepo, importedRepo, cipher, fhirClient, ncService);
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    // ---------- CRUD + chiffrement ----------

    @Test
    void createConnection_encryptsSecret_andSetsTenantFromContext() {
        when(cipher.encrypt("topsecret")).thenReturn("CIPHER==");
        when(connectionRepo.save(any())).thenAnswer(inv -> {
            EhrConnection c = inv.getArgument(0);
            c.setId(CONN);
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            return c;
        });
        EhrDto.CreateConnectionRequest req = new EhrDto.CreateConnectionRequest(
                "Prod FHIR", EhrProvider.FHIR_R5, "https://fhir.example.org/r5",
                EhrAuthMode.BEARER, null, "topsecret", "patient-safety", USER);

        EhrDto.ConnectionResponse out = service.createConnection(req);

        ArgumentCaptor<EhrConnection> cap = ArgumentCaptor.forClass(EhrConnection.class);
        verify(connectionRepo).save(cap.capture());
        assertThat(cap.getValue().getCredentialCipher()).isEqualTo("CIPHER==");
        assertThat(cap.getValue().getTenantId()).isEqualTo(TENANT);
        assertThat(out.tenantId()).isEqualTo(TENANT);
        assertThat(out.status()).isEqualTo(ConnectionStatus.ACTIVE);
        assertThat(out.authMode()).isEqualTo(EhrAuthMode.BEARER);
    }

    @Test
    void createConnection_withoutTenant_throws() {
        TenantContext.clear();
        EhrDto.CreateConnectionRequest req = new EhrDto.CreateConnectionRequest(
                "n", EhrProvider.FHIR_R4, "https://x", EhrAuthMode.BASIC, "u", "secret", null, USER);
        assertThatThrownBy(() -> service.createConnection(req))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void getConnection_otherTenant_appearsNotFound() {
        EhrConnection c = connection();
        c.setTenantId(UUID.randomUUID());
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.getConnection(CONN))
                .isInstanceOf(EhrConnectionNotFoundException.class);
    }

    @Test
    void getConnection_missing_throws() {
        when(connectionRepo.findById(CONN)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getConnection(CONN))
                .isInstanceOf(EhrConnectionNotFoundException.class);
    }

    @Test
    void updateConnection_reEncryptsSecret_andResetsFailuresOnReactivation() {
        EhrConnection c = connection();
        c.setStatus(ConnectionStatus.DISABLED_ON_ERRORS);
        c.setConsecutiveFailures(9);
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.encrypt("new")).thenReturn("NEW-CIPHER");
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateConnection(CONN, new EhrDto.UpdateConnectionRequest(
                "renamed", "https://new.example.org", EhrAuthMode.BASIC, "u2", "new",
                "lab-critical", ConnectionStatus.ACTIVE));

        assertThat(c.getName()).isEqualTo("renamed");
        assertThat(c.getFhirBaseUrl()).isEqualTo("https://new.example.org");
        assertThat(c.getAuthMode()).isEqualTo(EhrAuthMode.BASIC);
        assertThat(c.getCredentialCipher()).isEqualTo("NEW-CIPHER");
        assertThat(c.getResourceCategory()).isEqualTo("lab-critical");
        assertThat(c.getStatus()).isEqualTo(ConnectionStatus.ACTIVE);
        assertThat(c.getConsecutiveFailures()).isZero();
    }

    @Test
    void deleteConnection_removesEntity() {
        EhrConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        service.deleteConnection(CONN);
        verify(connectionRepo).delete(c);
    }

    @Test
    void listConnections_paginated() {
        when(connectionRepo.findByTenantId(eq(TENANT), any()))
                .thenReturn(new PageImpl<>(List.of(connection())));
        Page<EhrDto.ConnectionResponse> out = service.listConnections(PageRequest.of(0, 10));
        assertThat(out.getTotalElements()).isOne();
    }

    // ---------- sync : NC creation + privacy ----------

    @Test
    void sync_abnormalObservation_createsNc_withoutPii() {
        EhrConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.decrypt(any())).thenReturn("plain");
        FhirResource abnormal = new FhirResource("Observation", "obs-1", "8867-4", "Heart rate",
                "final", "HH", Instant.parse("2026-05-10T08:00:00Z"));
        when(fhirClient.fetchAdverseResources(any(), eq("plain"), any())).thenReturn(List.of(abnormal));
        when(importedRepo.existsByConnectionIdAndFhirResourceId(CONN, "obs-1")).thenReturn(false);
        UUID ncId = UUID.randomUUID();
        when(ncService.create(any())).thenReturn(ncResponse(ncId));
        when(importedRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EhrDto.SyncReport report = service.sync(CONN);

        assertThat(report.totalFetched()).isEqualTo(1);
        assertThat(report.created()).isEqualTo(1);
        assertThat(report.skipped()).isZero();
        assertThat(report.errorMessage()).isNull();

        // La NC est SAFETY, sévérité critique (HH), et SANS donnée patient.
        ArgumentCaptor<NcDto.CreateRequest> ncCap = ArgumentCaptor.forClass(NcDto.CreateRequest.class);
        verify(ncService).create(ncCap.capture());
        NcDto.CreateRequest created = ncCap.getValue();
        assertThat(created.category()).isEqualTo(NcCategory.SAFETY);
        assertThat(created.severity()).isEqualTo(NcSeverity.CRITICAL);
        assertThat(created.reporterId()).isNull();
        assertThat(created.title()).contains("Heart rate");
        assertThat(created.description()).contains("Observation/obs-1");
        // PRIVACY : pas d'identifiant patient nominatif.
        assertThat(created.description()).doesNotContainIgnoringCase("patient/");
        assertThat(created.description().toLowerCase()).contains("no patient-identifiable data");

        // L'idempotence est tracée avec l'id technique uniquement.
        ArgumentCaptor<EhrImportedResource> impCap = ArgumentCaptor.forClass(EhrImportedResource.class);
        verify(importedRepo).save(impCap.capture());
        assertThat(impCap.getValue().getFhirResourceId()).isEqualTo("obs-1");
        assertThat(impCap.getValue().getNcId()).isEqualTo(ncId);
        assertThat(impCap.getValue().getTenantId()).isEqualTo(TENANT);
    }

    @Test
    void sync_secondRun_doesNotRecreateNc_idempotent() {
        EhrConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.decrypt(any())).thenReturn("plain");
        FhirResource abnormal = new FhirResource("Observation", "obs-1", "x", "X",
                "final", "H", Instant.now());
        when(fhirClient.fetchAdverseResources(any(), any(), any())).thenReturn(List.of(abnormal));
        // déjà importée
        when(importedRepo.existsByConnectionIdAndFhirResourceId(CONN, "obs-1")).thenReturn(true);
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EhrDto.SyncReport report = service.sync(CONN);

        assertThat(report.totalFetched()).isEqualTo(1);
        assertThat(report.created()).isZero();
        assertThat(report.skipped()).isEqualTo(1);
        verify(ncService, never()).create(any());
        verify(importedRepo, never()).save(any());
    }

    @Test
    void sync_networkFailure_returnsErrorReport_andCountsFailure() {
        EhrConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.decrypt(any())).thenReturn("plain");
        when(fhirClient.fetchAdverseResources(any(), any(), any()))
                .thenThrow(new EhrSyncException("FHIR Observation fetch failed: connection refused"));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EhrDto.SyncReport report = service.sync(CONN);

        assertThat(report.errors()).isEqualTo(1);
        assertThat(report.created()).isZero();
        assertThat(report.errorMessage()).contains("fetch failed");
        assertThat(c.getConsecutiveFailures()).isOne();
        assertThat(c.getStatus()).isEqualTo(ConnectionStatus.ACTIVE);
        verify(ncService, never()).create(any());
    }

    @Test
    void sync_decryptionFails_returnsErrorReport() {
        EhrConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.decrypt(any())).thenThrow(new IllegalStateException("bad key"));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EhrDto.SyncReport report = service.sync(CONN);

        assertThat(report.errorMessage()).contains("decryption");
        assertThat(c.getConsecutiveFailures()).isOne();
        verify(fhirClient, never()).fetchAdverseResources(any(), any(), any());
    }

    @Test
    void sync_notActive_returnsEarlyReport() {
        EhrConnection c = connection();
        c.setStatus(ConnectionStatus.DISABLED);
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EhrDto.SyncReport report = service.sync(CONN);

        assertThat(report.errorMessage()).contains("DISABLED");
        verify(fhirClient, never()).fetchAdverseResources(any(), any(), any());
    }

    @Test
    void sync_autoDisablesAfterMaxFailures() {
        EhrConnection c = connection();
        c.setConsecutiveFailures(EhrConnectorService.MAX_CONSECUTIVE_FAILURES_BEFORE_DISABLE - 1);
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.decrypt(any())).thenReturn("plain");
        when(fhirClient.fetchAdverseResources(any(), any(), any()))
                .thenThrow(new EhrSyncException("still broken"));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.sync(CONN);

        assertThat(c.getStatus()).isEqualTo(ConnectionStatus.DISABLED_ON_ERRORS);
    }

    @Test
    void sync_ncCreationThrows_countsErrorButContinues() {
        EhrConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.decrypt(any())).thenReturn("plain");
        FhirResource r1 = new FhirResource("Observation", "obs-1", "x", "X", "final", "A", Instant.now());
        FhirResource r2 = new FhirResource("Observation", "obs-2", "y", "Y", "final", "A", Instant.now());
        when(fhirClient.fetchAdverseResources(any(), any(), any())).thenReturn(List.of(r1, r2));
        when(importedRepo.existsByConnectionIdAndFhirResourceId(any(), any())).thenReturn(false);
        when(ncService.create(any()))
                .thenThrow(new RuntimeException("nc boom"))
                .thenReturn(ncResponse(UUID.randomUUID()));
        when(importedRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EhrDto.SyncReport report = service.sync(CONN);

        assertThat(report.created()).isEqualTo(1);
        assertThat(report.errors()).isEqualTo(1);
        assertThat(report.errorMessage()).isNull(); // batch a abouti malgré 1 échec unitaire
    }

    @Test
    void mapSeverity_interpretationMapping() {
        assertThat(EhrConnectorService.mapSeverity("HH")).isEqualTo(NcSeverity.CRITICAL);
        assertThat(EhrConnectorService.mapSeverity("AA")).isEqualTo(NcSeverity.CRITICAL);
        assertThat(EhrConnectorService.mapSeverity("LL")).isEqualTo(NcSeverity.CRITICAL);
        assertThat(EhrConnectorService.mapSeverity("A")).isEqualTo(NcSeverity.MAJOR);
        assertThat(EhrConnectorService.mapSeverity("H")).isEqualTo(NcSeverity.MAJOR);
        assertThat(EhrConnectorService.mapSeverity("POS")).isEqualTo(NcSeverity.MAJOR);
        assertThat(EhrConnectorService.mapSeverity(null)).isEqualTo(NcSeverity.MINOR);
        assertThat(EhrConnectorService.mapSeverity("ZZZ")).isEqualTo(NcSeverity.MINOR);
    }

    private EhrConnection connection() {
        EhrConnection c = new EhrConnection();
        c.setId(CONN);
        c.setTenantId(TENANT);
        c.setName("fhir");
        c.setProvider(EhrProvider.FHIR_R5);
        c.setFhirBaseUrl("https://fhir.example.org/r5");
        c.setAuthMode(EhrAuthMode.BEARER);
        c.setCredentialCipher("CIPHER");
        c.setStatus(ConnectionStatus.ACTIVE);
        c.setCreatedBy(USER);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return c;
    }

    private NcDto.Response ncResponse(UUID id) {
        return new NcDto.Response(id, TENANT, "NC-2026-0001", "t", "d",
                NcCategory.SAFETY, NcSeverity.CRITICAL, NcStatus.OPEN, Instant.now(),
                null, null, null, null, null, null, null, null, null, null,
                Instant.now(), Instant.now());
    }
}
