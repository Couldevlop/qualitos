package com.openlab.qualitos.quality.consent.web;

import com.openlab.qualitos.quality.consent.application.ConsentDto;
import com.openlab.qualitos.quality.consent.application.ConsentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Endpoints consentement RGPD (Art. 7).
 * - POST  /api/v1/gdpr/consents — saisie d'un consentement (PII hashée côté service).
 * - POST  /api/v1/gdpr/consents/{id}/withdraw — retrait (Art. 7§3).
 * - GET   /api/v1/gdpr/consents/{id}
 * - GET   /api/v1/gdpr/consents/search?subjectIdentifier=...
 * - GET   /api/v1/gdpr/consents/active?subjectIdentifier=...&purposeCode=...
 * - GET   /api/v1/gdpr/consents/by-purpose?purposeCode=...
 * - POST  /api/v1/gdpr/consents/expire-due — scheduler.
 */
@RestController
@RequestMapping("/api/v1/gdpr/consents")
@Validated
public class ConsentController {

    private final ConsentService service;

    public ConsentController(ConsentService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConsentDto.View grant(@Valid @RequestBody ConsentWebDto.GrantRequest req) {
        return service.grant(new ConsentDto.GrantRequest(
                req.subjectIdentifier(), req.subjectIdentifierLabel(),
                req.purposeCode(), req.purposeVersion(),
                req.source(), req.evidenceUrl(),
                req.ipAddress(), req.userAgent(),
                req.grantedByUserId(), req.expiresAt()));
    }

    @PostMapping("/{id}/withdraw")
    public ConsentDto.View withdraw(@PathVariable UUID id,
                                    @Valid @RequestBody ConsentWebDto.WithdrawRequest req) {
        return service.withdraw(id, new ConsentDto.WithdrawRequest(req.actorUserId(), req.reason()));
    }

    @GetMapping("/{id}")
    public ConsentDto.View get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping("/search")
    public List<ConsentDto.View> search(
            @RequestParam @NotBlank @Size(max = 320) String subjectIdentifier) {
        return service.findBySubject(subjectIdentifier);
    }

    @GetMapping("/active")
    public ResponseEntity<ConsentDto.View> active(
            @RequestParam @NotBlank @Size(max = 320) String subjectIdentifier,
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[a-z][a-z0-9._-]{1,63}$") String purposeCode) {
        Optional<ConsentDto.View> found = service.findActiveByPurpose(subjectIdentifier, purposeCode);
        return found.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-purpose")
    public List<ConsentDto.View> byPurpose(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[a-z][a-z0-9._-]{1,63}$") String purposeCode) {
        return service.listByPurpose(purposeCode);
    }

    @PostMapping("/expire-due")
    public Map<String, Integer> expireDue(
            @RequestParam(defaultValue = "200") @Min(1) @Max(500) int limit) {
        return Map.of("expired", service.expireDue(limit));
    }
}
