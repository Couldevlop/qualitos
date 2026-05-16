package com.openlab.qualitos.quality.ehs.web;

import com.openlab.qualitos.quality.ehs.application.IncidentDto;
import com.openlab.qualitos.quality.ehs.application.IncidentService;
import com.openlab.qualitos.quality.ehs.domain.IncidentRepository;
import com.openlab.qualitos.quality.ehs.domain.IncidentSeverity;
import com.openlab.qualitos.quality.ehs.domain.IncidentStatus;
import com.openlab.qualitos.quality.ehs.domain.IncidentType;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ehs/incidents")
public class IncidentController {

    private final IncidentService service;

    public IncidentController(IncidentService service) { this.service = service; }

    @GetMapping
    public Page<IncidentDto.IncidentView> list(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentType type,
            @RequestParam(required = false) IncidentSeverity severity,
            @PageableDefault(size = 50) Pageable pageable) {
        IncidentRepository.PagedResult<IncidentDto.IncidentView> r =
                service.list(status, type, severity, pageable.getPageNumber(), pageable.getPageSize());
        return new PageImpl<>(r.content(),
                PageRequest.of(r.page(), r.size()), r.totalElements());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentDto.IncidentView report(@Valid @RequestBody IncidentWebDto.ReportRequest req) {
        return service.report(new IncidentDto.ReportRequest(
                req.code(), req.title(), req.description(), req.type(), req.severity(),
                req.occurredAt(), req.location(), req.reportedBy()));
    }

    @GetMapping("/{id}")
    public IncidentDto.IncidentView get(@PathVariable UUID id) { return service.get(id); }

    @PatchMapping("/{id}")
    public IncidentDto.IncidentView edit(@PathVariable UUID id,
                                         @Valid @RequestBody IncidentWebDto.EditRequest req) {
        return service.edit(id, new IncidentDto.EditRequest(
                req.title(), req.description(), req.location(),
                req.personsInvolved(), req.severity(), req.standardsCsv()));
    }

    @PostMapping("/{id}/investigate")
    public IncidentDto.IncidentView investigate(@PathVariable UUID id,
                                                @RequestBody(required = false)
                                                IncidentWebDto.InvestigateRequest req) {
        return service.investigate(id,
                new IncidentDto.InvestigateRequest(req == null ? null : req.ownerUserId()));
    }

    @PostMapping("/{id}/mitigate")
    public IncidentDto.IncidentView mitigate(@PathVariable UUID id,
                                             @Valid @RequestBody IncidentWebDto.MitigateRequest req) {
        return service.mitigate(id, new IncidentDto.MitigateRequest(
                req.rootCause(), req.correctiveActions()));
    }

    @PostMapping("/{id}/close")
    public IncidentDto.IncidentView close(@PathVariable UUID id) { return service.close(id); }

    @PostMapping("/{id}/cancel")
    public IncidentDto.IncidentView cancel(@PathVariable UUID id) { return service.cancel(id); }

    @PostMapping("/{id}/link-capa")
    public IncidentDto.IncidentView linkCapa(@PathVariable UUID id,
                                             @Valid @RequestBody IncidentWebDto.LinkCapaRequest req) {
        return service.linkCapa(id, new IncidentDto.LinkCapaRequest(req.capaCaseId()));
    }

    @PostMapping("/{id}/link-nc")
    public IncidentDto.IncidentView linkNc(@PathVariable UUID id,
                                           @Valid @RequestBody IncidentWebDto.LinkNcRequest req) {
        return service.linkNc(id, new IncidentDto.LinkNcRequest(req.ncId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }

    @GetMapping("/statistics")
    public IncidentService.Statistics statistics() { return service.statistics(); }
}
