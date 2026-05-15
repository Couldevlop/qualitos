package com.openlab.qualitos.quality.change;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/changes")
public class ChangeRequestController {

    private final ChangeRequestService service;

    public ChangeRequestController(ChangeRequestService service) { this.service = service; }

    // ---- CRUD ----

    @GetMapping
    public Page<ChangeDto.ChangeResponse> list(
            @RequestParam(required = false) ChangeRequestStatus status,
            @RequestParam(required = false) ChangeRequestType type,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.list(status, type, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChangeDto.ChangeResponse create(@Valid @RequestBody ChangeDto.CreateChangeRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    public ChangeDto.ChangeResponse get(@PathVariable UUID id) { return service.get(id); }

    @PatchMapping("/{id}")
    public ChangeDto.ChangeResponse update(@PathVariable UUID id,
                                           @Valid @RequestBody ChangeDto.UpdateChangeRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }

    // ---- Workflow ----

    @PostMapping("/{id}/submit")
    public ChangeDto.ChangeResponse submit(@PathVariable UUID id) { return service.submit(id); }

    @PostMapping("/{id}/cancel")
    public ChangeDto.ChangeResponse cancel(@PathVariable UUID id,
                                           @RequestParam(required = false) String reason) {
        return service.cancel(id, reason);
    }

    @PostMapping("/{id}/implement")
    public ChangeDto.ChangeResponse implement(@PathVariable UUID id,
                                              @Valid @RequestBody ChangeDto.ImplementRequest req) {
        return service.implement(id, req);
    }

    @GetMapping("/{id}/summary")
    public ChangeDto.ChangeSummary summary(@PathVariable UUID id) { return service.summary(id); }

    // ---- Approvers / decisions ----

    @GetMapping("/{id}/approvals")
    public List<ChangeDto.ApprovalResponse> listApprovals(@PathVariable UUID id) {
        return service.listApprovals(id);
    }

    @PostMapping("/{id}/approvers")
    @ResponseStatus(HttpStatus.CREATED)
    public ChangeDto.ApprovalResponse addApprover(@PathVariable UUID id,
                                                  @Valid @RequestBody ChangeDto.AddApproverRequest req) {
        return service.addApprover(id, req);
    }

    @DeleteMapping("/{id}/approvers/{approverUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeApprover(@PathVariable UUID id, @PathVariable UUID approverUserId) {
        service.removeApprover(id, approverUserId);
    }

    @PostMapping("/{id}/decisions")
    public ChangeDto.ApprovalResponse decide(@PathVariable UUID id,
                                             @Valid @RequestBody ChangeDto.DecisionRequest req) {
        return service.decide(id, req);
    }

    // ---- Impacts ----

    @GetMapping("/{id}/impacts")
    public List<ChangeDto.ImpactResponse> listImpacts(@PathVariable UUID id) {
        return service.listImpacts(id);
    }

    @PostMapping("/{id}/impacts")
    @ResponseStatus(HttpStatus.CREATED)
    public ChangeDto.ImpactResponse addImpact(@PathVariable UUID id,
                                              @Valid @RequestBody ChangeDto.AddImpactRequest req) {
        return service.addImpact(id, req);
    }

    @DeleteMapping("/{id}/impacts/{impactId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeImpact(@PathVariable UUID id, @PathVariable UUID impactId) {
        service.removeImpact(id, impactId);
    }
}
