package com.openlab.qualitos.quality.standards.normdoc.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.standards.Standard;
import com.openlab.qualitos.quality.standards.StandardRepository;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocEventPublisher;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocStandardLookup;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocSection;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormativeDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NormDocAdaptersTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID STD = UUID.randomUUID();
    static final UUID AUTHOR = UUID.randomUUID();
    static final UUID APPROVER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-06-20T08:00:00Z");

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ---- Tenant provider ----

    @Test
    void tenantProvider_returnsContext() {
        TenantContext.setTenantId(TENANT.toString());
        assertThat(new NormDocTenantContextProvider().requireTenantId()).isEqualTo(TENANT);
    }

    @Test
    void tenantProvider_missingThrows() {
        assertThatThrownBy(() -> new NormDocTenantContextProvider().requireTenantId())
                .isInstanceOf(MissingTenantContextException.class);
    }

    // ---- Actor provider ----

    @Test
    void actorProvider_returnsJwtSubject() {
        UUID sub = UUID.fromString("11111111-1111-1111-1111-111111111111");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(sub.toString(), "n/a", List.of()));
        UUID actor = new NormDocCurrentUserActorProvider().requireActorId();
        assertThat(actor).isEqualTo(sub);
    }

    @Test
    void actorProvider_unauthenticatedThrows() {
        assertThatThrownBy(() -> new NormDocCurrentUserActorProvider().requireActorId())
                .isInstanceOf(MissingTenantContextException.class);
    }

    // ---- Standard lookup ----

    @Test
    void standardLookup_found(@Mock StandardRepository repo) {
        Standard s = new Standard();
        s.setId(STD);
        s.setCode("iso-9001");
        s.setFullName("ISO 9001:2015");
        when(repo.findById(STD)).thenReturn(Optional.of(s));
        Optional<NormDocStandardLookup.StandardRef> ref =
                new StandardLookupAdapter(repo).findById(STD);
        assertThat(ref).isPresent();
        assertThat(ref.get().code()).isEqualTo("iso-9001");
        assertThat(ref.get().fullName()).isEqualTo("ISO 9001:2015");
    }

    @Test
    void standardLookup_absent(@Mock StandardRepository repo) {
        when(repo.findById(STD)).thenReturn(Optional.empty());
        assertThat(new StandardLookupAdapter(repo).findById(STD)).isEmpty();
    }

    // ---- Audit publisher ----

    private NormativeDocument doc() {
        return NormativeDocument.draftFromAi(TENANT, STD, "iso-9001", NormDocKind.POLICY,
                "Politique", List.of(new NormDocSection("k", "t", List.of(), "b")),
                "ollama", AUTHOR, NOW);
    }

    @Test
    void auditPublisher_generated_recordsCreator(@Mock AuditEventService audit) {
        NormativeDocument d = doc();
        d.assignId(UUID.randomUUID());
        new AuditLogNormDocEventPublisher(audit)
                .publish(d, NormDocEventPublisher.Action.GENERATED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> req =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(audit).recordForTenant(eq(TENANT), req.capture());
        assertThat(req.getValue().action()).isEqualTo("standards.normdoc.generated");
        assertThat(req.getValue().actorType()).isEqualTo(ActorType.USER);
        assertThat(req.getValue().actorUserId()).isEqualTo(AUTHOR);
        assertThat(req.getValue().resourceType()).isEqualTo("standard-norm-document");
        assertThat(req.getValue().payloadJson()).contains("iso-9001").contains("POLICY");
    }

    @Test
    void auditPublisher_submitted_recordsSubmitter(@Mock AuditEventService audit) {
        NormativeDocument d = doc();
        d.assignId(UUID.randomUUID());
        d.submitForReview(APPROVER, NOW);
        new AuditLogNormDocEventPublisher(audit)
                .publish(d, NormDocEventPublisher.Action.SUBMITTED);
        ArgumentCaptor<AuditEventDto.RecordEventRequest> req =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(audit).recordForTenant(eq(TENANT), req.capture());
        assertThat(req.getValue().actorUserId()).isEqualTo(APPROVER);
        assertThat(req.getValue().action()).isEqualTo("standards.normdoc.submitted");
    }

    @Test
    void auditPublisher_approved_recordsApprover(@Mock AuditEventService audit) {
        NormativeDocument d = doc();
        d.assignId(UUID.randomUUID());
        d.submitForReview(AUTHOR, NOW);
        d.approve(APPROVER, "sig", null, NOW);
        new AuditLogNormDocEventPublisher(audit)
                .publish(d, NormDocEventPublisher.Action.APPROVED);
        ArgumentCaptor<AuditEventDto.RecordEventRequest> req =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(audit).recordForTenant(eq(TENANT), req.capture());
        assertThat(req.getValue().actorUserId()).isEqualTo(APPROVER);
        assertThat(req.getValue().action()).isEqualTo("standards.normdoc.approved");
    }

    @Test
    void noOpEventPublisher_doesNothing() {
        NormDocEventPublisher noop = new NormDocEventPublisher.NoOp();
        noop.publish(doc(), NormDocEventPublisher.Action.DELETED); // ne lève rien
    }
}
