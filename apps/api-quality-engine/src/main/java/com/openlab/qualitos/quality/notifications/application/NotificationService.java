package com.openlab.qualitos.quality.notifications.application;

import com.openlab.qualitos.quality.notifications.domain.Notification;
import com.openlab.qualitos.quality.notifications.domain.NotificationNotFoundException;
import com.openlab.qualitos.quality.notifications.domain.NotificationRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — notifications in-app.
 *
 * <p>Isolation tenant + utilisateur (OWASP A01) : tout passe par {@link TenantProvider} /
 * {@link UserProvider} (issus du JWT) ; accéder à une notification d'un autre tenant ou
 * non visible par l'utilisateur renvoie 404 (pas de divulgation). Audit à la création
 * (OWASP A09). Horloge injectée pour la testabilité.
 */
public class NotificationService {

    private static final int MAX_LIMIT = 100;
    private static final int DEFAULT_LIMIT = 20;

    private final NotificationRepository repo;
    private final TenantProvider tenantProvider;
    private final UserProvider userProvider;
    private final NotificationEventPublisher events;
    private final Clock clock;

    public NotificationService(NotificationRepository repo, TenantProvider tenantProvider,
                               UserProvider userProvider, Clock clock) {
        this(repo, tenantProvider, userProvider, new NotificationEventPublisher.NoOp(), clock);
    }

    public NotificationService(NotificationRepository repo, TenantProvider tenantProvider,
                               UserProvider userProvider, NotificationEventPublisher events,
                               Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.userProvider = userProvider;
        this.events = events;
        this.clock = clock;
    }

    /** Crée une notification pour le tenant courant (destinataire optionnel = diffusion). */
    public NotificationDto.View create(NotificationDto.CreateCommand cmd) {
        UUID tenantId = tenantProvider.requireTenantId();
        Notification n = Notification.create(
                UUID.randomUUID(), tenantId, blankToNull(cmd.recipientUserId()),
                cmd.type(), cmd.title(), cmd.body(), cmd.link(), clock.instant());
        Notification saved = repo.save(n);
        events.created(saved);
        return NotificationDto.View.from(saved);
    }

    /** Notifications récentes visibles par l'utilisateur courant. */
    public List<NotificationDto.View> listRecent(Integer limit) {
        UUID tenantId = tenantProvider.requireTenantId();
        String userId = userProvider.requireUserId();
        int effective = clampLimit(limit);
        return repo.findRecentForRecipient(tenantId, userId, effective).stream()
                .map(NotificationDto.View::from)
                .toList();
    }

    /** Compteur de non-lues de l'utilisateur courant. */
    public NotificationDto.UnreadCount unreadCount() {
        UUID tenantId = tenantProvider.requireTenantId();
        String userId = userProvider.requireUserId();
        return new NotificationDto.UnreadCount(repo.countUnreadForRecipient(tenantId, userId));
    }

    /** Marque une notification comme lue (404 si absente ou non visible par l'utilisateur). */
    public NotificationDto.View markRead(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        String userId = userProvider.requireUserId();
        Notification n = repo.findByIdForTenant(id, tenantId)
                .filter(notif -> notif.isVisibleTo(userId))
                .orElseThrow(() -> new NotificationNotFoundException(id));
        n.markRead(clock.instant());
        return NotificationDto.View.from(repo.save(n));
    }

    /** Marque toutes les non-lues de l'utilisateur comme lues ; renvoie le nouveau compteur (0). */
    public NotificationDto.UnreadCount markAllRead() {
        UUID tenantId = tenantProvider.requireTenantId();
        String userId = userProvider.requireUserId();
        repo.markAllReadForRecipient(tenantId, userId, clock.instant());
        return new NotificationDto.UnreadCount(repo.countUnreadForRecipient(tenantId, userId));
    }

    private int clampLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
