package com.openlab.qualitos.quality.auditlog;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuditEventDto.EventResponse record(
            @Valid @RequestBody AuditEventDto.RecordEventRequest req) {
        return service.record(req);
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
