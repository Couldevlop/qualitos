package com.openlab.qualitos.quality.complaints;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/complaints")
public class ComplaintController {

    private final ComplaintService service;

    public ComplaintController(ComplaintService service) { this.service = service; }

    @GetMapping
    public Page<ComplaintDto.ComplaintResponse> list(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) ComplaintCategory category,
            @RequestParam(required = false) UUID supplierId,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.list(status, category, supplierId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintDto.ComplaintResponse create(
            @Valid @RequestBody ComplaintDto.CreateComplaintRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    public ComplaintDto.ComplaintResponse get(@PathVariable UUID id) { return service.get(id); }

    @PatchMapping("/{id}")
    public ComplaintDto.ComplaintResponse update(@PathVariable UUID id,
                                                 @Valid @RequestBody ComplaintDto.UpdateComplaintRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }

    @PostMapping("/{id}/assign")
    public ComplaintDto.ComplaintResponse assign(@PathVariable UUID id,
                                                 @Valid @RequestBody ComplaintDto.AssignRequest req) {
        return service.assign(id, req);
    }

    @PostMapping("/{id}/reject")
    public ComplaintDto.ComplaintResponse reject(@PathVariable UUID id,
                                                 @Valid @RequestBody ComplaintDto.RejectRequest req) {
        return service.reject(id, req);
    }

    @PostMapping("/{id}/resolve")
    public ComplaintDto.ComplaintResponse resolve(@PathVariable UUID id,
                                                  @RequestBody(required = false) ComplaintDto.ResolveRequest req) {
        return service.resolve(id, req);
    }

    @PostMapping("/{id}/close")
    public ComplaintDto.ComplaintResponse close(@PathVariable UUID id) { return service.close(id); }

    @PostMapping("/{id}/reopen")
    public ComplaintDto.ComplaintResponse reopen(@PathVariable UUID id) { return service.reopen(id); }

    @PostMapping("/{id}/satisfaction")
    public ComplaintDto.ComplaintResponse satisfaction(@PathVariable UUID id,
                                                       @Valid @RequestBody ComplaintDto.SatisfactionRequest req) {
        return service.setSatisfaction(id, req);
    }

    @GetMapping("/{id}/responses")
    public Page<ComplaintDto.ResponseEntryResponse> responses(@PathVariable UUID id,
                                                              @PageableDefault(size = 100) Pageable pageable) {
        return service.listResponses(id, pageable);
    }

    @PostMapping("/{id}/responses")
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintDto.ResponseEntryResponse addResponse(
            @PathVariable UUID id,
            @Valid @RequestBody ComplaintDto.AddResponseRequest req) {
        return service.addResponse(id, req);
    }

    @DeleteMapping("/{id}/responses/{responseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResponse(@PathVariable UUID id, @PathVariable UUID responseId) {
        service.deleteResponse(id, responseId);
    }

    @GetMapping("/statistics")
    public ComplaintDto.ComplaintStatistics statistics() { return service.statistics(); }
}
