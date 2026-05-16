package com.openlab.qualitos.quality.retention.application;

import com.openlab.qualitos.quality.retention.domain.RetentionRule;
import com.openlab.qualitos.quality.retention.domain.RetentionRuleNotFoundException;
import com.openlab.qualitos.quality.retention.domain.RetentionRuleRepository;
import com.openlab.qualitos.quality.retention.domain.RetentionRuleStateException;
import com.openlab.qualitos.quality.retention.domain.RetentionRuleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Duration;
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
class RetentionRuleServiceTest {

    @Mock RetentionRuleRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock RetentionRuleEventPublisher events;

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();

    RetentionRuleService service;

    @BeforeEach
    void setup() {
        service = new RetentionRuleService(repo, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.save(any())).thenAnswer(inv -> {
            RetentionRule r = inv.getArgument(0);
            if (r.getId() == null) r.assignId(ID);
            return r;
        });
    }

    @Test
    void create_persistsDraft_andPublishes() {
        RetentionRuleDto.View v = service.create(req());
        verify(events).publish(any(), eq(RetentionRuleEventPublisher.Action.CREATED));
        assertThat(v.status()).isEqualTo(RetentionRuleStatus.DRAFT);
        assertThat(v.dataCategoryCode()).isEqualTo("marketing");
    }

    @Test
    void create_missingActor_throws() {
        assertThatThrownBy(() -> service.create(new RetentionRuleDto.CreateRequest(
                "marketing", null, Duration.ofDays(30), "basis", null, null)))
                .isInstanceOf(RetentionRuleStateException.class);
    }

