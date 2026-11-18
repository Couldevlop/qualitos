package com.openlab.qualitos.quality.notifications.web;

import com.openlab.qualitos.quality.notifications.domain.NotificationNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ce banc vivait dans {@code GlobalExceptionHandlerTest}. Il a suivi son gestionnaire
 * quand le socle a cessé de porter les refus des modules (ADR 0128).
 */
class NotificationExceptionHandlerTest {

    private final NotificationExceptionHandler handler = new NotificationExceptionHandler();

    @Test
    void notificationNotFound_mapsTo404() {
        ProblemDetail pd = handler.handleNotificationNotFound(
                new NotificationNotFoundException(UUID.randomUUID()));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getTitle()).isEqualTo("Notification Not Found");
    }
}
