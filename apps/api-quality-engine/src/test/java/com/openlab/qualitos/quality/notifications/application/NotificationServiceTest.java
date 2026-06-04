package com.openlab.qualitos.quality.notifications.application;

import com.openlab.qualitos.quality.notifications.domain.Notification;
import com.openlab.qualitos.quality.notifications.domain.NotificationNotFoundException;
import com.openlab.qualitos.quality.notifications.domain.NotificationRepository;
import com.openlab.qualitos.quality.notifications.domain.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private final NotificationRepository repo = mock(NotificationRepository.class);
    private final TenantProvider tenantProvider = mock(TenantProvider.class);
    private final UserProvider userProvider = mock(UserProvider.class);
    private final NotificationEventPublisher events = mock(NotificationEventPublisher.class);
    private final Instant now = Instant.parse("2026-06-04T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    private final UUID tenant = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private final String user = "user-1";

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repo, tenantProvider, userProvider, events, clock);
        when(tenantProvider.requireTenantId()).thenReturn(tenant);
        when(userProvider.requireUserId()).thenReturn(user);
    }

    private Notification persisted(String recipient, Instant readAt) {
        return new Notification(UUID.randomUUID(), tenant, recipient, NotificationType.INFO,
                "Titre", "Corps", "/lien", now, readAt);
    }

    @Test
    void create_persistsAndAudits_blankRecipientBecomesBroadcast() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var view = service.create(new NotificationDto.CreateCommand(
                "  ", NotificationType.SUCCESS, "Hello", "Body", "/x"));
        assertThat(view.read()).isFalse();
        assertThat(view.title()).isEqualTo("Hello");
        // recipient blanc → diffusion (null) ; audité
        verify(repo).save(any());
        verify(events).created(any());
    }

    @Test
    void create_keepsExplicitRecipient() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.create(new NotificationDto.CreateCommand("dest", NotificationType.INFO, "T", null, null));
        verify(repo).save(org.mockito.ArgumentMatchers.argThat(n -> "dest".equals(n.getRecipientUserId())));
    }

    @Test
    void listRecent_mapsViews() {
        when(repo.findRecentForRecipient(eq(tenant), eq(user), anyInt()))
                .thenReturn(List.of(persisted("user-1", null), persisted(null, now)));
        var views = service.listRecent(5);
        assertThat(views).hasSize(2);
        assertThat(views.get(1).read()).isTrue();
    }

    @Test
    void listRecent_clampsLimit() {
        when(repo.findRecentForRecipient(eq(tenant), eq(user), eq(20))).thenReturn(List.of());
        service.listRecent(null);          // null → défaut 20
        service.listRecent(0);             // <=0 → défaut 20
        verify(repo, org.mockito.Mockito.times(2)).findRecentForRecipient(tenant, user, 20);
        when(repo.findRecentForRecipient(eq(tenant), eq(user), eq(100))).thenReturn(List.of());
        service.listRecent(9999);          // > max → 100
        verify(repo).findRecentForRecipient(tenant, user, 100);
    }

    @Test
    void unreadCount_delegates() {
        when(repo.countUnreadForRecipient(tenant, user)).thenReturn(3L);
        assertThat(service.unreadCount().unread()).isEqualTo(3L);
    }

    @Test
    void markRead_marksVisibleNotification() {
        Notification n = persisted("user-1", null);
        when(repo.findByIdForTenant(n.getId(), tenant)).thenReturn(Optional.of(n));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var view = service.markRead(n.getId());
        assertThat(view.read()).isTrue();
        assertThat(view.readAt()).isEqualTo(now);
    }

    @Test
    void markRead_404_whenAbsent() {
        UUID id = UUID.randomUUID();
        when(repo.findByIdForTenant(id, tenant)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markRead(id)).isInstanceOf(NotificationNotFoundException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void markRead_404_whenNotVisibleToUser() {
        Notification other = persisted("autre-user", null);
        when(repo.findByIdForTenant(other.getId(), tenant)).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.markRead(other.getId()))
                .isInstanceOf(NotificationNotFoundException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void markAllRead_returnsRecomputedCount() {
        when(repo.markAllReadForRecipient(eq(tenant), eq(user), eq(now))).thenReturn(4);
        when(repo.countUnreadForRecipient(tenant, user)).thenReturn(0L);
        assertThat(service.markAllRead().unread()).isZero();
        verify(repo).markAllReadForRecipient(tenant, user, now);
    }

    @Test
    void defaultConstructor_usesNoOpPublisher() {
        var s = new NotificationService(repo, tenantProvider, userProvider, clock);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // ne doit pas lever malgré l'absence de publisher explicite
        s.create(new NotificationDto.CreateCommand(null, NotificationType.INFO, "T", null, null));
        verify(repo).save(any());
    }
}
