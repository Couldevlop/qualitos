package com.openlab.qualitos.quality.auditlog;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit/events")
public class AuditEventController {

    private final AuditEventService service;

    public AuditEventController(AuditEventService service) { this.service = service; }

    @GetMapping
    public Page<AuditEventDto.EventResponse> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) UUID resourceId,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.list(action, resourceType, resourceId, actorUserId, from, to, pageable);
    }

    /**
     * C1 — Anti-forgery du journal d'audit (chaîne de hash inviolable).
     *
     * <p>Ce endpoint N'EST PAS un point d'écriture générique pour les end-users : le
     * flux normal d'audit est alimenté en interne par les {@code AuditLog*EventPublisher}
     * via {@link AuditEventService#recordForTenant}. Il est ici réservé aux rôles
     * d'administration (back-office / intégration machine).</p>
     *
     * <p>Les champs sensibles à l'attribution sont TOUJOURS surchargés côté serveur et
     * jamais lus du body :</p>
     * <ul>
     *   <li>{@code actorUserId} = {@code sub} du JWT ;</li>
     *   <li>{@code ipAddress} / {@code userAgent} = caractéristiques de la requête ;</li>
     *   <li>{@code occurredAt} / {@code recordedAt} = horloge serveur (cf. service).</li>
     * </ul>
     * Le body ne contrôle plus que la description métier (action, ressource, résumé,
     * payload). Toute valeur d'attribution fournie dans le body est ignorée.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public AuditEventDto.EventResponse record(
            @Valid @RequestBody AuditEventDto.RecordEventRequest req,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        UUID actor = parseActor(jwt);
        AuditEventDto.RecordEventRequest sanitized = new AuditEventDto.RecordEventRequest(
                // occurredAt forcé à null → le service utilise l'horloge serveur.
                null,
                req.actorType(),
                actor,
                req.action(),
                req.resourceType(),
                req.resourceId(),
                req.summary(),
                req.payloadJson(),
                clientIp(httpRequest),
                truncate(httpRequest.getHeader("User-Agent"), 500));
        return service.record(sanitized);
    }

    private static UUID parseActor(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            return null;
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            // sub non-UUID (cas rare) : on n'invente pas d'acteur falsifiable.
            return null;
        }
    }

    private static String clientIp(HttpServletRequest req) {
        String ip = req.getRemoteAddr();
        return truncate(ip, 64);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }

    @GetMapping("/{id}")
    public AuditEventDto.EventResponse get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping("/verify")
    public AuditEventDto.ChainVerification verify(
            @RequestParam long fromSeq,
            @RequestParam long toSeq) {
        return service.verifyChain(fromSeq, toSeq);
    }

    @PostMapping("/{id}/anchor")
    public AuditEventDto.EventResponse anchor(
            @PathVariable UUID id,
            @Valid @RequestBody AuditEventDto.AnchorRequest req) {
        return service.anchor(id, req);
    }
}
