package com.openlab.qualitos.quality.privacynotices.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class PrivacyNoticeTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID P = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void draft_validInputs_createsDraft() {
        PrivacyNotice n = draft();
        assertThat(n.isDraft()).isTrue();
        assertThat(n.getStatus()).isEqualTo(PrivacyNoticeStatus.DRAFT);
        assertThat(n.getLanguage()).isEqualTo("fr");
    }

    @Test
    void draft_invalidReference_throws() {
        assertThatThrownBy(() -> PrivacyNotice.draft(T, "lowercase", "1.0", "fr",
                "Title", "summary", "content", Set.of(), null, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_invalidVersion_throws() {
        assertThatThrownBy(() -> PrivacyNotice.draft(T, "PN-1", "v 1!", "fr",
                "T", "s", "c", Set.of(), null, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_invalidLanguage_throws() {
        assertThatThrownBy(() -> PrivacyNotice.draft(T, "PN-1", "1.0", "FR",
                "T", "s", "c", Set.of(), null, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PrivacyNotice.draft(T, "PN-1", "1.0", "fra",
                "T", "s", "c", Set.of(), null, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_invalidUrl_throws() {
        assertThatThrownBy(() -> PrivacyNotice.draft(T, "PN-1", "1.0", "fr",
                "T", "s", "c", Set.of(), "not-a-url", null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_validHttpsUrl_ok() {
        PrivacyNotice n = PrivacyNotice.draft(T, "PN-1", "1.0", "fr",
                "T", "s", "c", Set.of(), "https://example.com/privacy",
                null, null, U, NOW);
        assertThat(n.getPublishUrl()).isEqualTo("https://example.com/privacy");
    }

    @Test
    void draft_blankUrl_treatedAsNull() {
        PrivacyNotice n = PrivacyNotice.draft(T, "PN-1", "1.0", "fr",
                "T", "s", "c", Set.of(), "  ", null, null, U, NOW);
        assertThat(n.getPublishUrl()).isNull();
    }

    @Test
    void editDraft_succeeds() {
        PrivacyNotice n = draft();
        n.editDraft("Updated", "new summary", "new content",
                Set.of(P), "https://x.com/p", "DPO", "dpo@x.com",
                NOW.plusSeconds(60));
        assertThat(n.getTitle()).isEqualTo("Updated");
        assertThat(n.getContactName()).isEqualTo("DPO");
    }

    @Test
    void editDraft_whenPublished_rejected() {
        PrivacyNotice n = draftReadyToPublish();
        n.publish(U, NOW);
        assertThatThrownBy(() -> n.editDraft("X", "s", "c", Set.of(), null, null, null,
                NOW.plusSeconds(60)))
                .isInstanceOf(PrivacyNoticeStateException.class);
    }

    @Test
    void publish_requiresSummaryAndContent() {
        PrivacyNotice n = draft();
        // summary/content non renseignés en draft()
        n.editDraft("T", null, "c", Set.of(), null, null, null, NOW);
        assertThatThrownBy(() -> n.publish(U, NOW.plusSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class);

        PrivacyNotice n2 = draft();
        n2.editDraft("T", "s", null, Set.of(), null, null, null, NOW);
        assertThatThrownBy(() -> n2.publish(U, NOW.plusSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void publish_requiresPublishedByUserId() {
        PrivacyNotice n = draftReadyToPublish();
        assertThatThrownBy(() -> n.publish(null, NOW))
                .isInstanceOf(PrivacyNoticeStateException.class);
    }

    @Test
    void publish_fromDraft_moves() {
        PrivacyNotice n = draftReadyToPublish();
        n.publish(U, NOW.plusSeconds(60));
        assertThat(n.isPublished()).isTrue();
        assertThat(n.getEffectiveFrom()).isEqualTo(NOW.plusSeconds(60));
        assertThat(n.getPublishedByUserId()).isEqualTo(U);
    }

    @Test
    void publish_alreadyPublished_rejected() {
        PrivacyNotice n = draftReadyToPublish();
        n.publish(U, NOW);
        assertThatThrownBy(() -> n.publish(U, NOW.plusSeconds(60)))
                .isInstanceOf(PrivacyNoticeStateException.class);
    }

    @Test
    void archive_fromPublished_succeeds() {
        PrivacyNotice n = draftReadyToPublish();
        n.publish(U, NOW);
        n.archive(NOW.plusSeconds(86400));
        assertThat(n.isArchived()).isTrue();
        assertThat(n.getEffectiveTo()).isEqualTo(NOW.plusSeconds(86400));
    }

    @Test
    void archive_fromDraft_rejected() {
        PrivacyNotice n = draft();
        assertThatThrownBy(() -> n.archive(NOW))
                .isInstanceOf(PrivacyNoticeStateException.class);
    }

    private PrivacyNotice draft() {
        return PrivacyNotice.draft(T, "PN-CUSTOMERS", "1.0", "fr",
                "Mention clients", null, null, Set.of(P),
                null, null, null, U, NOW);
    }

    private PrivacyNotice draftReadyToPublish() {
        return PrivacyNotice.draft(T, "PN-CUSTOMERS", "1.0", "fr",
                "Mention clients",
                "Résumé court",
                "# Mention\nContenu complet en markdown",
                Set.of(P), "https://example.com/privacy",
                "DPO", "dpo@example.com", U, NOW);
    }
}
