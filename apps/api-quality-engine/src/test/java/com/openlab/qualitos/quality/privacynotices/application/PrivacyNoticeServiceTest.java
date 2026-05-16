package com.openlab.qualitos.quality.privacynotices.application;

import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNotice;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNoticeNotFoundException;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNoticeRepository;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNoticeStateException;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNoticeStatus;
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
class PrivacyNoticeServiceTest {

    @Mock PrivacyNoticeRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock PrivacyNoticeEventPublisher events;

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();

    PrivacyNoticeService service;

    @BeforeEach
    void setup() {
        service = new PrivacyNoticeService(repo, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.existsByTenantAndReferenceAndVersionAndLanguage(any(), any(), any(), any()))
                .thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            PrivacyNotice n = inv.getArgument(0);
            if (n.getId() == null) n.assignId(ID);
            return n;
        });
    }

    @Test
    void create_persistsDraft_andPublishes() {
        PrivacyNoticeDto.View v = service.create(req("PN-CUSTOMERS", "1.0", "fr"));
        verify(events).publish(any(), eq(PrivacyNoticeEventPublisher.Action.CREATED));
        assertThat(v.status()).isEqualTo(PrivacyNoticeStatus.DRAFT);
    }

    @Test
    void create_duplicateVersionLanguage_throws() {
        when(repo.existsByTenantAndReferenceAndVersionAndLanguage(
                TENANT, "PN-DUP", "1.0", "fr")).thenReturn(true);
        assertThatThrownBy(() -> service.create(req("PN-DUP", "1.0", "fr")))
                .isInstanceOf(PrivacyNoticeStateException.class);
    }

    @Test
    void create_missingActor_throws() {
        PrivacyNoticeDto.CreateRequest r = new PrivacyNoticeDto.CreateRequest(
                "PN-1", "1.0", "fr", "T", "s", "c",
                Set.of(), null, null, null, null /* createdByUserId */);
        assertThatThrownBy(() -> service.create(r))
                .isInstanceOf(PrivacyNoticeStateException.class);
    }

    @Test
    void edit_onDraft_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        PrivacyNoticeDto.View v = service.edit(ID, new PrivacyNoticeDto.EditRequest(
                "Updated", "s", "c", Set.of(), null, null, null));
        assertThat(v.title()).isEqualTo("Updated");
        verify(events).publish(any(), eq(PrivacyNoticeEventPublisher.Action.EDITED));
    }

    @Test
    void edit_onPublished_throws() {
        PrivacyNotice n = readyToPublish();
        n.publish(USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> service.edit(ID, new PrivacyNoticeDto.EditRequest(
                "X", "s", "c", Set.of(), null, null, null)))
                .isInstanceOf(PrivacyNoticeStateException.class);
    }

    @Test
    void publish_autoArchivesPreviousActive() {
        PrivacyNotice draft = readyToPublish();
        UUID oldId = UUID.randomUUID();
        PrivacyNotice oldPub = PrivacyNotice.draft(TENANT, "PN-CUSTOMERS", "0.9", "fr",
                "Old", "old summary", "old content",
                Set.of(), null, null, null, USER, NOW.minusSeconds(86400));
        oldPub.assignId(oldId);
        oldPub.publish(USER, NOW.minusSeconds(86400));
        when(repo.findById(ID)).thenReturn(Optional.of(draft));
        when(repo.findPublishedByReferenceAndLanguage(TENANT, "PN-CUSTOMERS", "fr"))
                .thenReturn(Optional.of(oldPub));

        PrivacyNoticeDto.View v = service.publish(ID,
                new PrivacyNoticeDto.PublishRequest(USER));
        assertThat(v.status()).isEqualTo(PrivacyNoticeStatus.PUBLISHED);
        verify(events).publish(any(), eq(PrivacyNoticeEventPublisher.Action.ARCHIVED));
        verify(events).publish(any(), eq(PrivacyNoticeEventPublisher.Action.PUBLISHED));
    }

    @Test
    void publish_noPreviousActive_succeeds() {
        PrivacyNotice n = readyToPublish();
        when(repo.findById(ID)).thenReturn(Optional.of(n));
        when(repo.findPublishedByReferenceAndLanguage(TENANT, "PN-CUSTOMERS", "fr"))
                .thenReturn(Optional.empty());
        service.publish(ID, new PrivacyNoticeDto.PublishRequest(USER));
        verify(events, never()).publish(any(), eq(PrivacyNoticeEventPublisher.Action.ARCHIVED));
    }

    @Test
    void publish_notDraft_throws() {
        PrivacyNotice n = readyToPublish();
        n.publish(USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> service.publish(ID,
                new PrivacyNoticeDto.PublishRequest(USER)))
                .isInstanceOf(PrivacyNoticeStateException.class);
    }

    @Test
    void archive_fromPublished_succeeds() {
        PrivacyNotice n = readyToPublish();
        n.publish(USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(n));
        PrivacyNoticeDto.View v = service.archive(ID);
        assertThat(v.status()).isEqualTo(PrivacyNoticeStatus.ARCHIVED);
    }

    @Test
    void delete_onDraft_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        service.delete(ID);
        verify(repo).delete(ID);
        verify(events).publish(any(), eq(PrivacyNoticeEventPublisher.Action.DELETED));
    }

    @Test
    void delete_onPublished_throws() {
        PrivacyNotice n = readyToPublish();
        n.publish(USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(PrivacyNoticeStateException.class);
        verify(repo, never()).delete(any());
    }

    @Test
    void get_missing_404() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(PrivacyNoticeNotFoundException.class);
    }

    @Test
    void get_crossTenant_404_noLeak() {
        PrivacyNotice other = PrivacyNotice.draft(UUID.randomUUID(),
                "PN-X", "1.0", "fr", "T", "s", "c",
                Set.of(), null, null, null, USER, NOW);
        other.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(PrivacyNoticeNotFoundException.class);
    }

    @Test
    void list_byStatus_filters() {
        when(repo.findByTenantAndStatus(TENANT, PrivacyNoticeStatus.PUBLISHED))
                .thenReturn(List.of(stored()));
        assertThat(service.list(PrivacyNoticeStatus.PUBLISHED)).hasSize(1);
    }

    @Test
    void list_nullStatus_returnsAll() {
        when(repo.findByTenant(TENANT)).thenReturn(List.of(stored(), stored()));
        assertThat(service.list(null)).hasSize(2);
    }

    @Test
    void findPublished_blankParams_returnsEmpty() {
        assertThat(service.findPublished(null, "fr")).isEmpty();
        assertThat(service.findPublished("PN-1", " ")).isEmpty();
        verify(repo, never()).findPublishedByReferenceAndLanguage(any(), any(), any());
    }

    @Test
    void findPublished_returnsView() {
        when(repo.findPublishedByReferenceAndLanguage(TENANT, "PN-CUSTOMERS", "fr"))
                .thenReturn(Optional.of(stored()));
        assertThat(service.findPublished("PN-CUSTOMERS", "fr")).isPresent();
    }

    @Test
    void versions_blankReference_returnsEmpty() {
        assertThat(service.versions(" ")).isEmpty();
        verify(repo, never()).findVersionsByReference(any(), any());
    }

    @Test
    void versions_returnsAll() {
        when(repo.findVersionsByReference(TENANT, "PN-CUSTOMERS"))
                .thenReturn(List.of(stored(), stored()));
        assertThat(service.versions("PN-CUSTOMERS")).hasSize(2);
    }

    private PrivacyNoticeDto.CreateRequest req(String ref, String ver, String lang) {
        return new PrivacyNoticeDto.CreateRequest(ref, ver, lang,
                "Mention", "résumé", "contenu",
                Set.of(), null, null, null, USER);
    }

    private PrivacyNotice stored() {
        PrivacyNotice n = PrivacyNotice.draft(TENANT, "PN-CUSTOMERS", "1.0", "fr",
                "Mention", "summary", "content",
                Set.of(), null, null, null, USER, NOW);
        n.assignId(ID);
        return n;
    }

    private PrivacyNotice readyToPublish() {
        PrivacyNotice n = PrivacyNotice.draft(TENANT, "PN-CUSTOMERS", "1.0", "fr",
                "Mention", "résumé valide", "contenu complet",
                Set.of(), null, null, null, USER, NOW);
        n.assignId(ID);
        return n;
    }
}
