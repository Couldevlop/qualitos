package com.openlab.qualitos.quality.kpi;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kpis")
public class KpiController {

    private final KpiService service;

    public KpiController(KpiService service) { this.service = service; }

    @GetMapping
    public Page<KpiDto.KpiResponse> list(
            @RequestParam(required = false) KpiStatus status,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.list(status, category, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KpiDto.KpiResponse create(@Valid @RequestBody KpiDto.CreateKpiRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    public KpiDto.KpiResponse get(@PathVariable UUID id) { return service.get(id); }

    @PatchMapping("/{id}")
    public KpiDto.KpiResponse update(@PathVariable UUID id,
                                     @Valid @RequestBody KpiDto.UpdateKpiRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }

    @PostMapping("/{id}/activate")
    public KpiDto.KpiResponse activate(@PathVariable UUID id) { return service.activate(id); }

    @PostMapping("/{id}/reopen")
    public KpiDto.KpiResponse reopen(@PathVariable UUID id) { return service.reopen(id); }

    @PostMapping("/{id}/archive")
    public KpiDto.KpiResponse archive(@PathVariable UUID id) { return service.archive(id); }

    @GetMapping("/{id}/status")
    public KpiDto.KpiCurrentStatus status(@PathVariable UUID id) {
        return service.currentStatus(id);
    }

    @GetMapping("/{id}/trend")
    public KpiDto.KpiTrend trend(@PathVariable UUID id) {
        return service.trend(id);
    }

    // ---- Measurements ----

    @GetMapping("/{id}/measurements")
    public Page<KpiDto.MeasurementResponse> measurements(@PathVariable UUID id,
                                                         @PageableDefault(size = 100) Pageable pageable) {
        return service.listMeasurements(id, pageable);
    }

    @PostMapping("/{id}/measurements")
    @ResponseStatus(HttpStatus.CREATED)
    public KpiDto.MeasurementResponse record(@PathVariable UUID id,
                                             @Valid @RequestBody KpiDto.RecordMeasurementRequest req) {
        return service.record(id, req);
    }

    @DeleteMapping("/{id}/measurements/{measurementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMeasurement(@PathVariable UUID id, @PathVariable UUID measurementId) {
        service.deleteMeasurement(id, measurementId);
    }
}
