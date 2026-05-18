package com.openlab.qualitos.quality.aieudb.application;

import com.openlab.qualitos.quality.aieudb.domain.EudbRegistration;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistrationNotFoundException;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistrationRepository;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistrationStateException;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistrationStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EudbRegistrationServiceTest {

    @Mock EudbRegistrationRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock EudbRegistrationEventPublisher events;

    static final Instant NOW = Instant.parse("2026-05-17T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID OTHER = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();

    EudbRegistrationService service;

    @BeforeEach
    void setup() {
        service = new EudbRegistrationService(repo, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.existsByTenantAndReference(any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            EudbRegistration r = inv.getArgument(0);
            if (r.getId() == null) r.assignId(ID);
            return r;
        });
    }

    @Test
    void draft_publishes() {
        EudbRegistrationDto.View v = service.draft(req("REF-1"));
        verify(events).publish(any(), eq(EudbRegistrationEventPublisher.Action.DRAFTED));
        assertThat(v.status()).isEqualTo(EudbRegistrationStatus.DRAFT);
    }

    @Test
    void draft_duplicate_throws() {
        when(repo.existsByTenantAndReference(TENANT, "REF-DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.draft(req("REF-DUP")))
                .isInstanceOf(EudbRegistrationStateException.class);
    }

    @Test
    void draft_missingActor_throws() {
        EudbRegistrationDto.DraftRequest r = new EudbRegistrationDto.DraftRequest(
                "REF-1", SYS, "Acme", null, "FR", "purpose", null, null);
        assertThatThrownBy(() -> service.draft(r))
                .isInstanceOf(EudbRegistrationStateException.class);
    }

    @Test
    void draft_missingSystem_throws() {
        EudbRegistrationDto.DraftRequest r = new EudbRegistrationDto.DraftRequest(
                "REF-1", null, "Acme", null, "FR", "purpose", null, USER);
        assertThatThrownBy(() -> service.draft(r))
                .isInstanceOf(EudbRegistrationStateException.class);
    }

    @Test
    void edit_ok() {
        EudbRegistration r = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(r));
        EudbRegistrationDto.View v = service.edit(ID, new EudbRegistrationDto.EditRequest(
                "Acme 2", null, "DE", "new", "doc2"));
        assertThat(v.providerEntityName()).isEqualTo("Acme 2");
        verify(events).publish(any(), eq(EudbRegistrationEventPublisher.Action.EDITED));
    }

    @Test
    void edit_crossTenant_404() {
        EudbRegistration foreign = EudbRegistration.draft(OTHER, "REF-1", SYS,
                "Acme", null, "FR", "purpose", null, USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.edit(ID, new EudbRegistrationDto.EditRequest(
                "x", null, "FR", "p", null)))
                .isInstanceOf(EudbRegistrationNotFoundException.class);
    }

    @Test
    void submit_ok() {
        EudbRegistration r = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(r));
        EudbRegistrationDto.View v = service.submit(ID,
                new EudbRegistrationDto.SubmitRequest(USER));
        assertThat(v.status()).isEqualTo(EudbRegistrationStatus.SUBMITTED);
        verify(events).publish(any(), eq(EudbRegistrationEventPublisher.Action.SUBMITTED));
    }

    @Test
    void submit_missingActor_throws() {
        EudbRegistration r = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.submit(ID,
                new EudbRegistrationDto.SubmitRequest(null)))
                .isInstanceOf(EudbRegistrationStateException.class);
    }

    @Test
    void markRegistered_ok() {
        EudbRegistration r = ready();
        r.submit(USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(r));
        EudbRegistrationDto.View v = service.markRegistered(ID,
                new EudbRegistrationDto.MarkRegisteredRequest("EUDB-AI-ABC123", NOW));
        assertThat(v.status()).isEqualTo(EudbRegistrationStatus.REGISTERED);
        verify(events).publish(any(), eq(EudbRegistrationEventPublisher.Action.REGISTERED));
    }

    @Test
    void markRegistered_missingEudbId_throws() {
        EudbRegistration r = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.markRegistered(ID,
                new EudbRegistrationDto.MarkRegisteredRequest(" ", NOW)))
                .isInstanceOf(EudbRegistrationStateException.class);
    }

    @Test
    void markRegistered_missingDate_throws() {
        EudbRegistration r = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.markRegistered(ID,
                new EudbRegistrationDto.MarkRegisteredRequest("EUDB-AI-ABC123", null)))
                .isInstanceOf(EudbRegistrationStateException.class);
    }

    @Test
    void declareUpdate_ok() {
        EudbRegistration r = registered();
        when(repo.findById(ID)).thenReturn(Optional.of(r));
        Instant upd = NOW.plusSeconds(86400);
        EudbRegistrationDto.View v = service.declareUpdate(ID,
                new EudbRegistrationDto.DeclareUpdateRequest("retrained", upd));
        assertThat(v.status()).isEqualTo(EudbRegistrationStatus.UPDATED);
        verify(events).publish(any(), eq(EudbRegistrationEventPublisher.Action.UPDATED));
    }

    @Test
    void reject_ok() {
        EudbRegistration r = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(r));
        EudbRegistrationDto.View v = service.reject(ID,
                new EudbRegistrationDto.RejectRequest("incomplete"));
        assertThat(v.status()).isEqualTo(EudbRegistrationStatus.REJECTED);
        verify(events).publish(any(), eq(EudbRegistrationEventPublisher.Action.REJECTED));
    }

    @Test
    void retire_ok() {
        EudbRegistration r = registered();
        when(repo.findById(ID)).thenReturn(Optional.of(r));
        EudbRegistrationDto.View v = service.retire(ID,
                new EudbRegistrationDto.RetireRequest("EOL"));
        assertThat(v.status()).isEqualTo(EudbRegistrationStatus.RETIRED);
        verify(events).publish(any(), eq(EudbRegistrationEventPublisher.Action.RETIRED));
    }

    @Test
    void delete_draftOnly() {
        EudbRegistration r = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(r));
        service.delete(ID);
        verify(repo).delete(ID);
        verify(events).publish(any(), eq(EudbRegistrationEventPublisher.Action.DELETED));
    }

    @Test
    void delete_nonDraft_throws() {
        EudbRegistration r = ready();
        r.submit(USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(EudbRegistrationStateException.class);
        verify(repo, never()).delete(any());
    }

    @Test
    void get_notFound_throws() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(EudbRegistrationNotFoundException.class);
    }

    @Test
    void get_crossTenant_404() {
        EudbRegistration foreign = EudbRegistration.draft(OTHER, "REF-1", SYS,
                "Acme", null, "FR", "purpose", null, USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(EudbRegistrationNotFoundException.class);
    }

    @Test
    void list_all() {
        when(repo.findByTenant(TENANT)).thenReturn(List.of(ready()));
        assertThat(service.list(null)).hasSize(1);
    }

    @Test
    void list_byStatus() {
        when(repo.findByTenantAndStatus(TENANT, EudbRegistrationStatus.DRAFT))
                .thenReturn(List.of(ready()));
        assertThat(service.list(EudbRegistrationStatus.DRAFT)).hasSize(1);
    }

    @Test
    void listByAiSystem_ok() {
        when(repo.findByTenantAndAiSystemId(TENANT, SYS)).thenReturn(List.of(ready()));
        assertThat(service.listByAiSystem(SYS)).hasSize(1);
    }

    @Test
    void listByAiSystem_null_throws() {
        assertThatThrownBy(() -> service.listByAiSystem(null))
                .isInstanceOf(EudbRegistrationStateException.class);
    }

    @Test
    void getByReference_blank_throws() {
        assertThatThrownBy(() -> service.getByReference(" "))
                .isInstanceOf(EudbRegistrationStateException.class);
    }

    @Test
    void getByReference_notFound_throws() {
        when(repo.findByTenantAndReference(TENANT, "REF-X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByReference("REF-X"))
                .isInstanceOf(EudbRegistrationNotFoundException.class);
    }

    @Test
    void getByReference_found() {
        when(repo.findByTenantAndReference(TENANT, "REF-1"))
                .thenReturn(Optional.of(ready()));
        assertThat(service.getByReference("REF-1").reference()).isEqualTo("REF-1");
    }

    @Test
    void getByEudbId_blank_throws() {
        assertThatThrownBy(() -> service.getByEudbId(" "))
                .isInstanceOf(EudbRegistrationStateException.class);
    }

    @Test
    void getByEudbId_notFound_throws() {
        when(repo.findByTenantAndEudbId(TENANT, "EUDB-AI-XXXXXX"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByEudbId("EUDB-AI-XXXXXX"))
                .isInstanceOf(EudbRegistrationNotFoundException.class);
    }

    @Test
    void getByEudbId_found() {
        when(repo.findByTenantAndEudbId(TENANT, "EUDB-AI-ABC123"))
                .thenReturn(Optional.of(registered()));
        assertThat(service.getByEudbId("EUDB-AI-ABC123").eudbId()).isEqualTo("EUDB-AI-ABC123");
    }

    @Test
    void constructor_withoutEvents_usesNoOp() {
        EudbRegistrationService s2 = new EudbRegistrationService(repo, tenantProvider, CLOCK);
        s2.draft(req("REF-N"));
    }

    private EudbRegistrationDto.DraftRequest req(String ref) {
        return new EudbRegistrationDto.DraftRequest(ref, SYS,
                "Acme Corp", "EU Rep SARL", "FR",
                "purpose summary", "TECH-DOC-1", USER);
    }

    private EudbRegistration ready() {
        EudbRegistration r = EudbRegistration.draft(TENANT, "REF-1", SYS,
                "Acme Corp", "EU Rep SARL", "FR",
                "purpose summary", "TECH-DOC-1", USER, NOW);
        r.assignId(ID);
        return r;
    }

    private EudbRegistration registered() {
        EudbRegistration r = ready();
        r.submit(USER, NOW);
        r.markRegistered("EUDB-AI-ABC123", NOW, NOW);
        return r;
    }
}
