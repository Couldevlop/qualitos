package com.openlab.qualitos.quality.gdpr.application;

import com.openlab.qualitos.quality.gdpr.domain.DataSubjectRequest;
import com.openlab.qualitos.quality.gdpr.domain.DataSubjectRequestRepository;
import com.openlab.qualitos.quality.gdpr.domain.SubjectIdentifierHasher;
import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestNotFoundException;
import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestStateException;
import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestStatus;
import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestType;
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
class SubjectRequestServiceTest {

    @Mock DataSubjectRequestRepository repo;
    @Mock SubjectIdentifierHasher hasher;
    @Mock TenantProvider tenantProvider;
    @Mock SubjectRequestEventPublisher events;

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID HANDLER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final String HASH = "h".repeat(64);

    SubjectRequestService service;

    @BeforeEach
    void setup() {
        service = new SubjectRequestService(repo, hasher, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(hasher.hash(anyString())).thenReturn(HASH);
        when(repo.save(any())).thenAnswer(inv -> {
            DataSubjectRequest r = inv.getArgument(0);
            r.assignId(ID);
            return r;
        });
    }

    @Test
    void receive_hashesIdentifier_andPersists() {
        SubjectRequestDto.View v = service.receive(new SubjectRequestDto.ReceiveRequest(
                SubjectRequestType.ACCESS, "Jane@Example.COM", "j***@e.com", USER));
        verify(hasher).hash("jane@example.com"); // normalisé : trim + lowercase
        verify(repo).save(any());
        verify(events).publish(any(), eq(SubjectRequestEventPublisher.Action.RECEIVED));
        assertThat(v.subjectIdentifierHash()).isEqualTo(HASH);
        assertThat(v.status()).isEqualTo(SubjectRequestStatus.RECEIVED);
    }

    @Test
    void receive_blankIdentifier_rejected() {
        assertThatThrownBy(() -> service.receive(new SubjectRequestDto.ReceiveRequest(
                SubjectRequestType.ACCESS, "  ", null, USER)))
                .isInstanceOf(SubjectRequestStateException.class);
    }

    @Test
    void receive_nullType_rejected() {
        assertThatThrownBy(() -> service.receive(new SubjectRequestDto.ReceiveRequest(
                null, "x", null, USER)))
                .isInstanceOf(SubjectRequestStateException.class);
    }

    @Test
    void get_missing_throws404() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(SubjectRequestNotFoundException.class);
    }

    @Test
    void get_crossTenant_throws404_noLeak() {
        DataSubjectRequest other = DataSubjectRequest.receive(
                UUID.randomUUID(), SubjectRequestType.ACCESS, HASH, null, USER, NOW);
        other.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(SubjectRequestNotFoundException.class);
    }

    @Test
    void startProcessing_movesToInProgress_andPublishes() {
        DataSubjectRequest stored = freshStored();
        when(repo.findById(ID)).thenReturn(Optional.of(stored));
        SubjectRequestDto.View v = service.startProcessing(ID,
                new SubjectRequestDto.StartProcessingRequest(HANDLER));
        assertThat(v.status()).isEqualTo(SubjectRequestStatus.IN_PROGRESS);
        verify(events).publish(any(), eq(SubjectRequestEventPublisher.Action.IN_PROGRESS));
    }

    @Test
    void complete_movesToCompleted_andPublishes() {
        DataSubjectRequest stored = freshStored();
        stored.startProcessing(HANDLER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(stored));
        SubjectRequestDto.View v = service.complete(ID,
                new SubjectRequestDto.CompleteRequest("Resolved", "url", HANDLER));
        assertThat(v.status()).isEqualTo(SubjectRequestStatus.COMPLETED);
        verify(events).publish(any(), eq(SubjectRequestEventPublisher.Action.COMPLETED));
    }

    @Test
    void reject_movesToRejected_andPublishes() {
        DataSubjectRequest stored = freshStored();
        when(repo.findById(ID)).thenReturn(Optional.of(stored));
        SubjectRequestDto.View v = service.reject(ID,
                new SubjectRequestDto.RejectRequest("Out of scope", HANDLER));
        assertThat(v.status()).isEqualTo(SubjectRequestStatus.REJECTED);
        verify(events).publish(any(), eq(SubjectRequestEventPublisher.Action.REJECTED));
    }

    @Test
    void extend_updatesDeadline_andPublishes() {
        DataSubjectRequest stored = freshStored();
        when(repo.findById(ID)).thenReturn(Optional.of(stored));
        Instant newDeadline = NOW.plusSeconds(86400L * 60);
        SubjectRequestDto.View v = service.extendDeadline(ID,
                new SubjectRequestDto.ExtendDeadlineRequest(newDeadline));
        assertThat(v.extended()).isTrue();
        assertThat(v.deadlineAt()).isEqualTo(newDeadline);
        verify(events).publish(any(), eq(SubjectRequestEventPublisher.Action.EXTENDED));
    }

    @Test
    void findBySubjectIdentifier_hashesBeforeLookup() {
        when(repo.findByTenantIdAndSubjectIdentifierHash(TENANT, HASH)).thenReturn(List.of());
        service.findBySubjectIdentifier("Jane@example.com");
        verify(hasher).hash("jane@example.com");
        verify(repo).findByTenantIdAndSubjectIdentifierHash(TENANT, HASH);
    }

    @Test
    void findBySubjectIdentifier_blank_returnsEmpty_noLookup() {
        assertThat(service.findBySubjectIdentifier("  ")).isEmpty();
        verify(repo, never()).findByTenantIdAndSubjectIdentifierHash(any(), any());
    }

    @Test
    void list_withStatus_filters() {
        when(repo.findByTenantIdAndStatus(TENANT, SubjectRequestStatus.IN_PROGRESS))
                .thenReturn(List.of(freshStored()));
        List<SubjectRequestDto.View> out = service.list(SubjectRequestStatus.IN_PROGRESS);
        assertThat(out).hasSize(1);
    }

    @Test
    void list_nullStatus_returnsAll() {
        when(repo.findByTenantId(TENANT)).thenReturn(List.of(freshStored(), freshStored()));
        assertThat(service.list(null)).hasSize(2);
    }

    @Test
    void overdue_capsLimit_atUpperBound() {
        when(repo.findOverdue(eq(NOW), eq(500))).thenReturn(List.of());
        service.overdue(99999);
        verify(repo).findOverdue(NOW, 500);
    }

    @Test
    void overdue_flooredAt1() {
        when(repo.findOverdue(eq(NOW), eq(1))).thenReturn(List.of());
        service.overdue(0);
        verify(repo).findOverdue(NOW, 1);
    }

    private DataSubjectRequest freshStored() {
        DataSubjectRequest r = DataSubjectRequest.receive(
                TENANT, SubjectRequestType.ACCESS, HASH, "j***@e.com", USER, NOW);
        r.assignId(ID);
        return r;
    }
}
