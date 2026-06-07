package com.openlab.qualitos.quality.commconnector;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.itsm.ConnectionStatus;
import com.openlab.qualitos.quality.itsm.SecretCipher;
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
class CommConnectorServiceTest {

    @Mock CommConnectionRepository repo;
    @Mock SecretCipher cipher;
    @Mock CommProviderClient teams;
    @Mock CommProviderClient slack;

    CommConnectorService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID CONN = UUID.randomUUID();

    @BeforeEach
    void setup() {
        lenient().when(teams.provider()).thenReturn(CommProvider.TEAMS);
        lenient().when(slack.provider()).thenReturn(CommProvider.SLACK);
        service = new CommConnectorService(repo, cipher, List.of(teams, slack));
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    // ---- CRUD + chiffrement ----

    @Test
    void createConnection_encryptsWebhookUrl() {
        when(cipher.encrypt("https://hooks.slack.com/xxx")).thenReturn("CIPHER==");
        when(repo.save(any())).thenAnswer(inv -> {
            CommConnection c = inv.getArgument(0);
            c.setId(CONN);
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            return c;
        });
        CommDto.CreateConnectionRequest req = new CommDto.CreateConnectionRequest(
                "Slack Qualité", CommProvider.SLACK, "https://hooks.slack.com/xxx", "#qualite", USER);

        CommDto.ConnectionResponse out = service.createConnection(req);

        ArgumentCaptor<CommConnection> cap = ArgumentCaptor.forClass(CommConnection.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getWebhookUrlCipher()).isEqualTo("CIPHER==");
        assertThat(cap.getValue().getTenantId()).isEqualTo(TENANT);
        assertThat(out.status()).isEqualTo(ConnectionStatus.ACTIVE);
        // la réponse n'expose jamais l'URL → record ConnectionResponse ne porte aucun champ secret
        assertThat(out.channel()).isEqualTo("#qualite");
    }

    @Test
    void createConnection_withoutTenant_throws() {
        TenantContext.clear();
        CommDto.CreateConnectionRequest req = new CommDto.CreateConnectionRequest(
                "n", CommProvider.TEAMS, "https://x/y", null, USER);
        assertThatThrownBy(() -> service.createConnection(req))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void getConnection_missing_throwsNotFound() {
        when(repo.findByIdAndTenantId(CONN, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getConnection(CONN))
                .isInstanceOf(CommConnectionNotFoundException.class);
    }

    @Test
    void updateConnection_reEncryptsAndResetsFailuresOnReactivate() {
        CommConnection c = conn(CommProvider.SLACK);
        c.setStatus(ConnectionStatus.DISABLED_ON_ERRORS);
        c.setConsecutiveFailures(15);
        when(repo.findByIdAndTenantId(CONN, TENANT)).thenReturn(Optional.of(c));
        when(cipher.encrypt("https://new/url")).thenReturn("NEW");
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateConnection(CONN, new CommDto.UpdateConnectionRequest(
                "renamed", "https://new/url", "#other", ConnectionStatus.ACTIVE));

        assertThat(c.getName()).isEqualTo("renamed");
        assertThat(c.getWebhookUrlCipher()).isEqualTo("NEW");
        assertThat(c.getChannel()).isEqualTo("#other");
        assertThat(c.getStatus()).isEqualTo(ConnectionStatus.ACTIVE);
        assertThat(c.getConsecutiveFailures()).isZero();
    }

    @Test
    void deleteConnection_removesEntity() {
        CommConnection c = conn(CommProvider.SLACK);
        when(repo.findByIdAndTenantId(CONN, TENANT)).thenReturn(Optional.of(c));
        service.deleteConnection(CONN);
        verify(repo).delete(c);
    }

    @Test
    void listConnections_paginated() {
        when(repo.findByTenantId(eq(TENANT), any())).thenReturn(new PageImpl<>(List.of(conn(CommProvider.TEAMS))));
        Page<CommDto.ConnectionResponse> out = service.listConnections(PageRequest.of(0, 10));
        assertThat(out.getTotalElements()).isOne();
    }

    // ---- test() ----

    @Test
    void test_success_marksSuccessAndReturnsOk() {
        CommConnection c = conn(CommProvider.SLACK);
        when(repo.findByIdAndTenantId(CONN, TENANT)).thenReturn(Optional.of(c));
        when(cipher.decrypt("CIPHER")).thenReturn("https://hooks/x");
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CommDto.TestResult r = service.test(CONN);

        assertThat(r.success()).isTrue();
        assertThat(r.errorMessage()).isNull();
        verify(slack).send(eq(c), eq("https://hooks/x"), any(CommMessage.class));
        assertThat(c.getLastSuccessAt()).isNotNull();
        assertThat(c.getConsecutiveFailures()).isZero();
    }

    @Test
    void test_sendFails_recordsFailureAndReturnsError() {
        CommConnection c = conn(CommProvider.SLACK);
        when(repo.findByIdAndTenantId(CONN, TENANT)).thenReturn(Optional.of(c));
        when(cipher.decrypt(any())).thenReturn("https://hooks/x");
        doThrow(new CommSendException("HTTP 500")).when(slack).send(any(), any(), any());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CommDto.TestResult r = service.test(CONN);

        assertThat(r.success()).isFalse();
        assertThat(r.errorMessage()).contains("500");
        assertThat(c.getConsecutiveFailures()).isOne();
    }

    @Test
    void test_noClientForProvider_returnsError() {
        // service construit sans le client TEAMS → provider non enregistré
        CommConnectorService svc = new CommConnectorService(repo, cipher, List.of(slack));
        CommConnection c = conn(CommProvider.TEAMS);
        when(repo.findByIdAndTenantId(CONN, TENANT)).thenReturn(Optional.of(c));

        CommDto.TestResult r = svc.test(CONN);

        assertThat(r.success()).isFalse();
        assertThat(r.errorMessage()).contains("No client");
    }

    // ---- notify() ----

    @Test
    void notify_noActiveConnection_isNoOp() {
        when(repo.findByTenantIdAndStatus(TENANT, ConnectionStatus.ACTIVE)).thenReturn(List.of());
        int sent = service.notify(TENANT, ncEvent());
        assertThat(sent).isZero();
        verify(slack, never()).send(any(), any(), any());
        verify(teams, never()).send(any(), any(), any());
    }

    @Test
    void notify_nullArgs_returnsZero() {
        assertThat(service.notify(null, ncEvent())).isZero();
        assertThat(service.notify(TENANT, null)).isZero();
    }

    @Test
    void notify_sendsToAllActiveConnections() {
        CommConnection a = conn(CommProvider.SLACK);
        CommConnection b = conn(CommProvider.TEAMS);
        when(repo.findByTenantIdAndStatus(TENANT, ConnectionStatus.ACTIVE)).thenReturn(List.of(a, b));
        when(cipher.decrypt(any())).thenReturn("https://hooks/x");
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int sent = service.notify(TENANT, ncEvent());

        assertThat(sent).isEqualTo(2);
        verify(slack).send(eq(a), any(), any());
        verify(teams).send(eq(b), any(), any());
    }

    @Test
    void notify_oneFails_othersStillSentAndFailureCounted() {
        CommConnection a = conn(CommProvider.SLACK);
        CommConnection b = conn(CommProvider.TEAMS);
        when(repo.findByTenantIdAndStatus(TENANT, ConnectionStatus.ACTIVE)).thenReturn(List.of(a, b));
        when(cipher.decrypt(any())).thenReturn("https://hooks/x");
        doThrow(new CommSendException("boom")).when(slack).send(any(), any(), any());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int sent = service.notify(TENANT, ncEvent());

        assertThat(sent).isOne(); // only teams succeeded
        assertThat(a.getConsecutiveFailures()).isOne();
        assertThat(b.getConsecutiveFailures()).isZero();
    }

    @Test
    void notify_autoDisablesAfterMaxFailures() {
        CommConnection a = conn(CommProvider.SLACK);
        a.setConsecutiveFailures(CommConnectorService.MAX_CONSECUTIVE_FAILURES_BEFORE_DISABLE - 1);
        when(repo.findByTenantIdAndStatus(TENANT, ConnectionStatus.ACTIVE)).thenReturn(List.of(a));
        when(cipher.decrypt(any())).thenReturn("https://hooks/x");
        doThrow(new CommSendException("still broken")).when(slack).send(any(), any(), any());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.notify(TENANT, ncEvent());

        assertThat(a.getStatus()).isEqualTo(ConnectionStatus.DISABLED_ON_ERRORS);
    }

    @Test
    void notify_decryptionFails_countedAsFailure() {
        CommConnection a = conn(CommProvider.SLACK);
        when(repo.findByTenantIdAndStatus(TENANT, ConnectionStatus.ACTIVE)).thenReturn(List.of(a));
        when(cipher.decrypt(any())).thenThrow(new RuntimeException("bad key"));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int sent = service.notify(TENANT, ncEvent());

        assertThat(sent).isZero();
        assertThat(a.getConsecutiveFailures()).isOne();
        verify(slack, never()).send(any(), any(), any()); // never reached send()
    }

    // ---- toMessage mapping ----

    @Test
    void toMessage_buildsDeepLinkAndFactsFromEvent() {
        CommEvent event = new CommEvent(CommEvent.Kind.NC_DETECTED, null, "Défaut X",
                "NON_CONFORMITY", "42", null);
        CommMessage msg = service.toMessage(event);

        assertThat(msg.title()).isEqualTo("Non-conformité détectée"); // default title from Kind
        assertThat(msg.severity()).isEqualTo(CommSeverity.WARNING);   // default severity from Kind
        assertThat(msg.text()).isEqualTo("Défaut X");
        assertThat(msg.linkUrl()).isEqualTo("/app/non-conformity/42");
        assertThat(msg.facts()).anySatisfy(f -> {
            assertThat(f.getKey()).isEqualTo("Ressource");
            assertThat(f.getValue()).isEqualTo("NON_CONFORMITY");
        });
    }

    @Test
    void toMessage_noResource_hasNoLink() {
        CommEvent event = new CommEvent(CommEvent.Kind.KPI_THRESHOLD_BREACHED, "Seuil franchi",
                "COQ > 3.2%", null, null, CommSeverity.CRITICAL);
        CommMessage msg = service.toMessage(event);
        assertThat(msg.linkUrl()).isNull();
        assertThat(msg.severity()).isEqualTo(CommSeverity.CRITICAL);
    }

    @Test
    void clientsView_returnsRegisteredProviders() {
        assertThat(service.clientsView()).containsKeys(CommProvider.TEAMS, CommProvider.SLACK);
    }

    // ---- helpers ----

    private CommConnection conn(CommProvider p) {
        CommConnection c = new CommConnection();
        c.setId(CONN);
        c.setTenantId(TENANT);
        c.setProvider(p);
        c.setName("conn-" + p);
        c.setWebhookUrlCipher("CIPHER");
        c.setStatus(ConnectionStatus.ACTIVE);
        c.setCreatedBy(USER);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return c;
    }

    private CommEvent ncEvent() {
        return new CommEvent(CommEvent.Kind.NC_DETECTED, null, "Défaut",
                "NON_CONFORMITY", UUID.randomUUID().toString(), null);
    }
}
