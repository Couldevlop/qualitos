package com.openlab.qualitos.quality.gdpr.web;

import com.openlab.qualitos.quality.gdpr.application.SubjectRequestDto;
import com.openlab.qualitos.quality.gdpr.application.SubjectRequestService;
import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints RGPD (Articles 15/16/17/18/20/21). Hash de la PII côté service
 * — la PII brute n'est jamais persistée.
 */
@RestController
@RequestMapping("/api/v1/gdpr/subject-requests")
@Validated
public class SubjectRequestController {

    private final SubjectRequestService service;

    public SubjectRequestController(SubjectRequestService service) {
        this.service = service;
    }

    @GetMapping
    public List<SubjectRequestDto.View> list(
            @RequestParam(required = false) SubjectRequestStatus status) {
        return service.list(status);
    }

    @GetMapping("/search")
    public List<SubjectRequestDto.View> searchBySubject(
            @RequestParam @NotBlank @Size(max = 320) String subjectIdentifier) {
        return service.findBySubjectIdentifier(subjectIdentifier);
    }

    @GetMapping("/overdue")
    public List<SubjectRequestDto.View> overdue(
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return service.overdue(limit);
    }

    @GetMapping("/{id}")
    public SubjectRequestDto.View get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectRequestDto.View receive(
            @Valid @RequestBody SubjectRequestWebDto.ReceiveRequest req) {
        return service.receive(new SubjectRequestDto.ReceiveRequest(
                req.type(), req.subjectIdentifier(),
                req.subjectIdentifierLabel(), req.requestedByUserId()));
    }

    @PostMapping("/{id}/start")
    public SubjectRequestDto.View start(
            @PathVariable UUID id,
            @Valid @RequestBody SubjectRequestWebDto.StartProcessingRequest req) {
        return service.startProcessing(id,
                new SubjectRequestDto.StartProcessingRequest(req.handledByUserId()));
    }

    @PostMapping("/{id}/complete")
    public SubjectRequestDto.View complete(
            @PathVariable UUID id,
            @Valid @RequestBody SubjectRequestWebDto.CompleteRequest req) {
        return service.complete(id, new SubjectRequestDto.CompleteRequest(
                req.resolutionNotes(), req.evidenceUrl(), req.handledByUserId()));
    }

    @PostMapping("/{id}/reject")
    public SubjectRequestDto.View reject(
            @PathVariable UUID id,
            @Valid @RequestBody SubjectRequestWebDto.RejectRequest req) {
        return service.reject(id,
                new SubjectRequestDto.RejectRequest(req.reason(), req.handledByUserId()));
    }

    @PostMapping("/{id}/extend")
    public SubjectRequestDto.View extend(
            @PathVariable UUID id,
            @Valid @RequestBody SubjectRequestWebDto.ExtendDeadlineRequest req) {
        return service.extendDeadline(id,
                new SubjectRequestDto.ExtendDeadlineRequest(req.newDeadline()));
    }
}
