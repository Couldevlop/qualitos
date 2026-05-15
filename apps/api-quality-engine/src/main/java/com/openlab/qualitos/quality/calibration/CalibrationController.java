package com.openlab.qualitos.quality.calibration;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calibration")
public class CalibrationController {

    private final CalibrationService service;

    public CalibrationController(CalibrationService service) { this.service = service; }

    // ---- Equipment ----

    @GetMapping("/equipment")
    public Page<CalibrationDto.EquipmentResponse> list(
            @RequestParam(required = false) EquipmentStatus status,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.listEquipment(status, pageable);
    }

    @PostMapping("/equipment")
    @ResponseStatus(HttpStatus.CREATED)
    public CalibrationDto.EquipmentResponse create(
            @Valid @RequestBody CalibrationDto.CreateEquipmentRequest req) {
        return service.createEquipment(req);
    }

    @GetMapping("/equipment/{id}")
    public CalibrationDto.EquipmentResponse get(@PathVariable UUID id) {
        return service.getEquipment(id);
    }

    @PatchMapping("/equipment/{id}")
    public CalibrationDto.EquipmentResponse update(@PathVariable UUID id,
                                                   @Valid @RequestBody CalibrationDto.UpdateEquipmentRequest req) {
        return service.updateEquipment(id, req);
    }

    @PostMapping("/equipment/{id}/status/{target}")
    public CalibrationDto.EquipmentResponse setStatus(@PathVariable UUID id,
                                                     @PathVariable EquipmentStatus target) {
        return service.setEquipmentStatus(id, target);
    }

    @DeleteMapping("/equipment/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.deleteEquipment(id); }

    @GetMapping("/equipment/{id}/summary")
    public CalibrationDto.EquipmentSummary summary(@PathVariable UUID id) {
        return service.summary(id);
    }

    // ---- Plan ----

    @PutMapping("/equipment/{id}/plan")
    public CalibrationDto.PlanResponse upsertPlan(@PathVariable UUID id,
                                                  @Valid @RequestBody CalibrationDto.UpsertPlanRequest req) {
        return service.upsertPlan(id, req);
    }

    @GetMapping("/equipment/{id}/plan")
    public ResponseEntity<CalibrationDto.PlanResponse> getPlan(@PathVariable UUID id) {
        return service.getPlan(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/equipment/{id}/plan")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlan(@PathVariable UUID id) { service.deletePlan(id); }

    @GetMapping("/plans/overdue")
    public Page<CalibrationDto.PlanResponse> overdue(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cutoff,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.overdue(cutoff, pageable);
    }

    // ---- Records ----

    @GetMapping("/equipment/{id}/records")
    public Page<CalibrationDto.RecordResponse> listRecords(@PathVariable UUID id,
                                                           @PageableDefault(size = 100) Pageable pageable) {
        return service.listRecords(id, pageable);
    }

    @PostMapping("/equipment/{id}/records")
    @ResponseStatus(HttpStatus.CREATED)
    public CalibrationDto.RecordResponse addRecord(@PathVariable UUID id,
                                                   @Valid @RequestBody CalibrationDto.CreateRecordRequest req) {
        return service.addRecord(id, req);
    }

    // ---- MSA ----

    @GetMapping("/equipment/{id}/msa")
    public Page<CalibrationDto.MsaResponse> listMsa(@PathVariable UUID id,
                                                    @PageableDefault(size = 100) Pageable pageable) {
        return service.listMsa(id, pageable);
    }

    @PostMapping("/equipment/{id}/msa")
    @ResponseStatus(HttpStatus.CREATED)
    public CalibrationDto.MsaResponse addMsa(@PathVariable UUID id,
                                             @Valid @RequestBody CalibrationDto.CreateMsaRequest req) {
        return service.addMsa(id, req);
    }
}
