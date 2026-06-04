package com.openlab.qualitos.quality.notifications.infrastructure;

import com.openlab.qualitos.quality.notifications.domain.Notification;
import com.openlab.qualitos.quality.notifications.domain.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapter JPA du port {@link NotificationRepository}. Isolation tenant systématique. */
@Component
public class NotificationRepositoryAdapter implements NotificationRepository {

    private static final int MAX_PAGE_SIZE = 200;

    private final NotificationJpaRepository jpa;

    public NotificationRepositoryAdapter(NotificationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Notification save(Notification notification) {
        return NotificationMapper.toDomain(jpa.save(NotificationMapper.toEntity(notification)));
    }

    @Override
    public Optional<Notification> findByIdForTenant(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId).map(NotificationMapper::toDomain);
    }

    @Override
    public List<Notification> findRecentForRecipient(UUID tenantId, String userId, int limit) {
        int size = Math.min(Math.max(limit, 1), MAX_PAGE_SIZE);
        return jpa.findVisible(tenantId, userId, PageRequest.of(0, size)).stream()
                .map(NotificationMapper::toDomain)
                .toList();
    }

    @Override
    public long countUnreadForRecipient(UUID tenantId, String userId) {
        return jpa.countUnread(tenantId, userId);
    }

    @Override
    public int markAllReadForRecipient(UUID tenantId, String userId, Instant at) {
        return jpa.markAllRead(tenantId, userId, at);
    }
}
