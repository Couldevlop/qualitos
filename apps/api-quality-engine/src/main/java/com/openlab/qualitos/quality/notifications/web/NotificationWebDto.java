package com.openlab.qualitos.quality.notifications.web;

import com.openlab.qualitos.quality.notifications.application.NotificationDto;
import com.openlab.qualitos.quality.notifications.domain.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** DTO web des notifications (validation Jakarta — OWASP A03). */
public final class NotificationWebDto {

    private NotificationWebDto() {}

    public record CreateRequest(
            @Size(max = 128) String recipientUserId,
            @NotNull NotificationType type,
            @NotBlank @Size(max = 255) String title,
            @Size(max = 5000) String body,
            @Size(max = 2048) String link
    ) {
        public NotificationDto.CreateCommand toCommand() {
            return new NotificationDto.CreateCommand(recipientUserId, type, title, body, link);
        }
    }
}
