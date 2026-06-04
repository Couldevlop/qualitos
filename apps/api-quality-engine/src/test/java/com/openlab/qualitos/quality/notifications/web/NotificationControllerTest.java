package com.openlab.qualitos.quality.notifications.web;

import com.openlab.qualitos.quality.notifications.application.NotificationDto;
import com.openlab.qualitos.quality.notifications.application.NotificationService;
import com.openlab.qualitos.quality.notifications.domain.NotificationType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationControllerTest {

    private final NotificationService service = mock(NotificationService.class);
    private final NotificationController controller = new NotificationController(service);

    private NotificationDto.View view() {
        return new NotificationDto.View(UUID.randomUUID(), NotificationType.INFO, "T", "B", "/l",
                false, Instant.parse("2026-06-04T10:00:00Z"), null);
    }

    @Test
    void recent_delegatesWithLimit() {
        when(service.listRecent(5)).thenReturn(List.of(view()));
        assertThat(controller.recent(5)).hasSize(1);
        verify(service).listRecent(5);
    }

    @Test
    void unreadCount_delegates() {
        when(service.unreadCount()).thenReturn(new NotificationDto.UnreadCount(2L));
        assertThat(controller.unreadCount().unread()).isEqualTo(2L);
    }

    @Test
    void create_mapsRequestToCommand() {
        when(service.create(any())).thenReturn(view());
        var req = new NotificationWebDto.CreateRequest("dest", NotificationType.SUCCESS, "Hi", "Body", "/x");
        controller.create(req);

        ArgumentCaptor<NotificationDto.CreateCommand> cmd =
                ArgumentCaptor.forClass(NotificationDto.CreateCommand.class);
        verify(service).create(cmd.capture());
        assertThat(cmd.getValue().recipientUserId()).isEqualTo("dest");
        assertThat(cmd.getValue().type()).isEqualTo(NotificationType.SUCCESS);
        assertThat(cmd.getValue().title()).isEqualTo("Hi");
    }

    @Test
    void markRead_delegates() {
        UUID id = UUID.randomUUID();
        when(service.markRead(id)).thenReturn(view());
        controller.markRead(id);
        verify(service).markRead(id);
    }

    @Test
    void markAllRead_delegates() {
        when(service.markAllRead()).thenReturn(new NotificationDto.UnreadCount(0L));
        assertThat(controller.markAllRead().unread()).isZero();
        verify(service).markAllRead();
    }
}
