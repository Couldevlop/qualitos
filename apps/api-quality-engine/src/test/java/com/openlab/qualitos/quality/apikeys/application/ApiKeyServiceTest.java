package com.openlab.qualitos.quality.apikeys.application;

import com.openlab.qualitos.quality.apikeys.domain.ApiKey;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeyHasher;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeyNotFoundException;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeyRepository;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeySecretGenerator;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeyStateException;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApiKeyServiceTest {

    @Mock ApiKeyRepository repo;
    @Mock ApiKeyHasher hasher;
    @Mock ApiKeySecretGenerator generator;
    @Mock TenantProvider tenantProvider;
    @Mock ApiKeyEventPublisher events;
    ApiKeyService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID ACTOR = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Instant FUTURE = NOW.plusSeconds(86400L * 30);
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        service = new ApiKeyService(repo, hasher, generator, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(generator.generate()).thenReturn(new ApiKeySecretGenerator.Material(
                "pfx12345", "raw-secret", "qos_pfx12345_raw-secret"));
        when(hasher.hash("raw-secret")).thenReturn("bcrypt$...");
        when(repo.save(any())).thenAnswer(inv -> {
            ApiKey k = inv.getArgument(0);
            if (k.getId() == null) k.assignId(ID);
            return k;
        });
    }

    // ---- create ----

    @Test
    void create_returnsPlaintextOnce_andHidesHash() {
        ApiKeyDto.IssuedKey out = service.create(new ApiKeyDto.CreateRequest(
                "ci-bot", Set.of("audit.read"), FUTURE, ACTOR));
        assertThat(out.plaintext()).isEqualTo("qos_pfx12345_raw-secret");
        assertThat(out.view().status()).isEqualTo(ApiKeyStatus.ACTIVE);
        assertThat(out.view().scopes()).containsExactly("audit.read");
        verify(events).publish(any(), eq(ApiKeyEventPublisher.Action.ISSUED));
    }

    @Test
    void create_emptyName_rejected() {
        assertThatThrownBy(() -> service.create(new ApiKeyDto.CreateRequest(
                "  ", Set.of(), null, ACTOR)))
                .isInstanceOf(ApiKeyStateException.class);
    }

    @Test
    void create_invalidScope_rejected() {
        assertThatThrownBy(() -> service.create(new ApiKeyDto.CreateRequest(
                "n", Set.of("BAD SCOPE"), null, ACTOR)))
                .isInstanceOf(ApiKeyStateException.class);
    }

    // ---- revoke / cross-tenant ----

    @Test
    void revoke_marksAsRevoked() {
        ApiKey k = sample(); k.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(k));
        ApiKeyDto.View v = service.revoke(ID, new ApiKeyDto.RevokeRequest(ACTOR));
        assertThat(v.status()).isEqualTo(ApiKeyStatus.REVOKED);
        verify(events).publish(any(), eq(ApiKeyEventPublisher.Action.REVOKED));
    }

    @Test
    void crossTenant_appearsNotFound() {
        ApiKey foreign = new ApiKey(ID, UUID.randomUUID(), "x", "p", "h",
                Set.of(), ApiKeyStatus.ACTIVE, NOW, ACTOR, null, null, null, null);
        when(repo.findById(ID)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(ApiKeyNotFoundException.class);
    }

    // ---- rotate ----

    @Test
    void rotate_revokesOld_issuesNew_withNewPrefix() {
        ApiKey k = sample(); k.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(k));
        when(generator.generate()).thenReturn(new ApiKeySecretGenerator.Material(
                "newpfx00", "new-secret", "qos_newpfx00_new-secret"));
        when(hasher.hash("new-secret")).thenReturn("bcrypt$new");

        ApiKeyDto.IssuedKey out = service.rotate(ID, new ApiKeyDto.RotateRequest(ACTOR));
        assertThat(out.plaintext()).isEqualTo("qos_newpfx00_new-secret");
        assertThat(out.view().prefix()).isEqualTo("newpfx00");
        verify(events).publish(any(), eq(ApiKeyEventPublisher.Action.REVOKED));
        verify(events).publish(any(), eq(ApiKeyEventPublisher.Action.ROTATED));
    }

    // ---- verify ----

    @Test
    void verify_validKey_returnsAndStampsLastUsed() {
        ApiKey k = sample(); k.assignId(ID);
        when(repo.findByPrefix("pfx12345")).thenReturn(Optional.of(k));
        when(hasher.matches("raw-secret", "bcrypt$...")).thenReturn(true);
        Optional<ApiKey> v = service.verify("qos_pfx12345_raw-secret");
        assertThat(v).isPresent();
        assertThat(v.get().getLastUsedAt()).isEqualTo(NOW);
        verify(events).publish(any(), eq(ApiKeyEventPublisher.Action.USED));
    }

    @Test
    void verify_wrongFormat_empty_noLookup() {
        assertThat(service.verify(null)).isEmpty();
        assertThat(service.verify("notqos_x_y")).isEmpty();
        assertThat(service.verify("qos__y")).isEmpty();
        assertThat(service.verify("qos_x_")).isEmpty();
        verify(repo, never()).findByPrefix(any());
    }

    @Test
    void verify_unknownPrefix_empty() {
        when(repo.findByPrefix("pfx12345")).thenReturn(Optional.empty());
        assertThat(service.verify("qos_pfx12345_raw-secret")).isEmpty();
        verify(hasher, never()).matches(any(), any());
    }

    @Test
    void verify_wrongSecret_empty() {
        ApiKey k = sample(); k.assignId(ID);
        when(repo.findByPrefix("pfx12345")).thenReturn(Optional.of(k));
        when(hasher.matches("raw-secret", "bcrypt$...")).thenReturn(false);
        assertThat(service.verify("qos_pfx12345_raw-secret")).isEmpty();
        verify(events, never()).publish(any(), any());
    }

    @Test
    void verify_revokedKey_empty() {
        ApiKey k = sample(); k.assignId(ID);
        k.revoke(ACTOR, NOW.minusSeconds(60));
        when(repo.findByPrefix("pfx12345")).thenReturn(Optional.of(k));
        assertThat(service.verify("qos_pfx12345_raw-secret")).isEmpty();
        verify(hasher, never()).matches(any(), any());
    }

    @Test
    void verify_expiredAtBoundary_recordsExpiredEvent() {
        ApiKey k = ApiKey.issued(TENANT, "x", "pfx12345", "bcrypt$...",
                Set.of(), NOW, ACTOR, NOW.minusSeconds(60));
        // ↑ trick : NOW = expiresAt → recordUsage l'expire
        // Mais issued() rejette si expiresAt ≤ NOW. Donc on construit manuellement.
        k = new ApiKey(ID, TENANT, "x", "pfx12345", "bcrypt$...",
                Set.of(), ApiKeyStatus.ACTIVE,
                NOW.minusSeconds(120), ACTOR, NOW, null, null, null);
        when(repo.findByPrefix("pfx12345")).thenReturn(Optional.of(k));
        when(hasher.matches("raw-secret", "bcrypt$...")).thenReturn(true);
        assertThat(service.verify("qos_pfx12345_raw-secret")).isEmpty();
        verify(events).publish(any(), eq(ApiKeyEventPublisher.Action.EXPIRED));
    }

    // ---- expireDue ----

    @Test
    void expireDue_processesAndPublishes() {
        ApiKey k = new ApiKey(ID, TENANT, "x", "pfx12345", "h",
                Set.of(), ApiKeyStatus.ACTIVE,
                NOW.minusSeconds(120), ACTOR, NOW.minusSeconds(1), null, null, null);
        when(repo.findExpirable(eq(NOW), eq(200))).thenReturn(List.of(k));
        assertThat(service.expireDue(200)).isEqualTo(1);
        verify(events).publish(any(), eq(ApiKeyEventPublisher.Action.EXPIRED));
    }

    @Test
    void expireDue_clampsLimit() {
        when(repo.findExpirable(any(), eq(500))).thenReturn(List.of());
        service.expireDue(9999);
        verify(repo).findExpirable(eq(NOW), eq(500));
        service.expireDue(0);
        verify(repo).findExpirable(eq(NOW), eq(1));
    }

    // ---- list / get ----

    @Test
    void list_returnsAllForTenant() {
        when(repo.findAllByTenantId(TENANT)).thenReturn(List.of(sample(), sample()));
        assertThat(service.list()).hasSize(2);
    }

    private ApiKey sample() {
        return ApiKey.issued(TENANT, "ci-bot", "pfx12345", "bcrypt$...",
                Set.of("audit.read"), FUTURE, ACTOR, NOW);
    }
}
