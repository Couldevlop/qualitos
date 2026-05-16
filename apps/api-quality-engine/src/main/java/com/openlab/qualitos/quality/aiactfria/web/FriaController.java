package com.openlab.qualitos.quality.aiactfria.web;

import com.openlab.qualitos.quality.aiactfria.application.FriaDto;
import com.openlab.qualitos.quality.aiactfria.application.FriaService;
import com.openlab.qualitos.quality.aiactfria.domain.FriaStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints — registre des FRIA (AI Act Art. 27).
 */
@RestController
@RequestMapping("/api/v1/ai-act/fria")
@Validated
public class FriaController {

    private final FriaService service;

    public FriaController(FriaService service) { this.service = service; }

    @GetMapping
    public List<FriaDto.View> list(@RequestParam(required = false) FriaStatus status) {
        return service.list(status);
    }

    @GetMapping("/by-system")
    public List<FriaDto.View> byAiSystem(@RequestParam @NotNull UUID aiSystemId) {
        return service.listByAiSystem(aiSystemId);
    }

    @GetMapping("/{id}")
    public FriaDto.View get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping("/by-reference")
    public FriaDto.View getByReference(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$") String reference) {
        return service.getByReference(reference);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FriaDto.View draft(@Valid @RequestBody FriaWebDto.DraftRequest req) {
        return service.draft(new FriaDto.DraftRequest(
                req.reference(), req.aiSystemId(),
                req.processDescription(), req.deploymentDurationDescription(),
                req.affectedPersonsCategories(), req.specificRisks(),
                req.mitigationMeasures(), req.humanOversightMeasures(),
                req.complaintMechanismDescription(), req.createdByUserId()));
    }

    @PutMapping("/{id}")
    public FriaDto.View edit(@PathVariable UUID id,
                             @Valid @RequestBody FriaWebDto.EditRequest req) {
        return service.edit(id, new FriaDto.EditRequest(
                req.processDescription(), req.deploymentDurationDescription(),
                req.affectedPersonsCategories(), req.specificRisks(),
                req.mitigationMeasures(), req.humanOversightMeasures(),
                req.complaintMechanismDescription()));
    }

    @PostMapping("/{id}/submit")
    public FriaDto.View submit(@PathVariable UUID id,
                               @Valid @RequestBody FriaWebDto.SubmitRequest req) {
        return service.submit(id, new FriaDto.SubmitRequest(req.submittedByUserId()));
    }

    @PostMapping("/{id}/approve")
    public FriaDto.View approve(@PathVariable UUID id,
                                @Valid @RequestBody FriaWebDto.ApproveRequest req) {
        return service.approve(id, new FriaDto.ApproveRequest(
                req.approvedByUserId(), req.approvalNotes()));
    }

    @PostMapping("/{id}/return")
    public FriaDto.View returnToDraft(@PathVariable UUID id,
                                      @Valid @RequestBody FriaWebDto.ReturnRequest req) {
        return service.returnToDraft(id, new FriaDto.ReturnRequest(req.reason()));
    }

    @PostMapping("/{id}/archive")
    public FriaDto.View archive(@PathVariable UUID id,
                                @Valid @RequestBody FriaWebDto.ArchiveRequest req) {
        return service.archive(id, new FriaDto.ArchiveRequest(req.reason()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
