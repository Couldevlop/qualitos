package com.openlab.qualitos.quality.privacynotices.web;

import com.openlab.qualitos.quality.privacynotices.application.PrivacyNoticeDto;
import com.openlab.qualitos.quality.privacynotices.application.PrivacyNoticeService;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNoticeStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Endpoints — mentions d'information RGPD (Art. 13/14).
 */
@RestController
@RequestMapping("/api/v1/gdpr/privacy-notices")
@Validated
public class PrivacyNoticeController {

    private final PrivacyNoticeService service;

    public PrivacyNoticeController(PrivacyNoticeService service) {
        this.service = service;
    }

    @GetMapping
    public List<PrivacyNoticeDto.View> list(
            @RequestParam(required = false) PrivacyNoticeStatus status) {
        return service.list(status);
    }

    @GetMapping("/{id}")
    public PrivacyNoticeDto.View get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/published")
    public ResponseEntity<PrivacyNoticeDto.View> findPublished(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$") String reference,
            @RequestParam @NotBlank @Size(min = 2, max = 2)
            @Pattern(regexp = "^[a-z]{2}$") String language) {
        Optional<PrivacyNoticeDto.View> found = service.findPublished(reference, language);
        return found.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/versions")
    public List<PrivacyNoticeDto.View> versions(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$") String reference) {
        return service.versions(reference);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrivacyNoticeDto.View create(@Valid @RequestBody PrivacyNoticeWebDto.CreateRequest req) {
        return service.create(new PrivacyNoticeDto.CreateRequest(
                req.reference(), req.version(), req.language(),
                req.title(), req.summary(), req.contentMarkdown(),
                req.linkedProcessingActivityIds(), req.publishUrl(),
                req.contactName(), req.contactEmail(), req.createdByUserId()));
    }

    @PutMapping("/{id}")
    public PrivacyNoticeDto.View edit(@PathVariable UUID id,
                                      @Valid @RequestBody PrivacyNoticeWebDto.EditRequest req) {
        return service.edit(id, new PrivacyNoticeDto.EditRequest(
                req.title(), req.summary(), req.contentMarkdown(),
                req.linkedProcessingActivityIds(), req.publishUrl(),
                req.contactName(), req.contactEmail()));
    }

    @PostMapping("/{id}/publish")
    public PrivacyNoticeDto.View publish(@PathVariable UUID id,
                                         @Valid @RequestBody PrivacyNoticeWebDto.PublishRequest req) {
        return service.publish(id, new PrivacyNoticeDto.PublishRequest(req.publishedByUserId()));
    }

    @PostMapping("/{id}/archive")
    public PrivacyNoticeDto.View archive(@PathVariable UUID id) {
        return service.archive(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
