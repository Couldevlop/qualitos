package com.openlab.qualitos.quality.itsm;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
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
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItsmConnectorServiceTest {

    @Mock ItsmConnectionRepository connectionRepo;
    @Mock ItsmIncidentMappingRepository mappingRepo;
    @Mock SecretCipher cipher;
    @Mock ItsmProviderClient serviceNow;
    @Mock ItsmProviderClient jira;

    ItsmConnectorService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID CONN = UUID.randomUUID();

    @BeforeEach
    void setup() {
        when(serviceNow.provider()).thenReturn(ItsmProvider.SERVICENOW);
        when(jira.provider()).thenReturn(ItsmProvider.JIRA_SM);
        service = new ItsmConnectorService(connectionRepo, mappingRepo, cipher,
                List.of(serviceNow, jira));
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void createConnection_persistsWithEncryptedSecret() {
        when(cipher.encrypt("supersecret")).thenReturn("CIPHER==");
        when(connectionRepo.save(any())).thenAnswer(inv -> {
            ItsmConnection c = inv.getArgument(0);
            c.setId(CONN);
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            return c;
        });
        ItsmDto.CreateConnectionRequest req = new ItsmDto.CreateConnectionRequest(
                "Prod ServiceNow", ItsmProvider.SERVICENOW,
                "https://x.service-now.com", "admin", "supersecret", null, USER);

        ItsmDto.ConnectionResponse out = service.createConnection(req);

        ArgumentCaptor<ItsmConnection> cap = ArgumentCaptor.forClass(ItsmConnection.class);
        verify(connectionRepo).save(cap.capture());
        assertThat(cap.getValue().getCredentialCipher()).isEqualTo("CIPHER==");
        assertThat(cap.getValue().getTenantId()).isEqualTo(TENANT);
        assertThat(out.tenantId()).isEqualTo(TENANT);
        assertThat(out.status()).isEqualTo(ConnectionStatus.ACTIVE);
    }

    @Test
    void createConnection_withoutTenant_throws() {
        TenantContext.clear();
        ItsmDto.CreateConnectionRequest req = new ItsmDto.CreateConnectionRequest(
                "n", ItsmProvider.JIRA_SM, "https://x", "u", "supersecret", null, USER);
        assertThatThrownBy(() -> service.createConnection(req))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void getConnection_otherTenant_appearsNotFound() {
        ItsmConnection c = connection(); c.setTenantId(UUID.randomUUID());
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.getConnection(CONN))
                .isInstanceOf(ItsmConnectionNotFoundException.class);
    }

    @Test
    void getConnection_missing_throwsNotFound() {
        when(connectionRepo.findById(CONN)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getConnection(CONN))
                .isInstanceOf(ItsmConnectionNotFoundException.class);
    }

    @Test
    void updateConnection_changesFieldsAndReEncryptsSecret() {
        ItsmConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.encrypt("new-secret")).thenReturn("NEW-CIPHER");
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateConnection(CONN, new ItsmDto.UpdateConnectionRequest(
                "renamed", "https://new.example.com", "user2", "new-secret", "PRJ",
                ConnectionStatus.DISABLED));

        assertThat(c.getName()).isEqualTo("renamed");
        assertThat(c.getBaseUrl()).isEqualTo("https://new.example.com");
        assertThat(c.getUsername()).isEqualTo("user2");
        assertThat(c.getCredentialCipher()).isEqualTo("NEW-CIPHER");
        assertThat(c.getExternalScope()).isEqualTo("PRJ");
        assertThat(c.getStatus()).isEqualTo(ConnectionStatus.DISABLED);
    }

    @Test
    void updateConnection_reactivatingResetsFailures() {
        ItsmConnection c = connection();
        c.setStatus(ConnectionStatus.DISABLED_ON_ERRORS);
        c.setConsecutiveFailures(15);
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateConnection(CONN, new ItsmDto.UpdateConnectionRequest(
                null, null, null, null, null, ConnectionStatus.ACTIVE));

        assertThat(c.getStatus()).isEqualTo(ConnectionStatus.ACTIVE);
        assertThat(c.getConsecutiveFailures()).isZero();
    }

    @Test
    void deleteConnection_removesEntity() {
        ItsmConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        service.deleteConnection(CONN);
        verify(connectionRepo).delete(c);
    }

    @Test
    void listConnections_paginated() {
        when(connectionRepo.findByTenantId(eq(TENANT), any()))
                .thenReturn(new PageImpl<>(List.of(connection())));
        Page<ItsmDto.ConnectionResponse> out = service.listConnections(PageRequest.of(0, 10));
        assertThat(out.getTotalElements()).isOne();
    }

    @Test
    void syncConnection_disabled_returnsEarlyReport() {
        ItsmConnection c = connection(); c.setStatus(ConnectionStatus.DISABLED);
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ItsmDto.SyncReport r = service.syncConnection(CONN);
        assertThat(r.totalFetched()).isZero();
        assertThat(r.errorMessage()).contains("DISABLED");
        // pas d'appel fetchIncidents quand la connexion n'est pas ACTIVE
        verify(serviceNow, never()).fetchIncidents(any(), any(), any());
        verify(jira, never()).fetchIncidents(any(), any(), any());
    }

    @Test
    void syncConnection_decryptionFails_recordsFailure() {
        ItsmConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.decrypt(any())).thenThrow(new RuntimeException("kaboom"));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ItsmDto.SyncReport r = service.syncConnection(CONN);

        assertThat(r.errorMessage()).contains("decryption");
        assertThat(c.getConsecutiveFailures()).isOne();
    }

    @Test
    void syncConnection_providerThrows_recordsFailure() {
        ItsmConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.decrypt(any())).thenReturn("plain");
        when(serviceNow.fetchIncidents(any(), any(), any()))
                .thenThrow(new ItsmSyncException("boom"));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ItsmDto.SyncReport r = service.syncConnection(CONN);

        assertThat(r.errorMessage()).isEqualTo("boom");
        assertThat(c.getConsecutiveFailures()).isOne();
        assertThat(c.getStatus()).isEqualTo(ConnectionStatus.ACTIVE); // < 10 failures
    }

    @Test
    void syncConnection_autoDisablesAfterMaxFailures() {
        ItsmConnection c = connection();
        c.setConsecutiveFailures(ItsmConnectorService.MAX_CONSECUTIVE_FAILURES_BEFORE_DISABLE - 1);
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.decrypt(any())).thenReturn("plain");
        when(serviceNow.fetchIncidents(any(), any(), any()))
                .thenThrow(new ItsmSyncException("still broken"));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.syncConnection(CONN);

        assertThat(c.getStatus()).isEqualTo(ConnectionStatus.DISABLED_ON_ERRORS);
        assertThat(c.getConsecutiveFailures())
                .isEqualTo(ItsmConnectorService.MAX_CONSECUTIVE_FAILURES_BEFORE_DISABLE);
    }

    @Test
    void syncConnection_success_createsAndUpdatesMappings() {
        ItsmConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.decrypt(any())).thenReturn("plain");
        ExternalIncident inc1 = new ExternalIncident("abc", "T1", "d", "Open", "1",
                Instant.now(), Instant.now(), "http://u/abc");
        ExternalIncident inc2 = new ExternalIncident("def", "T2", "d", "Closed", "2",
                Instant.now(), Instant.now(), "http://u/def");
        when(serviceNow.fetchIncidents(any(), any(), any())).thenReturn(List.of(inc1, inc2));

        when(mappingRepo.findByConnectionIdAndExternalId(CONN, "abc"))
                .thenReturn(Optional.empty()); // new
        ItsmIncidentMapping existing = new ItsmIncidentMapping();
        existing.setId(UUID.randomUUID());
        existing.setExternalId("def");
        when(mappingRepo.findByConnectionIdAndExternalId(CONN, "def"))
                .thenReturn(Optional.of(existing));
        when(mappingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ItsmDto.SyncReport r = service.syncConnection(CONN);

        assertThat(r.totalFetched()).isEqualTo(2);
        assertThat(r.newImports()).isEqualTo(1);
        assertThat(r.alreadyKnown()).isEqualTo(1);
        assertThat(c.getConsecutiveFailures()).isZero();
        assertThat(c.getLastSuccessAt()).isNotNull();
    }

    @Test
    void syncConnection_skipsIncidentsWithBlankId() {
        ItsmConnection c = connection();
        when(connectionRepo.findById(CONN)).thenReturn(Optional.of(c));
        when(cipher.decrypt(any())).thenReturn("plain");
        when(serviceNow.fetchIncidents(any(), any(), any())).thenReturn(List.of(
                new ExternalIncident("", "t", "d", "s", "p", null, null, null),
                new ExternalIncident(null, "t", "d", "s", "p", null, null, null),
                new ExternalIncident("real", "t", "d", "s", "p", null, null, null)
        ));
        when(mappingRepo.findByConnectionIdAndExternalId(any(), any())).thenReturn(Optional.empty());
        when(mappingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(connectionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ItsmDto.SyncReport r = service.syncConnection(CONN);
        assertThat(r.newImports()).isEqualTo(1);
    }

    @Test
    void listMappings_byConnection_filtered() {
        when(mappingRepo.findByTenantIdAndConnectionId(eq(TENANT), eq(CONN), any()))
                .thenReturn(new PageImpl<>(List.of(new ItsmIncidentMapping())));
        Page<ItsmDto.MappingResponse> out = service.listMappings(CONN, PageRequest.of(0, 10));
        assertThat(out.getTotalElements()).isOne();
    }

    @Test
    void listMappings_all_byTenant() {
        when(mappingRepo.findByTenantId(eq(TENANT), any()))
                .thenReturn(new PageImpl<>(List.of(new ItsmIncidentMapping())));
        Page<ItsmDto.MappingResponse> out = service.listMappings(null, PageRequest.of(0, 10));
        assertThat(out.getTotalElements()).isOne();
    }

    @Test
    void clientsView_returnsRegisteredProviders() {
        assertThat(service.clientsView()).containsKeys(ItsmProvider.SERVICENOW, ItsmProvider.JIRA_SM);
    }

    private ItsmConnection connection() {
        ItsmConnection c = new ItsmConnection();
        c.setId(CONN);
        c.setTenantId(TENANT);
        c.setProvider(ItsmProvider.SERVICENOW);
        c.setBaseUrl("https://x.service-now.com");
        c.setUsername("admin");
        c.setCredentialCipher("CIPHER");
        c.setStatus(ConnectionStatus.ACTIVE);
        c.setCreatedBy(USER);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return c;
    }

    private static org.mockito.ArgumentMatcher<Object> eq2(Object x) {
        return a -> a != null && a.equals(x);
    }

    private static <T> T eq(T value) { return org.mockito.ArgumentMatchers.eq(value); }
}
