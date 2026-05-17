package com.openlab.qualitos.quality.aiqms.web;

import com.openlab.qualitos.quality.aiqms.application.AiQmsDto;
import com.openlab.qualitos.quality.aiqms.application.AiQmsService;
import com.openlab.qualitos.quality.aiqms.domain.AiQmsStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints — QMS AI Act (Art. 17).
 */
@RestController
@RequestMapping("/api/v1/ai-act/qms")
@Validated
public class AiQmsController {

    private final AiQmsService service;

    public AiQmsController(AiQmsService service) { this.service = service; }

    @GetMapping
    public List<AiQmsDto.View> list(@RequestParam(required = false) AiQmsStatus status) {
        return service.list(status);
    }

    @GetMapping("/{id}")
    public AiQmsDto.View get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping("/by-reference")
    public AiQmsDto.View getByReference(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$") String reference) {
        return service.getByReference(reference);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AiQmsDto.View draft(@Valid @RequestBody AiQmsWebDto.DraftRequest req) {
        return service.draft(new AiQmsDto.DraftRequest(
                req.reference(), req.version(), req.name(), req.description(),
                req.regulatoryComplianceStrategy(), req.designControlDescription(),
                req.qualityControlDescription(), req.dataManagementDescription(),
                req.riskManagementDescription(), req.pmmDescription(),
                req.regulatorCommunicationDescription(),
                req.resourceManagementDescription(), req.supplierMonitoringDescription(),
                req.coveredAiSystemIds(), req.createdByUserId()));
    }

    @PutMapping("/{id}")
    public AiQmsDto.View edit(@PathVariable UUID id,
                              @Valid @RequestBody AiQmsWebDto.EditRequest req) {
        return service.edit(id, new AiQmsDto.EditRequest(
                req.name(), req.description(),
                req.regulatoryComplianceStrategy(), req.designControlDescription(),
                req.qualityControlDescription(), req.dataManagementDescription(),
                req.riskManagementDescription(), req.pmmDescription(),
                req.regulatorCommunicationDescription(),
                req.resourceManagementDescription(), req.supplierMonitoringDescription(),
                req.coveredAiSystemIds()));
    }

    @PostMapping("/{id}/approve")
    public AiQmsDto.View approve(@PathVariable UUID id,
                                 @Valid @RequestBody AiQmsWebDto.ApproveRequest req) {
        return service.approve(id, new AiQmsDto.ApproveRequest(
                req.submittedByUserId(), req.approvedByUserId(), req.approvalNotes()));
    }

    @PostMapping("/{id}/put-in-force")
    public AiQmsDto.View putInForce(@PathVariable UUID id) {
        return service.putInForce(id);
    }

    @PostMapping("/{id}/supersede")
    public AiQmsDto.View supersede(@PathVariable UUID id,
                                   @Valid @RequestBody AiQmsWebDto.SupersedeRequest req) {
        return service.supersede(id, new AiQmsDto.SupersedeRequest(req.supersededByQmsId()));
    }

    @PostMapping("/{id}/archive")
    public AiQmsDto.View archive(@PathVariable UUID id,
                                 @Valid @RequestBody AiQmsWebDto.ArchiveRequest req) {
        return service.archive(id, new AiQmsDto.ArchiveRequest(req.reason()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
