package com.openlab.qualitos.quality.processoragreements.application;

import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreement;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementNotFoundException;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementRepository;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementStateException;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementStatus;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProcessorAgreementServiceTest {

    @Mock ProcessorAgreementRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock ProcessorAgreementEventPublisher events;

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();

    ProcessorAgreementService service;

    @BeforeEach
    void setup() {
        service = new ProcessorAgreementService(repo, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.existsByTenantAndReference(any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            ProcessorAgreement a = inv.getArgument(0);
            if (a.getId() == null) a.assignId(ID);
            return a;
        });
    }

    @Test
    void create_persistsDraft_andPublishes() {
        ProcessorAgreementDto.View v = service.create(req("DPA-2026-001"));
        verify(events).publish(any(), eq(ProcessorAgreementEventPublisher.Action.CREATED));
        assertThat(v.status()).isEqualTo(ProcessorAgreementStatus.DRAFT);
    }

    @Test
    void create_duplicateRef_throws() {
        when(repo.existsByTenantAndReference(TENANT, "DPA-DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.create(req("DPA-DUP")))
                .isInstanceOf(ProcessorAgreementStateException.class);
    }

    @Test
    void create_missingActor_throws() {
        ProcessorAgreementDto.CreateRequest r = new ProcessorAgreementDto.CreateRequest(
                "DPA-1",         // reference
                "Acme",          // processorName
                null,            // processorLegalEntity
                null,            // processorContact
                null,            // processorDpoContact
                null,            // processorCountry
                "Services",      // servicesDescription
                Set.of(), Set.of(), Set.of(),
                null,            // transferSafeguards
                null,            // contractDocumentUrl
                null, null, null, // signedAt, effectiveFrom, expirationDate
                null,            // securityMeasures
                72,              // breachNotificationCommitmentHours
                false,           // auditRights
                null, null,      // auditRightsNotes, dataReturnOrDeletionTerms
                null             // createdByUserId
        );
        assertThatThrownBy(() -> service.create(r))
                .isInstanceOf(ProcessorAgreementStateException.class);
    }

    @Test
    void edit_onDraft_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        ProcessorAgreementDto.View v = service.edit(ID, editReq("Updated"));
        assertThat(v.processorName()).isEqualTo("Updated");
        verify(events).publish(any(), eq(ProcessorAgreementEventPublisher.Action.EDITED));
    }

    @Test
    void edit_onActive_throws() {
        ProcessorAgreement a = stored();
        // make it activatable then activate
        a.editDraft("Acme", null, "ops@acme.com", null, "US",
                "Cloud", Set.of(), Set.of(), Set.of(), null, null,
                NOW, NOW, null, null, 72, false, null, null, NOW);
        a.activate(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(a));
        assertThatThrownBy(() -> service.edit(ID, editReq("X")))
                .isInstanceOf(ProcessorAgreementStateException.class);
    }

    @Test
    void activate_succeeds() {
        ProcessorAgreement a = ProcessorAgreement.draft(TENANT, "DPA-2026-001",
                "Acme", null, "ops@acme.com", null, "US", "Cloud",
                Set.of(), Set.of(), Set.of(), null, null,
                NOW, NOW, null, null, 72, false, null, null, USER, NOW);
        a.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(a));
        ProcessorAgreementDto.View v = service.activate(ID);
        assertThat(v.status()).isEqualTo(ProcessorAgreementStatus.ACTIVE);
        verify(events).publish(any(), eq(ProcessorAgreementEventPublisher.Action.ACTIVATED));
    }

    @Test
    void activate_missingSignedAt_throws() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        assertThatThrownBy(() -> service.activate(ID))
                .isInstanceOf(ProcessorAgreementStateException.class);
    }

    @Test
    void terminate_succeeds() {
        ProcessorAgreement a = activeStored();
        when(repo.findById(ID)).thenReturn(Optional.of(a));
        ProcessorAgreementDto.View v = service.terminate(ID,
                new ProcessorAgreementDto.TerminateRequest("end of contract"));
        assertThat(v.status()).isEqualTo(ProcessorAgreementStatus.TERMINATED);
        verify(events).publish(any(), eq(ProcessorAgreementEventPublisher.Action.TERMINATED));
    }

    @Test
    void delete_onDraft_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        service.delete(ID);
        verify(repo).delete(ID);
        verify(events).publish(any(), eq(ProcessorAgreementEventPublisher.Action.DELETED));
    }

    @Test
    void delete_onActive_throws() {
        when(repo.findById(ID)).thenReturn(Optional.of(activeStored()));
        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(ProcessorAgreementStateException.class);
        verify(repo, never()).delete(any());
    }

    @Test
    void get_missing_404() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(ProcessorAgreementNotFoundException.class);
    }

    @Test
    void get_crossTenant_404_noLeak() {
        ProcessorAgreement other = ProcessorAgreement.draft(UUID.randomUUID(),
                "DPA-X", "P", null, null, null, null, "S",
                Set.of(), Set.of(), Set.of(), null, null,
                null, null, null, null, 72, false, null, null, USER, NOW);
        other.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(ProcessorAgreementNotFoundException.class);
    }

    @Test
    void list_byStatus_filters() {
        when(repo.findByTenantAndStatus(TENANT, ProcessorAgreementStatus.DRAFT))
                .thenReturn(List.of(stored()));
        assertThat(service.list(ProcessorAgreementStatus.DRAFT)).hasSize(1);
    }

    @Test
    void list_nullStatus_returnsAll() {
        when(repo.findByTenant(TENANT)).thenReturn(List.of(stored(), stored()));
        assertThat(service.list(null)).hasSize(2);
    }

    @Test
    void getByReference_found() {
        when(repo.findByTenantAndReference(TENANT, "DPA-2026-001"))
                .thenReturn(Optional.of(stored()));
        assertThat(service.getByReference("DPA-2026-001").reference()).isEqualTo("DPA-2026-001");
    }

    @Test
    void getByReference_missing_404() {
        when(repo.findByTenantAndReference(TENANT, "DPA-X"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByReference("DPA-X"))
                .isInstanceOf(ProcessorAgreementNotFoundException.class);
    }

    @Test
    void getByReference_blank_throws() {
        assertThatThrownBy(() -> service.getByReference(" "))
                .isInstanceOf(ProcessorAgreementStateException.class);
    }

    @Test
    void expireDue_marksDueAndPublishes() {
        ProcessorAgreement due = ProcessorAgreement.draft(TENANT, "DPA-EXP",
                "Acme", null, "ops@x", null, "US", "S",
                Set.of(), Set.of(), Set.of(), null, null,
                NOW.minusSeconds(86400), NOW.minusSeconds(43200),
                NOW.minusSeconds(3600), null, 72, false, null, null, USER, NOW);
        due.assignId(UUID.randomUUID());
        due.activate(NOW.minusSeconds(43200));
        when(repo.findExpirable(eq(NOW), eq(200))).thenReturn(List.of(due));
        int n = service.expireDue(200);
        assertThat(n).isEqualTo(1);
        verify(events).publish(any(), eq(ProcessorAgreementEventPublisher.Action.EXPIRED));
    }

    @Test
    void expireDue_capsLimit() {
        when(repo.findExpirable(eq(NOW), eq(500))).thenReturn(List.of());
        service.expireDue(10_000);
        verify(repo).findExpirable(NOW, 500);
    }

    private ProcessorAgreementDto.CreateRequest req(String ref) {
        return new ProcessorAgreementDto.CreateRequest(ref,
                "Acme Corp", "Acme Corp Ltd", null, null, "US",
                "Cloud hosting", Set.of(), Set.of(), Set.of(),
                null, null, null, null, null,
                null, 72, false, null, null, USER);
    }

    private ProcessorAgreementDto.EditRequest editReq(String name) {
        return new ProcessorAgreementDto.EditRequest(
                name, null, "ops@acme.com", null, "US",
                "Updated services", Set.of(), Set.of(), Set.of(),
                null, null, NOW, NOW, null, null, 24, true, null, null);
    }

    private ProcessorAgreement stored() {
        ProcessorAgreement a = ProcessorAgreement.draft(TENANT, "DPA-2026-001",
                "Acme Corp", null, null, null, "US", "Cloud",
                Set.of(), Set.of(), Set.of(), null, null,
                null, null, null, null, 72, false, null, null, USER, NOW);
        a.assignId(ID);
        return a;
    }

    private ProcessorAgreement activeStored() {
        ProcessorAgreement a = ProcessorAgreement.draft(TENANT, "DPA-2026-001",
                "Acme Corp", null, "ops@acme.com", null, "US", "Cloud",
                Set.of(), Set.of(), Set.of(), null, null,
                NOW, NOW, null, null, 72, false, null, null, USER, NOW);
        a.assignId(ID);
        a.activate(NOW);
        return a;
    }
}
