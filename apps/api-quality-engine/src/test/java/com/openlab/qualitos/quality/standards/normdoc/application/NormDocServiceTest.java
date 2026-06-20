package com.openlab.qualitos.quality.standards.normdoc.application;

import com.openlab.qualitos.quality.standards.StandardNotFoundException;
import com.openlab.qualitos.quality.standards.normdoc.domain.GeneratedNormDoc;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocGenerationCommand;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocGenerator;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocNotFoundException;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocRepository;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocSection;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocStateException;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocStatus;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormativeDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NormDocServiceTest {

    @Mock NormDocRepository repo;
    @Mock NormDocGenerator generator;
    @Mock NormDocStandardLookup standards;
    @Mock NormDocTenantProvider tenantProvider;
    @Mock NormDocActorProvider actorProvider;
    @Mock NormDocEventPublisher events;

    NormDocService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID STD = UUID.randomUUID();
    static final UUID AUTHOR = UUID.randomUUID();
    static final UUID APPROVER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-06-20T08:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        service = new NormDocService(repo, generator, standards,
                tenantProvider, actorProvider, events, CLOCK);
    }

    private NormDocDto.GenerateRequest generateRequest() {
        return new NormDocDto.GenerateRequest(STD, NormDocKind.MANUAL,
                new NormDocDto.TenantProfile("ACME", "manufacturing", "PME", "fr",
                        List.of("achats")),
                List.of(new NormDocDto.SectionSpec("ctx", "Contexte", List.of("4.1"), "cadrer")));
    }

    private GeneratedNormDoc generated() {
        return new GeneratedNormDoc("Manuel Qualité — ACME (iso-9001)",
                List.of(new NormDocSection("ctx", "Contexte", List.of("4.1"), "Corps généré")),
                "ollama");
    }

    private NormativeDocument persisted(NormDocStatus status) {
        NormativeDocument d = NormativeDocument.draftFromAi(TENANT, STD, "iso-9001",
                NormDocKind.MANUAL, "Manuel Qualité — ACME (iso-9001)",
                List.of(new NormDocSection("ctx", "Contexte", List.of("4.1"), "Corps")),
                "ollama", AUTHOR, NOW);
        d.assignId(UUID.randomUUID());
        if (status == NormDocStatus.EN_VALIDATION) {
            d.submitForReview(AUTHOR, NOW);
        }
        return d;
    }

    @Test
    void generate_buildsCommandFromStandardAndProfile_persistsDraft() {
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(actorProvider.requireActorId()).thenReturn(AUTHOR);
        when(standards.findById(STD))
                .thenReturn(Optional.of(new NormDocStandardLookup.StandardRef(
                        STD, "iso-9001", "ISO 9001:2015")));
        when(generator.generate(any())).thenReturn(generated());
        when(repo.save(any())).thenAnswer(inv -> {
            NormativeDocument d = inv.getArgument(0);
            d.assignId(UUID.randomUUID());
            return d;
        });

        NormDocDto.View view = service.generate(generateRequest());

        assertThat(view.status()).isEqualTo(NormDocStatus.BROUILLON_IA);
        assertThat(view.standardCode()).isEqualTo("iso-9001");
        assertThat(view.createdByUserId()).isEqualTo(AUTHOR);
        assertThat(view.markdown()).contains("# Manuel Qualité — ACME");

        ArgumentCaptor<NormDocGenerationCommand> cmd =
                ArgumentCaptor.forClass(NormDocGenerationCommand.class);
        verify(generator).generate(cmd.capture());
        assertThat(cmd.getValue().standardName()).isEqualTo("ISO 9001:2015");
        assertThat(cmd.getValue().organizationName()).isEqualTo("ACME");
        assertThat(cmd.getValue().knownProcesses()).containsExactly("achats");
        verify(events).publish(any(), org.mockito.ArgumentMatchers.eq(
                NormDocEventPublisher.Action.GENERATED));
    }

    @Test
    void generate_unknownStandard_throws404() {
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(actorProvider.requireActorId()).thenReturn(AUTHOR);
        when(standards.findById(STD)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.generate(generateRequest()))
                .isInstanceOf(StandardNotFoundException.class);
        verify(generator, never()).generate(any());
    }

    @Test
    void generate_nullStandardId_throws() {
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(actorProvider.requireActorId()).thenReturn(AUTHOR);
        NormDocDto.GenerateRequest req = new NormDocDto.GenerateRequest(
                null, NormDocKind.MANUAL, generateRequest().tenantProfile(),
                generateRequest().sections());
        assertThatThrownBy(() -> service.generate(req))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("standardId");
    }

    @Test
    void generate_nullKind_throws() {
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(actorProvider.requireActorId()).thenReturn(AUTHOR);
        NormDocDto.GenerateRequest req = new NormDocDto.GenerateRequest(
                STD, null, generateRequest().tenantProfile(), generateRequest().sections());
        assertThatThrownBy(() -> service.generate(req))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("kind");
    }

    @Test
    void generate_nullProfile_throws() {
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(actorProvider.requireActorId()).thenReturn(AUTHOR);
        when(standards.findById(STD))
                .thenReturn(Optional.of(new NormDocStandardLookup.StandardRef(
                        STD, "iso-9001", "ISO 9001")));
        NormDocDto.GenerateRequest req = new NormDocDto.GenerateRequest(
                STD, NormDocKind.MANUAL, null, generateRequest().sections());
        assertThatThrownBy(() -> service.generate(req))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("tenantProfile");
    }

    @Test
    void generate_emptySections_throws() {
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(actorProvider.requireActorId()).thenReturn(AUTHOR);
        when(standards.findById(STD))
                .thenReturn(Optional.of(new NormDocStandardLookup.StandardRef(
                        STD, "iso-9001", "ISO 9001")));
        NormDocDto.GenerateRequest req = new NormDocDto.GenerateRequest(
                STD, NormDocKind.MANUAL, generateRequest().tenantProfile(), List.of());
        assertThatThrownBy(() -> service.generate(req))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at least one");
    }

    @Test
    void edit_updatesDraft() {
        NormativeDocument doc = persisted(NormDocStatus.BROUILLON_IA);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NormDocDto.View view = service.edit(doc.getId(), new NormDocDto.EditRequest(
                "Titre révisé",
                List.of(new NormDocDto.SectionView("ctx", "Contexte", List.of("4.1"), "Nouveau"))));
        assertThat(view.title()).isEqualTo("Titre révisé");
        verify(events).publish(any(), org.mockito.ArgumentMatchers.eq(
                NormDocEventPublisher.Action.EDITED));
    }

    @Test
    void edit_emptySections_throws() {
        NormativeDocument doc = persisted(NormDocStatus.BROUILLON_IA);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.findById(doc.getId())).thenReturn(Optional.of(doc));
        assertThatThrownBy(() -> service.edit(doc.getId(),
                new NormDocDto.EditRequest("t", List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void edit_nullSections_throws() {
        NormativeDocument doc = persisted(NormDocStatus.BROUILLON_IA);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.findById(doc.getId())).thenReturn(Optional.of(doc));
        assertThatThrownBy(() -> service.edit(doc.getId(),
                new NormDocDto.EditRequest("t", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generate_nullSections_throws() {
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(actorProvider.requireActorId()).thenReturn(AUTHOR);
        when(standards.findById(STD))
                .thenReturn(Optional.of(new NormDocStandardLookup.StandardRef(
                        STD, "iso-9001", "ISO 9001")));
        NormDocDto.GenerateRequest req = new NormDocDto.GenerateRequest(
                STD, NormDocKind.MANUAL, generateRequest().tenantProfile(), null);
        assertThatThrownBy(() -> service.generate(req))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at least one");
    }

    @Test
    void submit_movesToReview() {
        NormativeDocument doc = persisted(NormDocStatus.BROUILLON_IA);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(actorProvider.requireActorId()).thenReturn(AUTHOR);
        when(repo.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NormDocDto.View view = service.submitForReview(doc.getId());
        assertThat(view.status()).isEqualTo(NormDocStatus.EN_VALIDATION);
        verify(events).publish(any(), org.mockito.ArgumentMatchers.eq(
                NormDocEventPublisher.Action.SUBMITTED));
    }

    @Test
    void approve_signsWithJwtActor() {
        NormativeDocument doc = persisted(NormDocStatus.EN_VALIDATION);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(actorProvider.requireActorId()).thenReturn(APPROVER);
        when(repo.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NormDocDto.View view = service.approve(doc.getId(),
                new NormDocDto.ApproveRequest("signature-humaine", "OK"));
        assertThat(view.status()).isEqualTo(NormDocStatus.APPROUVE);
        assertThat(view.approvedByUserId()).isEqualTo(APPROVER);
        assertThat(view.humanSignature()).isEqualTo("signature-humaine");
        verify(events).publish(any(), org.mockito.ArgumentMatchers.eq(
                NormDocEventPublisher.Action.APPROVED));
    }

    @Test
    void reject_returnsToBrouillon() {
        NormativeDocument doc = persisted(NormDocStatus.EN_VALIDATION);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NormDocDto.View view = service.reject(doc.getId(),
                new NormDocDto.RejectRequest("Sections vides"));
        assertThat(view.status()).isEqualTo(NormDocStatus.BROUILLON_IA);
        assertThat(view.rejectionReason()).isEqualTo("Sections vides");
        verify(events).publish(any(), org.mockito.ArgumentMatchers.eq(
                NormDocEventPublisher.Action.REJECTED));
    }

    @Test
    void get_returnsView() {
        NormativeDocument doc = persisted(NormDocStatus.BROUILLON_IA);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.findById(doc.getId())).thenReturn(Optional.of(doc));
        assertThat(service.get(doc.getId()).id()).isEqualTo(doc.getId());
    }

    @Test
    void loadForTenant_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(id)).isInstanceOf(NormDocNotFoundException.class);
    }

    @Test
    void loadForTenant_crossTenant_throws404() {
        NormativeDocument doc = persisted(NormDocStatus.BROUILLON_IA);
        when(tenantProvider.requireTenantId()).thenReturn(UUID.randomUUID()); // autre tenant
        when(repo.findById(doc.getId())).thenReturn(Optional.of(doc));
        assertThatThrownBy(() -> service.get(doc.getId()))
                .isInstanceOf(NormDocNotFoundException.class);
    }

    @Test
    void list_all_andByStatus() {
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.findByTenant(TENANT)).thenReturn(List.of(persisted(NormDocStatus.BROUILLON_IA)));
        assertThat(service.list(null)).hasSize(1);

        when(repo.findByTenantAndStatus(TENANT, NormDocStatus.APPROUVE)).thenReturn(List.of());
        assertThat(service.list(NormDocStatus.APPROUVE)).isEmpty();
    }

    @Test
    void delete_draftAllowed() {
        NormativeDocument doc = persisted(NormDocStatus.BROUILLON_IA);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.findById(doc.getId())).thenReturn(Optional.of(doc));
        service.delete(doc.getId());
        verify(repo).delete(doc.getId());
        verify(events).publish(any(), org.mockito.ArgumentMatchers.eq(
                NormDocEventPublisher.Action.DELETED));
    }

    @Test
    void delete_approvedForbidden() {
        NormativeDocument doc = persisted(NormDocStatus.EN_VALIDATION);
        doc.approve(APPROVER, "sig", null, NOW);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.findById(doc.getId())).thenReturn(Optional.of(doc));
        assertThatThrownBy(() -> service.delete(doc.getId()))
                .isInstanceOf(NormDocStateException.class)
                .hasMessageContaining("audit");
        verify(repo, never()).delete(any());
    }
}