    @Test
    void edit_onDraft_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(draftStored()));
        RetentionRuleDto.View v = service.edit(ID, new RetentionRuleDto.EditRequest(
                "Updated", Duration.ofDays(60), "new basis", null));
        assertThat(v.retentionPeriod()).isEqualTo(Duration.ofDays(60));
        verify(events).publish(any(), eq(RetentionRuleEventPublisher.Action.EDITED));
    }

    @Test
    void edit_onActive_throws() {
        RetentionRule r = draftStored();
        r.activate(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.edit(ID, new RetentionRuleDto.EditRequest(
                null, Duration.ofDays(60), "x", null)))
                .isInstanceOf(RetentionRuleStateException.class);
    }

    @Test
    void activate_archivesPreviousActiveForSameCategory() {
        RetentionRule draft = draftStored();
        UUID oldId = UUID.randomUUID();
        RetentionRule oldActive = RetentionRule.draft(TENANT, "marketing", null,
                Duration.ofDays(180), "old basis", null, USER, NOW.minusSeconds(86400));
        oldActive.assignId(oldId);
        oldActive.activate(NOW.minusSeconds(86400));

        when(repo.findById(ID)).thenReturn(Optional.of(draft));
        when(repo.findActiveByCategory(TENANT, "marketing"))
                .thenReturn(Optional.of(oldActive));

        RetentionRuleDto.View v = service.activate(ID);
        assertThat(v.status()).isEqualTo(RetentionRuleStatus.ACTIVE);
        verify(events).publish(any(), eq(RetentionRuleEventPublisher.Action.ARCHIVED));
        verify(events).publish(any(), eq(RetentionRuleEventPublisher.Action.ACTIVATED));
    }

    @Test
    void activate_noPreviousActive_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(draftStored()));
        when(repo.findActiveByCategory(TENANT, "marketing")).thenReturn(Optional.empty());
        RetentionRuleDto.View v = service.activate(ID);
        assertThat(v.status()).isEqualTo(RetentionRuleStatus.ACTIVE);
        verify(events, never()).publish(any(), eq(RetentionRuleEventPublisher.Action.ARCHIVED));
    }

    @Test
    void activate_alreadyActive_throws() {
        RetentionRule r = draftStored();
        r.activate(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.activate(ID))
                .isInstanceOf(RetentionRuleStateException.class);
    }

    @Test
    void archive_onActive_succeeds() {
        RetentionRule r = draftStored();
        r.activate(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(r));
        RetentionRuleDto.View v = service.archive(ID);
        assertThat(v.status()).isEqualTo(RetentionRuleStatus.ARCHIVED);
    }

    @Test
    void delete_onDraft_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(draftStored()));
        service.delete(ID);
        verify(repo).delete(ID);
        verify(events).publish(any(), eq(RetentionRuleEventPublisher.Action.DELETED));
    }

    @Test
    void delete_onActive_throws() {
        RetentionRule r = draftStored();
        r.activate(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(RetentionRuleStateException.class);
        verify(repo, never()).delete(any());
    }

    @Test
    void get_missing_404() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(RetentionRuleNotFoundException.class);
    }

    @Test
    void get_crossTenant_404_noLeak() {
        RetentionRule r = RetentionRule.draft(UUID.randomUUID(), "marketing", null,
                Duration.ofDays(30), "x", null, USER, NOW);
        r.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(RetentionRuleNotFoundException.class);
    }

    @Test
    void list_withStatus_filters() {
        when(repo.findByTenantAndStatus(TENANT, RetentionRuleStatus.ACTIVE))
                .thenReturn(List.of(draftStored()));
        assertThat(service.list(RetentionRuleStatus.ACTIVE)).hasSize(1);
    }

    @Test
    void list_nullStatus_returnsAll() {
        when(repo.findByTenant(TENANT)).thenReturn(List.of(draftStored(), draftStored()));
        assertThat(service.list(null)).hasSize(2);
    }

    @Test
    void evaluateErasure_withActiveRule_returnsDateAndDueFlag() {
        RetentionRule r = draftStored();
        r.activate(NOW.minusSeconds(86400L * 60));
        when(repo.findActiveByCategory(TENANT, "marketing")).thenReturn(Optional.of(r));
        Instant created = NOW.minusSeconds(86400L * 60); // 60 jours en arrière
        Optional<RetentionRuleDto.ErasureEvaluation> v =
                service.evaluateErasure("marketing", created);
        assertThat(v).isPresent();
        // Période = 30 jours, donc erasureAt = created + 30j → bien avant NOW → dueNow
        assertThat(v.get().dueNow()).isTrue();
        assertThat(v.get().erasureAt()).isEqualTo(created.plus(Duration.ofDays(30)));
    }

    @Test
    void evaluateErasure_notYetDue_returnsFalse() {
        RetentionRule r = draftStored();
        r.activate(NOW);
        when(repo.findActiveByCategory(TENANT, "marketing")).thenReturn(Optional.of(r));
        Optional<RetentionRuleDto.ErasureEvaluation> v =
                service.evaluateErasure("marketing", NOW);
        assertThat(v).isPresent();
        assertThat(v.get().dueNow()).isFalse();
    }

    @Test
    void evaluateErasure_noActiveRule_empty() {
        when(repo.findActiveByCategory(TENANT, "marketing")).thenReturn(Optional.empty());
        assertThat(service.evaluateErasure("marketing", NOW)).isEmpty();
    }

    @Test
    void evaluateErasure_blankInputs_empty() {
        assertThat(service.evaluateErasure(null, NOW)).isEmpty();
        assertThat(service.evaluateErasure(" ", NOW)).isEmpty();
        assertThat(service.evaluateErasure("marketing", null)).isEmpty();
        verify(repo, never()).findActiveByCategory(any(), any());
    }

    private RetentionRuleDto.CreateRequest req() {
        return new RetentionRuleDto.CreateRequest(
                "marketing", "Marketing data", Duration.ofDays(30),
                "Consent (Art. 6.1.a)", null, USER);
    }

    private RetentionRule draftStored() {
        RetentionRule r = RetentionRule.draft(TENANT, "marketing", "Marketing data",
                Duration.ofDays(30), "basis", null, USER, NOW);
        r.assignId(ID);
        return r;
    }
}
