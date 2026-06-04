package com.openlab.qualitos.quality.notifications.web;

import com.openlab.qualitos.quality.notifications.application.NotificationDto;
import com.openlab.qualitos.quality.notifications.application.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Notifications in-app du tenant/utilisateur courant. Tenant + utilisateur sont résolus
 * depuis le contexte de sécurité (JWT), jamais depuis la requête (OWASP A01).
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificationDto.View> recent(@RequestParam(required = false) Integer limit) {
        return service.listRecent(limit);
    }

    @GetMapping("/unread-count")
    public NotificationDto.UnreadCount unreadCount() {
        return service.unreadCount();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationDto.View create(@Valid @RequestBody NotificationWebDto.CreateRequest req) {
        return service.create(req.toCommand());
    }

    @PostMapping("/{id}/read")
    public NotificationDto.View markRead(@PathVariable UUID id) {
        return service.markRead(id);
    }

    @PostMapping("/read-all")
    public NotificationDto.UnreadCount markAllRead() {
        return service.markAllRead();
    }
}
