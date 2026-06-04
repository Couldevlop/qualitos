package com.openlab.qualitos.quality.notifications.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {

    private final UUID id = UUID.randomUUID();
    private final UUID tenant = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-06-04T10:00:00Z");

    @Test
    void create_isUnread() {
        Notification n = Notification.create(id, tenant, "u1", NotificationType.INFO, "T", "B", "/l", now);
        assertThat(n.isRead()).isFalse();
        assertThat(n.getReadAt()).isNull();
        assertThat(n.getTitle()).isEqualTo("T");
        assertThat(n.getRecipientUserId()).isEqualTo("u1");
    }

    @Test
    void markRead_isIdempotent() {
        Notification n = Notification.create(id, tenant, null, NotificationType.ALERT, "T", null, null, now);
        n.markRead(now);
        Instant first = n.getReadAt();
        n.markRead(now.plusSeconds(60));
        assertThat(n.isRead()).isTrue();
        assertThat(n.getReadAt()).isEqualTo(first); // pas réécrit
    }

    @Test
    void isVisibleTo_broadcastVisibleToAnyone() {
        Notification n = Notification.create(id, tenant, null, NotificationType.INFO, "T", null, null, now);
        assertThat(n.isVisibleTo("anyone")).isTrue();
    }

    @Test
    void isVisibleTo_scopedOnlyToRecipient() {
        Notification n = Notification.create(id, tenant, "u1", NotificationType.INFO, "T", null, null, now);
        assertThat(n.isVisibleTo("u1")).isTrue();
        assertThat(n.isVisibleTo("u2")).isFalse();
    }

    @Test
    void constructor_rejectsMissingRequired() {
        assertThatThrownBy(() -> new Notification(null, tenant, null, NotificationType.INFO, "T", null, null, now, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Notification(id, null, null, NotificationType.INFO, "T", null, null, now, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Notification(id, tenant, null, null, "T", null, null, now, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Notification(id, tenant, null, NotificationType.INFO, "  ", null, null, now, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
