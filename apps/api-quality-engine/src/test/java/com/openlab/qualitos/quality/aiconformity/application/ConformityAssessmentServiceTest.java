package com.openlab.qualitos.quality.aiconformity.application;

import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessment;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentNotFoundException;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentRepository;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentStateException;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentStatus;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityProcedure;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConformityAssessmentServiceTest {

    @Mock ConformityAssessmentRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock ConformityAssessmentEventPublisher events;

    static final Instant NOW = Instant.parse("2026-05-17T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID OTHER = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final UUID QMS = UUID.randomUUID();
    static final Instant VALID_UNTIL = NOW.plusSeconds(365L * 86400);

    ConformityAssessmentService service;

    @BeforeEach
    void setup() {
        service = new ConformityAssessmentService(repo, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.existsByTenantAndReference(any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            ConformityAssessment a = inv.getArgument(0);
            if (a.getId() == null) a.assignId(ID);
            return a;
        });
    }

    @Test
    void plan_publishes() {
        ConformityAssessmentDto.View v = service.plan(req("REF-1"));
        verify(events).publish(any(), eq(ConformityAssessmentEventPublisher.Action.PLANNED));
        assertThat(v.status()).isEqualTo(ConformityAssessmentStatus.PLANNED);
    }

    @Test
    void plan_duplicate_throws() {
        when(repo.existsByTenantAndReference(TENANT, "REF-DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.plan(req("REF-DUP")))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void plan_missingActor_throws() {
        ConformityAssessmentDto.PlanRequest r = new ConformityAssessmentDto.PlanRequest(
                "REF-1", SYS, QMS, ConformityProcedure.INTERNAL_CONTROL,
                null, null, "scope", null);
        assertThatThrownBy(() -> service.plan(r))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void plan_missingSystem_throws() {
        ConformityAssessmentDto.PlanRequest r = new ConformityAssessmentDto.PlanRequest(
                "REF-1", null, QMS, ConformityProcedure.INTERNAL_CONTROL,
                null, null, "scope", USER);
        assertThatThrownBy(() -> service.plan(r))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void plan_missingProcedure_throws() {
        ConformityAssessmentDto.PlanRequest r = new ConformityAssessmentDto.PlanRequest(
                "REF-1", SYS, QMS, null, null, null, "scope", USER);
        assertThatThrownBy(() -> service.plan(r))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void edit_ok() {
        ConformityAssessment a = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(a));
        ConformityAssessmentDto.View v = service.edit(ID,
                new ConformityAssessmentDto.EditRequest(QMS, null, null, "new scope"));
        assertThat(v.scope()).isEqualTo("new scope");
        verify(events).publish(any(), eq(ConformityAssessmentEventPublisher.Action.EDITED));
    }

    @Test
    void edit_crossTenant_404() {
        ConformityAssessment foreign = ConformityAssessment.plan(OTHER, "REF-1", SYS, QMS,
                ConformityProcedure.INTERNAL_CONTROL, null, null, "scope", USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.edit(ID,
                new ConformityAssessmentDto.EditRequest(QMS, null, null, "x")))
                .isInstanceOf(ConformityAssessmentNotFoundException.class);
    }

    @Test
    void start_ok() {
        ConformityAssessment a = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(a));
        ConformityAssessmentDto.View v = service.start(ID);
        assertThat(v.status()).isEqualTo(ConformityAssessmentStatus.IN_PROGRESS);
        verify(events).publish(any(), eq(ConformityAssessmentEventPublisher.Action.STARTED));
    }

    @Test
    void certify_ok() {
        ConformityAssessment a = ready();
        a.start(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(a));
        ConformityAssessmentDto.View v = service.certify(ID,
                new ConformityAssessmentDto.CertifyRequest("CERT-1", "EU-1", VALID_UNTIL));
        assertThat(v.status()).isEqualTo(ConformityAssessmentStatus.CERTIFIED);
        verify(events).publish(any(), eq(ConformityAssessmentEventPublisher.Action.CERTIFIED));
    }

    @Test
    void markExpired_ok() {
        ConformityAssessment a = ready();
        a.start(NOW);
        a.certify("CERT-1", "EU-1", NOW.plusSeconds(1), NOW);
        Instant later = NOW.plusSeconds(2);
        Clock clock = Clock.fixed(later, ZoneOffset.UTC);
        ConformityAssessmentService s = new ConformityAssessmentService(repo,
                tenantProvider, events, clock);
        when(repo.findById(ID)).thenReturn(Optional.of(a));
        ConformityAssessmentDto.View v = s.markExpired(ID);
        assertThat(v.status()).isEqualTo(ConformityAssessmentStatus.EXPIRED);
        verify(events).publish(any(), eq(ConformityAssessmentEventPublisher.Action.EXPIRED));
    }

    @Test
    void revoke_ok() {
        ConformityAssessment a = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(a));
        ConformityAssessmentDto.View v = service.revoke(ID,
                new ConformityAssessmentDto.RevokeRequest("misuse"));
        assertThat(v.status()).isEqualTo(ConformityAssessmentStatus.REVOKED);
        verify(events).publish(any(), eq(ConformityAssessmentEventPublisher.Action.REVOKED));
    }

    @Test
    void markFailed_ok() {
        ConformityAssessment a = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(a));
        ConformityAssessmentDto.View v = service.markFailed(ID,
                new ConformityAssessmentDto.FailRequest("non-conformities"));
        assertThat(v.status()).isEqualTo(ConformityAssessmentStatus.FAILED);
        verify(events).publish(any(), eq(ConformityAssessmentEventPublisher.Action.FAILED));
    }

    @Test
    void delete_plannedOnly() {
        ConformityAssessment a = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(a));
        service.delete(ID);
        verify(repo).delete(ID);
        verify(events).publish(any(), eq(ConformityAssessmentEventPublisher.Action.DELETED));
    }

    @Test
    void delete_nonPlanned_throws() {
        ConformityAssessment a = ready();
        a.start(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(a));
        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(ConformityAssessmentStateException.class);
        verify(repo, never()).delete(any());
    }

    @Test
    void get_notFound_throws() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(ConformityAssessmentNotFoundException.class);
    }

    @Test
    void get_crossTenant_404() {
        ConformityAssessment foreign = ConformityAssessment.plan(OTHER, "REF-1", SYS, QMS,
                ConformityProcedure.INTERNAL_CONTROL, null, null, "scope", USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(ConformityAssessmentNotFoundException.class);
    }

    @Test
    void list_all() {
        when(repo.findByTenant(TENANT)).thenReturn(List.of(ready()));
        assertThat(service.list(null)).hasSize(1);
    }

    @Test
    void list_byStatus() {
        when(repo.findByTenantAndStatus(TENANT, ConformityAssessmentStatus.PLANNED))
                .thenReturn(List.of(ready()));
        assertThat(service.list(ConformityAssessmentStatus.PLANNED)).hasSize(1);
    }

    @Test
    void listByAiSystem_ok() {
        when(repo.findByTenantAndAiSystemId(TENANT, SYS))
                .thenReturn(List.of(ready()));
        assertThat(service.listByAiSystem(SYS)).hasSize(1);
    }

    @Test
    void listByAiSystem_null_throws() {
        assertThatThrownBy(() -> service.listByAiSystem(null))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void listExpiringCertificates_ok() {
        when(repo.findExpiringCertificates(eq(TENANT), any(), anyInt()))
                .thenReturn(List.of(ready()));
        assertThat(service.listExpiringCertificates(100)).hasSize(1);
    }

    @Test
    void listExpiringCertificates_outOfRange_throws() {
        assertThatThrownBy(() -> service.listExpiringCertificates(0))
                .isInstanceOf(ConformityAssessmentStateException.class);
        assertThatThrownBy(() -> service.listExpiringCertificates(1001))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void getByReference_blank_throws() {
        assertThatThrownBy(() -> service.getByReference(" "))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void getByReference_notFound_throws() {
        when(repo.findByTenantAndReference(TENANT, "REF-X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByReference("REF-X"))
                .isInstanceOf(ConformityAssessmentNotFoundException.class);
    }

    @Test
    void getByReference_found() {
        when(repo.findByTenantAndReference(TENANT, "REF-1"))
                .thenReturn(Optional.of(ready()));
        assertThat(service.getByReference("REF-1").reference()).isEqualTo("REF-1");
    }

    @Test
    void constructor_withoutEvents_usesNoOp() {
        ConformityAssessmentService s2 = new ConformityAssessmentService(repo, tenantProvider, CLOCK);
        s2.plan(req("REF-N"));
    }

    private ConformityAssessmentDto.PlanRequest req(String ref) {
        return new ConformityAssessmentDto.PlanRequest(ref, SYS, QMS,
                ConformityProcedure.INTERNAL_CONTROL, null, null, "scope", USER);
    }

    private ConformityAssessment ready() {
        ConformityAssessment a = ConformityAssessment.plan(TENANT, "REF-1", SYS, QMS,
                ConformityProcedure.INTERNAL_CONTROL, null, null, "scope", USER, NOW);
        a.assignId(ID);
        return a;
    }
}
