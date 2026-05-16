package com.openlab.qualitos.quality.nis2measures.application;

import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureCategory;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureNotFoundException;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureStateException;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureStatus;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2RiskMeasure;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2RiskMeasureRepository;
import com.openlab.qualitos.quality.nis2measures.domain.ResidualRiskRating;
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
class Nis2MeasureServiceTest {

    @Mock Nis2RiskMeasureRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock Nis2MeasureEventPublisher events;

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID REVIEWER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();

    Nis2MeasureService service;

    @BeforeEach
    void setup() {
        service = new Nis2MeasureService(repo, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.existsByTenantAndReference(any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            Nis2RiskMeasure m = inv.getArgument(0);
            if (m.getId() == null) m.assignId(ID);
            return m;
        });
    }

    @Test
    void plan_persistsAndPublishes() {
        Nis2MeasureDto.View v = service.plan(req("M-2026-001"));
        verify(events).publish(any(), eq(Nis2MeasureEventPublisher.Action.PLANNED));
        assertThat(v.status()).isEqualTo(Nis2MeasureStatus.PLANNED);
    }

    @Test
    void plan_duplicateRef_throws() {
        when(repo.existsByTenantAndReference(TENANT, "M-DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.plan(req("M-DUP")))
                .isInstanceOf(Nis2MeasureStateException.class);
    }

    @Test
    void plan_missingActor_throws() {
        Nis2MeasureDto.PlanRequest r = new Nis2MeasureDto.PlanRequest(
                "M-1", Nis2MeasureCategory.CRYPTOGRAPHY, "t", null,
                USER, 2, ResidualRiskRating.LOW, null, 365,
                Set.of(), Set.of(), Set.of(), null, null);
        assertThatThrownBy(() -> service.plan(r))
                .isInstanceOf(Nis2MeasureStateException.class);
    }

    @Test
    void plan_missingCategory_throws() {
        Nis2MeasureDto.PlanRequest r = new Nis2MeasureDto.PlanRequest(
                "M-1", null, "t", null,
                USER, 2, ResidualRiskRating.LOW, null, 365,
                Set.of(), Set.of(), Set.of(), null, USER);
        assertThatThrownBy(() -> service.plan(r))
                .isInstanceOf(Nis2MeasureStateException.class);
    }

    @Test
    void plan_missingResidual_throws() {
        Nis2MeasureDto.PlanRequest r = new Nis2MeasureDto.PlanRequest(
                "M-1", Nis2MeasureCategory.CRYPTOGRAPHY, "t", null,
                USER, 2, null, null, 365,
                Set.of(), Set.of(), Set.of(), null, USER);
        assertThatThrownBy(() -> service.plan(r))
                .isInstanceOf(Nis2MeasureStateException.class);
    }

    @Test
    void edit_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        Nis2MeasureDto.View v = service.edit(ID, editReq());
        assertThat(v.title()).isEqualTo("Updated");
        verify(events).publish(any(), eq(Nis2MeasureEventPublisher.Action.EDITED));
    }

    @Test
    void edit_missingResidual_throws() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        Nis2MeasureDto.EditRequest r = new Nis2MeasureDto.EditRequest(
                "X", null, USER, 2, null, null, 365,
                Set.of(), Set.of(), Set.of(), null);
        assertThatThrownBy(() -> service.edit(ID, r))
                .isInstanceOf(Nis2MeasureStateException.class);
    }

