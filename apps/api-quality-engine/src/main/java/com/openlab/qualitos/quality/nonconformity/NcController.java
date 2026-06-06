package com.openlab.qualitos.quality.nonconformity;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/nc")
public class NcController {

    private final NcService service;

    public NcController(NcService service) {
        this.service = service;
    }

    @GetMapping
    public Page<NcDto.Response> list(
            @RequestParam(required = false) NcStatus status,
            @RequestParam(required = false) NcSeverity severity,
            @RequestParam(required = false) NcCategory category,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.findAll(status, severity, category, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NcDto.Response create(@Valid @RequestBody NcDto.CreateRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public NcDto.Response get(@PathVariable UUID id) { return service.findById(id); }

    @PutMapping("/{id}")
    public NcDto.Response update(@PathVariable UUID id,
                                 @Valid @RequestBody NcDto.UpdateRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/start-analysis")
    public NcDto.Response startAnalysis(@PathVariable UUID id,
                                        @RequestBody(required = false) NcDto.StartAnalysisRequest request) {
        return service.startAnalysis(id, request);
    }

    @PostMapping("/{id}/define-action")
    public NcDto.Response defineAction(@PathVariable UUID id) {
        return service.defineAction(id);
    }

    @PostMapping("/{id}/resolve")
    public NcDto.Response resolve(@PathVariable UUID id,
                                  @Valid @RequestBody NcDto.ResolveRequest request) {
        return service.resolve(id, request);
    }

    @PostMapping("/{id}/close")
    public NcDto.Response close(@PathVariable UUID id) {
        return service.close(id);
    }

    @PostMapping("/{id}/cancel")
    public NcDto.Response cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }

    @PostMapping("/{id}/escalate-capa")
    public NcDto.Response escalateToCapa(@PathVariable UUID id,
                                         @Valid @RequestBody NcDto.EscalateRequest request) {
        return service.escalateToCapa(id, request);
    }
}
