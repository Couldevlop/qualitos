package com.openlab.qualitos.quality.consent.application;

import com.openlab.qualitos.quality.consent.domain.Consent;
import com.openlab.qualitos.quality.consent.domain.ConsentNotFoundException;
import com.openlab.qualitos.quality.consent.domain.ConsentRepository;
import com.openlab.qualitos.quality.consent.domain.ConsentSource;
import com.openlab.qualitos.quality.consent.domain.ConsentStateException;
import com.openlab.qualitos.quality.consent.domain.ConsentStatus;
import com.openlab.qualitos.quality.consent.domain.SubjectIdentifierHasher;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConsentServiceTest {

    @Mock ConsentRepository repo;
    @Mock SubjectIdentifierHasher hasher;
    @Mock TenantProvider tenantProvider;
    @Mock ConsentEventPublisher events;

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final String HASH = "a".repeat(64);

    ConsentService service;

    @BeforeEach
    void setup() {
        service = new ConsentService(repo, hasher, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(hasher.hash(anyString())).thenReturn(HASH);
        when(repo.save(any())).thenAnswer(inv -> {
            Consent c = inv.getArgument(0);
            c.assignId(ID);
            return c;
        });
    }

    @Test
    void grant_normalizesIdentifier_hashes_andPersists() {
        ConsentDto.View v = service.grant(req("Jane@Example.COM"));
        verify(hasher).hash("jane@example.com");
        verify(repo).save(any());
        verify(events).publish(any(), eq(ConsentEventPublisher.Action.GRANTED));
        assertThat(v.subjectIdentifierHash()).isEqualTo(HASH);
        assertThat(v.status()).isEqualTo(ConsentStatus.GRANTED);
        assertThat(v.active()).isTrue();
    }

    @Test
    void grant_blankIdentifier_rejected() {
        assertThatThrownBy(() -> service.grant(req("  ")))
                .isInstanceOf(ConsentStateException.class);
    }

    @Test
    void withdraw_movesToWithdrawn_andPublishes() {
        Consent c = stored();
        when(repo.findById(ID)).thenReturn(Optional.of(c));
        ConsentDto.View v = service.withdraw(ID,
                new ConsentDto.WithdrawRequest(USER, "user request"));
        assertThat(v.status()).isEqualTo(ConsentStatus.WITHDRAWN);
        assertThat(v.active()).isFalse();
        verify(events).publish(any(), eq(ConsentEventPublisher.Action.WITHDRAWN));
    }

    @Test
    void withdraw_alreadyWithdrawn_throws() {
        Consent c = stored();
        c.withdraw(USER, "x", NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.withdraw(ID,
                new ConsentDto.WithdrawRequest(USER, "y")))
                .isInstanceOf(ConsentStateException.class);
    }

    @Test
    void get_missing_404() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(ConsentNotFoundException.class);
    }

    @Test
    void get_crossTenant_404_noLeak() {
        Consent other = Consent.grant(UUID.randomUUID(), HASH, null,
                "marketing", "v1", ConsentSource.WEB_FORM, null, null, null,
                USER, NOW, null);
        other.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(ConsentNotFoundException.class);
    }

    @Test
    void findBySubject_blank_returnsEmpty_noLookup() {
        assertThat(service.findBySubject("  ")).isEmpty();
        verify(repo, never()).findByTenantAndSubjectHash(any(), any());
    }

    @Test
    void findBySubject_normalizesAndLookups() {
        when(repo.findByTenantAndSubjectHash(TENANT, HASH))
                .thenReturn(List.of(stored()));
        List<ConsentDto.View> out = service.findBySubject("JANE@example.COM");
        verify(hasher).hash("jane@example.com");
        assertThat(out).hasSize(1);
    }

    @Test
    void findActiveByPurpose_normalizesAndQueries() {
        when(repo.findLatestActiveByPurpose(TENANT, HASH, "marketing", NOW))
                .thenReturn(Optional.of(stored()));
        Optional<ConsentDto.View> v = service.findActiveByPurpose("Jane@e.com", "marketing");
        assertThat(v).isPresent();
        verify(hasher).hash("jane@e.com");
    }

    @Test
    void findActiveByPurpose_blankInputs_returnsEmpty() {
        assertThat(service.findActiveByPurpose(null, "marketing")).isEmpty();
        assertThat(service.findActiveByPurpose("x", " ")).isEmpty();
        verify(repo, never()).findLatestActiveByPurpose(any(), any(), any(), any());
    }

    @Test
    void listByPurpose_blank_returnsEmpty() {
        assertThat(service.listByPurpose(" ")).isEmpty();
        verify(repo, never()).findByTenantAndPurpose(any(), any());
    }

    @Test
    void listByPurpose_delegates() {
        when(repo.findByTenantAndPurpose(TENANT, "marketing"))
                .thenReturn(List.of(stored(), stored()));
        assertThat(service.listByPurpose("marketing")).hasSize(2);
    }

    @Test
    void expireDue_movesGrantedPastExpiryToExpired_andPublishes() {
        Consent due = Consent.grant(TENANT, HASH, null, "marketing", "v1",
                ConsentSource.WEB_FORM, null, null, null, USER, NOW.minusSeconds(120), NOW.minusSeconds(60));
        due.assignId(UUID.randomUUID());
        when(repo.findExpirable(eq(NOW), eq(200))).thenReturn(List.of(due));
        int n = service.expireDue(200);
        assertThat(n).isEqualTo(1);
        verify(events).publish(any(), eq(ConsentEventPublisher.Action.EXPIRED));
    }

    @Test
    void expireDue_capsLimitUpper() {
        when(repo.findExpirable(eq(NOW), eq(500))).thenReturn(List.of());
        service.expireDue(10_000);
        verify(repo).findExpirable(NOW, 500);
    }

    @Test
    void expireDue_floorsLimitLower() {
        when(repo.findExpirable(eq(NOW), eq(1))).thenReturn(List.of());
        service.expireDue(0);
        verify(repo).findExpirable(NOW, 1);
    }

    private ConsentDto.GrantRequest req(String identifier) {
        return new ConsentDto.GrantRequest(identifier, "label",
                "marketing", "v1", ConsentSource.WEB_FORM, null,
                "1.2.3.4", "UA", USER, null);
    }

    private Consent stored() {
        Consent c = Consent.grant(TENANT, HASH, "label", "marketing", "v1",
                ConsentSource.WEB_FORM, null, null, null, USER, NOW, null);
        c.assignId(ID);
        return c;
    }
}