    @Test
    void start_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        Nis2MeasureDto.View v = service.startImplementation(ID);
        assertThat(v.status()).isEqualTo(Nis2MeasureStatus.IN_PROGRESS);
        verify(events).publish(any(), eq(Nis2MeasureEventPublisher.Action.STARTED));
    }

    @Test
    void markImplemented_succeeds() {
        Nis2RiskMeasure m = stored();
        m.startImplementation(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(m));
        Nis2MeasureDto.View v = service.markImplemented(ID);
        assertThat(v.status()).isEqualTo(Nis2MeasureStatus.IMPLEMENTED);
    }

    @Test
    void verify_succeeds() {
        Nis2RiskMeasure m = stored();
        m.startImplementation(NOW);
        m.markImplemented(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(m));
        Nis2MeasureDto.View v = service.verify(ID,
                new Nis2MeasureDto.VerifyRequest(REVIEWER, NOW));
        assertThat(v.status()).isEqualTo(Nis2MeasureStatus.VERIFIED);
        assertThat(v.nextReviewDueAt()).isNotNull();
        verify(events).publish(any(), eq(Nis2MeasureEventPublisher.Action.VERIFIED));
    }

    @Test
    void review_succeeds() {
        Nis2RiskMeasure m = stored();
        m.startImplementation(NOW);
        m.markImplemented(NOW);
        m.verify(REVIEWER, NOW, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(m));
        service.review(ID, new Nis2MeasureDto.ReviewRequest(REVIEWER, NOW.plusSeconds(86400)));
        verify(events).publish(any(), eq(Nis2MeasureEventPublisher.Action.REVIEWED));
    }

    @Test
    void deprecate_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        service.deprecate(ID);
        verify(events).publish(any(), eq(Nis2MeasureEventPublisher.Action.DEPRECATED));
    }

    @Test
    void delete_onPlanned_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        service.delete(ID);
        verify(repo).delete(ID);
    }

    @Test
    void delete_onInProgress_throws() {
        Nis2RiskMeasure m = stored();
        m.startImplementation(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(m));
        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(Nis2MeasureStateException.class);
    }

    @Test
    void get_missing_404() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(Nis2MeasureNotFoundException.class);
    }

    @Test
    void get_crossTenant_404_noLeak() {
        Nis2RiskMeasure other = Nis2RiskMeasure.plan(UUID.randomUUID(), "M-X",
                Nis2MeasureCategory.CRYPTOGRAPHY, "t", null,
                USER, 2, ResidualRiskRating.LOW, null, 365,
                Set.of(), Set.of(), Set.of(), null, USER, NOW);
        other.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(Nis2MeasureNotFoundException.class);
    }

    @Test
    void list_byStatus_filters() {
        when(repo.findByTenantAndStatus(TENANT, Nis2MeasureStatus.PLANNED))
                .thenReturn(List.of(stored()));
        assertThat(service.list(Nis2MeasureStatus.PLANNED)).hasSize(1);
    }

    @Test
    void listByCategory_filters() {
        when(repo.findByTenantAndCategory(TENANT, Nis2MeasureCategory.CRYPTOGRAPHY))
                .thenReturn(List.of(stored()));
        assertThat(service.listByCategory(Nis2MeasureCategory.CRYPTOGRAPHY)).hasSize(1);
    }

    @Test
    void listByCategory_null_throws() {
        assertThatThrownBy(() -> service.listByCategory(null))
                .isInstanceOf(Nis2MeasureStateException.class);
    }

    @Test
    void getByReference_blank_throws() {
        assertThatThrownBy(() -> service.getByReference(" "))
                .isInstanceOf(Nis2MeasureStateException.class);
    }

    @Test
    void getByReference_missing_404() {
        when(repo.findByTenantAndReference(TENANT, "M-X"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByReference("M-X"))
                .isInstanceOf(Nis2MeasureNotFoundException.class);
    }

    @Test
    void reviewOverdue_capsLimit() {
        when(repo.findReviewOverdue(eq(NOW), eq(500))).thenReturn(List.of());
        service.reviewOverdue(10_000);
        verify(repo).findReviewOverdue(NOW, 500);
    }

    @Test
    void reviewOverdue_floorsLimit() {
        when(repo.findReviewOverdue(eq(NOW), eq(1))).thenReturn(List.of());
        service.reviewOverdue(0);
        verify(repo).findReviewOverdue(NOW, 1);
    }

    private Nis2MeasureDto.PlanRequest req(String ref) {
        return new Nis2MeasureDto.PlanRequest(ref,
                Nis2MeasureCategory.MFA_AND_COMMUNICATIONS,
                "MFA on admin accounts", null,
                USER, 2, ResidualRiskRating.LOW, null, 365,
                Set.of(), Set.of(), Set.of(), null, USER);
    }

    private Nis2MeasureDto.EditRequest editReq() {
        return new Nis2MeasureDto.EditRequest("Updated", null,
                USER, 3, ResidualRiskRating.MEDIUM, null, 180,
                Set.of(), Set.of(), Set.of(), null);
    }

    private Nis2RiskMeasure stored() {
        Nis2RiskMeasure m = Nis2RiskMeasure.plan(TENANT, "M-2026-001",
                Nis2MeasureCategory.CRYPTOGRAPHY, "t", null,
                USER, 2, ResidualRiskRating.LOW, null, 365,
                Set.of(), Set.of(), Set.of(), null, USER, NOW);
        m.assignId(ID);
        return m;
    }
}
