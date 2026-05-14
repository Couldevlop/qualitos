package com.openlab.qualitos.quality.fives;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fives")
public class FiveSController {

    private final FiveSService service;

    public FiveSController(FiveSService service) {
        this.service = service;
    }

    @GetMapping("/audits")
    public Page<FiveSDto.AuditResponse> list(
            @RequestParam(required = false) FiveSAuditStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.findAll(status, pageable);
    }

    @PostMapping("/audits")
    @ResponseStatus(HttpStatus.CREATED)
    public FiveSDto.AuditResponse create(@Valid @RequestBody FiveSDto.CreateAuditRequest request) {
        return service.createAudit(request);
    }

    @GetMapping("/audits/{id}")
    public FiveSDto.AuditResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PatchMapping("/audits/{id}")
    public FiveSDto.AuditResponse update(@PathVariable UUID id,
                                         @Valid @RequestBody FiveSDto.UpdateAuditRequest request) {
        return service.updateAudit(id, request);
    }

    @PatchMapping("/audits/{id}/start")
    public FiveSDto.AuditResponse start(@PathVariable UUID id) { return service.startAudit(id); }

    @PatchMapping("/audits/{id}/complete")
    public FiveSDto.AuditResponse complete(@PathVariable UUID id) { return service.completeAudit(id); }

    @PatchMapping("/audits/{id}/cancel")
    public FiveSDto.AuditResponse cancel(@PathVariable UUID id) { return service.cancelAudit(id); }

    @PutMapping("/audits/{id}/score")
    public FiveSDto.ItemResponse score(@PathVariable UUID id,
                                       @Valid @RequestBody FiveSDto.ScoreRequest request) {
        return service.scorePillar(id, request);
    }

    @DeleteMapping("/audits/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.deleteAudit(id); }
}
