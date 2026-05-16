package com.openlab.qualitos.quality.aiact.application;

import com.openlab.qualitos.quality.aiact.domain.AiRiskClassification;
import com.openlab.qualitos.quality.aiact.domain.AiSystem;
import com.openlab.qualitos.quality.aiact.domain.AiSystemNotFoundException;
import com.openlab.qualitos.quality.aiact.domain.AiSystemRepository;
import com.openlab.qualitos.quality.aiact.domain.AiSystemRole;
import com.openlab.qualitos.quality.aiact.domain.AiSystemStateException;
import com.openlab.qualitos.quality.aiact.domain.AiSystemStatus;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiSystemServiceTest {

    @Mock AiSystemRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock AiSystemEventPublisher events;

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID OTHER_TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();

    AiSystemService service;

    @BeforeEach
    void setup() {
        service = new AiSystemService(repo, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.existsByTenantAndReference(any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            AiSystem s = inv.getArgument(0);
            if (s.getId() == null) s.assignId(ID);
            return s;
        });
    }

    @Test
    void draft_persistsAndPublishes() {
        AiSystemDto.View v = service.draft(draftReq("REF-1", AiRiskClassification.LIMITED));
        verify(events).publish(any(), eq(AiSystemEventPublisher.Action.DRAFTED));
        assertThat(v.status()).isEqualTo(AiSystemStatus.DRAFT);
        assertThat(v.requiresTransparency()).isTrue();
    }

    @Test
    void draft_duplicateRef_throws() {
        when(repo.existsByTenantAndReference(TENANT, "REF-DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.draft(draftReq("REF-DUP", AiRiskClassification.LIMITED)))
                .isInstanceOf(AiSystemStateException.class);
    }

    @Test
    void draft_missingActor_throws() {
        AiSystemDto.DraftRequest r = new AiSystemDto.DraftRequest(
                "REF-1", "Name", null, null, "p",
                AiRiskClassification.MINIMAL_OR_NO, AiSystemRole.PROVIDER, false,
                null, null, null, null, null, null, null, null, null);
        assertThatThrownBy(() -> service.draft(r))
                .isInstanceOf(AiSystemStateException.class);
    }

    @Test
    void draft_missingRisk_throws() {
        AiSystemDto.DraftRequest r = new AiSystemDto.DraftRequest(
                "REF-1", "Name", null, null, "p",
                null, AiSystemRole.PROVIDER, false,
                null, null, null, null, null, null, null, null, USER);
        assertThatThrownBy(() -> service.draft(r))
                .isInstanceOf(AiSystemStateException.class);
    }

    @Test
    void draft_missingRole_throws() {
        AiSystemDto.DraftRequest r = new AiSystemDto.DraftRequest(
                "REF-1", "Name", null, null, "p",
                AiRiskClassification.MINIMAL_OR_NO, null, false,
                null, null, null, null, null, null, null, null, USER);
        assertThatThrownBy(() -> service.draft(r))
                .isInstanceOf(AiSystemStateException.class);
    }

    @Test
    void edit_modifies() {
        AiSystem s = makeDraft(AiRiskClassification.LIMITED);
        when(repo.findById(ID)).thenReturn(Optional.of(s));
        AiSystemDto.View v = service.edit(ID, new AiSystemDto.EditRequest(
                "New name", null, null, "purpose updated",
                AiRiskClassification.MINIMAL_OR_NO, AiSystemRole.DEPLOYER, true,
                null, null, null, null, null,
                null, null, null));
        assertThat(v.name()).isEqualTo("New name");
        assertThat(v.role()).isEqualTo(AiSystemRole.DEPLOYER);
        verify(events).publish(any(), eq(AiSystemEventPublisher.Action.EDITED));
    }

    @Test
    void edit_crossTenant_404() {
        AiSystem foreign = AiSystem.draft(OTHER_TENANT, "REF-2", "N", null, null, "p",
                AiRiskClassification.LIMITED, AiSystemRole.PROVIDER, false,
                null, null, null, null, null, null, null, null, USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.edit(ID, new AiSystemDto.EditRequest(
                "X", null, null, "p",
                AiRiskClassification.LIMITED, AiSystemRole.PROVIDER, false,
                null, null, null, null, null, null, null, null)))
                .isInstanceOf(AiSystemNotFoundException.class);
    }

    @Test
    void edit_missingRisk_throws() {
        AiSystem s = makeDraft(AiRiskClassification.LIMITED);
        when(repo.findById(ID)).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> service.edit(ID, new AiSystemDto.EditRequest(
                "n", null, null, "p", null, AiSystemRole.PROVIDER, false,
                null, null, null, null, null, null, null, null)))
                .isInstanceOf(AiSystemStateException.class);
    }

    @Test
    void edit_missingRole_throws() {
        AiSystem s = makeDraft(AiRiskClassification.LIMITED);
        when(repo.findById(ID)).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> service.edit(ID, new AiSystemDto.EditRequest(
                "n", null, null, "p",
                AiRiskClassification.MINIMAL_OR_NO, null, false,
                null, null, null, null, null, null, null, null)))
                .isInstanceOf(AiSystemStateException.class);
    }

    @Test
    void register_publishes() {
        AiSystem s = makeDraft(AiRiskClassification.MINIMAL_OR_NO);
        when(repo.findById(ID)).thenReturn(Optional.of(s));
        AiSystemDto.View v = service.register(ID);
        assertThat(v.status()).isEqualTo(AiSystemStatus.REGISTERED);
        verify(events).publish(any(), eq(AiSystemEventPublisher.Action.REGISTERED));
    }

    @Test
    void putInUse_publishes() {
        AiSystem s = makeDraft(AiRiskClassification.MINIMAL_OR_NO);
        s.register(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(s));
        AiSystemDto.View v = service.putInUse(ID);
        assertThat(v.status()).isEqualTo(AiSystemStatus.IN_USE);
        verify(events).publish(any(), eq(AiSystemEventPublisher.Action.PUT_IN_USE));
    }

    @Test
    void decommission_publishes() {
        AiSystem s = makeDraft(AiRiskClassification.MINIMAL_OR_NO);
        s.register(NOW);
        s.putInUse(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(s));
        AiSystemDto.View v = service.decommission(ID);
        assertThat(v.status()).isEqualTo(AiSystemStatus.DECOMMISSIONED);
        verify(events).publish(any(), eq(AiSystemEventPublisher.Action.DECOMMISSIONED));
    }

    @Test
    void withdraw_publishes() {
        AiSystem s = makeDraft(AiRiskClassification.MINIMAL_OR_NO);
        when(repo.findById(ID)).thenReturn(Optional.of(s));
        AiSystemDto.View v = service.withdraw(ID, new AiSystemDto.WithdrawRequest("reason"));
        assertThat(v.status()).isEqualTo(AiSystemStatus.WITHDRAWN);
        verify(events).publish(any(), eq(AiSystemEventPublisher.Action.WITHDRAWN));
    }

    @Test
    void delete_draftOnly() {
        AiSystem s = makeDraft(AiRiskClassification.LIMITED);
        when(repo.findById(ID)).thenReturn(Optional.of(s));
        service.delete(ID);
        verify(repo).delete(ID);
        verify(events).publish(any(), eq(AiSystemEventPublisher.Action.DELETED));
    }

    @Test
    void delete_nonDraft_throws() {
        AiSystem s = makeDraft(AiRiskClassification.MINIMAL_OR_NO);
        s.register(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(AiSystemStateException.class);
        verify(repo, never()).delete(any());
    }

    @Test
    void get_notFound_throws() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(AiSystemNotFoundException.class);
    }

    @Test
    void get_crossTenant_404() {
        AiSystem foreign = AiSystem.draft(OTHER_TENANT, "REF-1", "n", null, null, "p",
                AiRiskClassification.LIMITED, AiSystemRole.PROVIDER, false,
                null, null, null, null, null, null, null, null, USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(AiSystemNotFoundException.class);
    }

    @Test
    void list_all_returnsTenantOnly() {
        when(repo.findByTenant(TENANT)).thenReturn(List.of(makeDraft(AiRiskClassification.LIMITED)));
        List<AiSystemDto.View> all = service.list(null);
        assertThat(all).hasSize(1);
    }

    @Test
    void list_filteredByStatus_returnsSubset() {
        when(repo.findByTenantAndStatus(TENANT, AiSystemStatus.DRAFT))
                .thenReturn(List.of(makeDraft(AiRiskClassification.LIMITED)));
        assertThat(service.list(AiSystemStatus.DRAFT)).hasSize(1);
    }

    @Test
    void listByRisk_returnsSubset() {
        when(repo.findByTenantAndRiskClassification(TENANT, AiRiskClassification.HIGH))
                .thenReturn(List.of(makeDraft(AiRiskClassification.HIGH)));
        assertThat(service.listByRiskClassification(AiRiskClassification.HIGH)).hasSize(1);
    }

    @Test
    void listByRisk_nullRisk_throws() {
        assertThatThrownBy(() -> service.listByRiskClassification(null))
                .isInstanceOf(AiSystemStateException.class);
    }

    @Test
    void getByReference_blank_throws() {
        assertThatThrownBy(() -> service.getByReference(" "))
                .isInstanceOf(AiSystemStateException.class);
    }

    @Test
    void getByReference_notFound_throws() {
        when(repo.findByTenantAndReference(TENANT, "REF-X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByReference("REF-X"))
                .isInstanceOf(AiSystemNotFoundException.class);
    }

    @Test
    void getByReference_found_ok() {
        when(repo.findByTenantAndReference(TENANT, "REF-1"))
                .thenReturn(Optional.of(makeDraft(AiRiskClassification.LIMITED)));
        AiSystemDto.View v = service.getByReference("REF-1");
        assertThat(v.reference()).isEqualTo("REF-1");
    }

    @Test
    void constructor_withoutEventsPublisher_usesNoOp() {
        AiSystemService s2 = new AiSystemService(repo, tenantProvider, CLOCK);
        // Should not blow up
        s2.draft(draftReq("REF-NOOP", AiRiskClassification.LIMITED));
    }

    private AiSystemDto.DraftRequest draftReq(String ref, AiRiskClassification risk) {
        return new AiSystemDto.DraftRequest(
                ref, "Name", null, "Provider", "purpose",
                risk, AiSystemRole.PROVIDER, false,
                null, null, null, null, null,
                null, Set.of(), Set.of(), USER);
    }

    private AiSystem makeDraft(AiRiskClassification risk) {
        AiSystem s = AiSystem.draft(TENANT, "REF-1", "Name", null, null, "purpose",
                risk, AiSystemRole.PROVIDER, false,
                null, null, null, null, null,
                null, null, null, USER, NOW);
        s.assignId(ID);
        return s;
    }
}
