package com.openlab.qualitos.quality.notifications.infrastructure;

import com.openlab.qualitos.quality.notifications.domain.Notification;
import com.openlab.qualitos.quality.notifications.domain.NotificationType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationRepositoryAdapterTest {

    private final NotificationJpaRepository jpa = mock(NotificationJpaRepository.class);
    private final NotificationRepositoryAdapter adapter = new NotificationRepositoryAdapter(jpa);

    private final UUID tenant = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-06-04T10:00:00Z");

    private Notification domain(String recipient) {
        return new Notification(UUID.randomUUID(), tenant, recipient, NotificationType.WARNING,
                "T", "B", "/l", now, null);
    }

    @Test
    void save_roundTripsThroughMapper() {
        when(jpa.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Notification n = domain("u1");
        Notification out = adapter.save(n);
        assertThat(out.getId()).isEqualTo(n.getId());
        assertThat(out.getType()).isEqualTo(NotificationType.WARNING);
        assertThat(out.getRecipientUserId()).isEqualTo("u1");
        verify(jpa).save(any(NotificationJpaEntity.class));
    }

    @Test
    void findByIdForTenant_mapsPresent() {
        Notification n = domain(null);
        when(jpa.findByIdAndTenantId(n.getId(), tenant))
                .thenReturn(Optional.of(NotificationMapper.toEntity(n)));
        Optional<Notification> out = adapter.findByIdForTenant(n.getId(), tenant);
        assertThat(out).isPresent();
        assertThat(out.get().getRecipientUserId()).isNull();
    }

    @Test
    void findByIdForTenant_empty() {
        UUID id = UUID.randomUUID();
        when(jpa.findByIdAndTenantId(id, tenant)).thenReturn(Optional.empty());
        assertThat(adapter.findByIdForTenant(id, tenant)).isEmpty();
    }

    @Test
    void findRecent_clampsPageSizeAndMaps() {
        when(jpa.findVisible(eq(tenant), eq("u1"), any(Pageable.class)))
                .thenReturn(List.of(NotificationMapper.toEntity(domain("u1"))));
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);

        List<Notification> r0 = adapter.findRecentForRecipient(tenant, "u1", 0);   // min 1
        assertThat(r0).hasSize(1);
        List<Notification> rBig = adapter.findRecentForRecipient(tenant, "u1", 9999); // max 200

        verify(jpa, org.mockito.Mockito.atLeastOnce()).findVisible(eq(tenant), eq("u1"), page.capture());
        assertThat(page.getAllValues().get(0).getPageSize()).isEqualTo(1);
        assertThat(page.getAllValues().get(1).getPageSize()).isEqualTo(200);
        assertThat(rBig).hasSize(1);
    }

    @Test
    void countUnread_delegates() {
        when(jpa.countUnread(tenant, "u1")).thenReturn(7L);
        assertThat(adapter.countUnreadForRecipient(tenant, "u1")).isEqualTo(7L);
    }

    @Test
    void markAllRead_delegates() {
        when(jpa.markAllRead(tenant, "u1", now)).thenReturn(5);
        assertThat(adapter.markAllReadForRecipient(tenant, "u1", now)).isEqualTo(5);
    }
}
