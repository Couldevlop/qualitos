package com.openlab.qualitos.quality.audit;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audits")
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) { this.service = service; }

    @GetMapping("/plans")
    public Page<AuditDto.PlanResponse> list(
            @RequestParam(required = false) AuditStatus status,
            @RequestParam(required = false) AuditType type,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.findAll(status, type, pageable);
    }

    @PostMapping("/plans")
    @ResponseStatus(HttpStatus.CREATED)
    public AuditDto.PlanResponse create(@Valid @RequestBody AuditDto.CreatePlanRequest req) {
        return service.createPlan(req);
    }

    @GetMapping("/plans/{id}")
    public AuditDto.PlanResponse get(@PathVariable UUID id) { return service.findById(id); }

    @PatchMapping("/plans/{id}")
    public AuditDto.PlanResponse update(@PathVariable UUID id,
                                        @Valid @RequestBody AuditDto.UpdatePlanRequest req) {
        return service.updatePlan(id, req);
    }

    @PatchMapping("/plans/{id}/start")
    public AuditDto.PlanResponse start(@PathVariable UUID id) { return service.startPlan(id); }

    @PatchMapping("/plans/{id}/complete")
    public AuditDto.PlanResponse complete(@PathVariable UUID id,
                                          @RequestBody(required = false) AuditDto.CompleteRequest req) {
        return service.completePlan(id, req);
    }

    @PatchMapping("/plans/{id}/cancel")
    public AuditDto.PlanResponse cancel(@PathVariable UUID id) { return service.cancelPlan(id); }

    @DeleteMapping("/plans/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.deletePlan(id); }

    // checklist
    @PostMapping("/plans/{id}/checklist")
    @ResponseStatus(HttpStatus.CREATED)
    public AuditDto.ChecklistItemResponse addItem(@PathVariable UUID id,
                                                  @Valid @RequestBody AuditDto.ChecklistItemRequest req) {
        return service.addChecklistItem(id, req);
    }

    @PatchMapping("/plans/{id}/checklist/{itemId}")
    public AuditDto.ChecklistItemResponse updateItem(@PathVariable UUID id,
                                                    @PathVariable UUID itemId,
                                                    @RequestBody AuditDto.ChecklistItemRequest req) {
        return service.updateChecklistItem(id, itemId, req);
    }

    @PutMapping("/plans/{id}/checklist/{itemId}/response")
    public AuditDto.ChecklistItemResponse respondItem(@PathVariable UUID id,
                                                     @PathVariable UUID itemId,
                                                     @RequestBody AuditDto.ChecklistResponseRequest req) {
        return service.respondToChecklistItem(id, itemId, req);
    }

    @DeleteMapping("/plans/{id}/checklist/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable UUID id, @PathVariable UUID itemId) {
        service.deleteChecklistItem(id, itemId);
    }

    // findings
    @PostMapping("/plans/{id}/findings")
    @ResponseStatus(HttpStatus.CREATED)
    public AuditDto.FindingResponse addFinding(@PathVariable UUID id,
                                               @Valid @RequestBody AuditDto.FindingRequest req) {
        return service.addFinding(id, req);
    }

    @PatchMapping("/plans/{id}/findings/{findingId}")
    public AuditDto.FindingResponse updateFinding(@PathVariable UUID id,
                                                  @PathVariable UUID findingId,
                                                  @RequestBody AuditDto.UpdateFindingRequest req) {
        return service.updateFinding(id, findingId, req);
    }

    @DeleteMapping("/plans/{id}/findings/{findingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFinding(@PathVariable UUID id, @PathVariable UUID findingId) {
        service.deleteFinding(id, findingId);
    }
}
