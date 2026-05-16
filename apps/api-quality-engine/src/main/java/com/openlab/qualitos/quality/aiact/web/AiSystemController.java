package com.openlab.qualitos.quality.aiact.web;

import com.openlab.qualitos.quality.aiact.application.AiSystemDto;
import com.openlab.qualitos.quality.aiact.application.AiSystemService;
import com.openlab.qualitos.quality.aiact.domain.AiRiskClassification;
import com.openlab.qualitos.quality.aiact.domain.AiSystemStatus;
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
 * Endpoints — registre des systèmes d'IA AI Act.
 */
@RestController
@RequestMapping("/api/v1/ai-act/systems")
@Validated
public class AiSystemController {

    private final AiSystemService service;

    public AiSystemController(AiSystemService service) { this.service = service; }

    @GetMapping
    public List<AiSystemDto.View> list(@RequestParam(required = false) AiSystemStatus status) {
        return service.list(status);
    }

    @GetMapping("/by-risk")
    public List<AiSystemDto.View> byRisk(@RequestParam @NotNull AiRiskClassification classification) {
        return service.listByRiskClassification(classification);
    }

    @GetMapping("/{id}")
    public AiSystemDto.View get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping("/by-reference")
    public AiSystemDto.View getByReference(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$") String reference) {
        return service.getByReference(reference);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AiSystemDto.View draft(@Valid @RequestBody AiSystemWebDto.DraftRequest req) {
        return service.draft(new AiSystemDto.DraftRequest(
                req.reference(), req.name(), req.description(),
                req.providerName(), req.intendedPurpose(),
                req.riskClassification(), req.role(), req.generalPurpose(),
                req.conformityAssessmentEvidenceUrl(), req.ceMarkingNumber(),
                req.humanOversightDescription(), req.transparencyMeasures(),
                req.dataGovernanceNotes(),
                req.linkedDpiaId(),
                req.linkedProcessingActivityIds(),
                req.linkedAutomatedDecisionIds(),
                req.createdByUserId()));
    }

    @PutMapping("/{id}")
    public AiSystemDto.View edit(@PathVariable UUID id,
                                 @Valid @RequestBody AiSystemWebDto.EditRequest req) {
        return service.edit(id, new AiSystemDto.EditRequest(
                req.name(), req.description(),
                req.providerName(), req.intendedPurpose(),
                req.riskClassification(), req.role(), req.generalPurpose(),
                req.conformityAssessmentEvidenceUrl(), req.ceMarkingNumber(),
                req.humanOversightDescription(), req.transparencyMeasures(),
                req.dataGovernanceNotes(),
                req.linkedDpiaId(),
                req.linkedProcessingActivityIds(),
                req.linkedAutomatedDecisionIds()));
    }

    @PostMapping("/{id}/register")
    public AiSystemDto.View register(@PathVariable UUID id) {
        return service.register(id);
    }

    @PostMapping("/{id}/put-in-use")
    public AiSystemDto.View putInUse(@PathVariable UUID id) {
        return service.putInUse(id);
    }

    @PostMapping("/{id}/decommission")
    public AiSystemDto.View decommission(@PathVariable UUID id) {
        return service.decommission(id);
    }

    @PostMapping("/{id}/withdraw")
    public AiSystemDto.View withdraw(@PathVariable UUID id,
                                     @Valid @RequestBody AiSystemWebDto.WithdrawRequest req) {
        return service.withdraw(id, new AiSystemDto.WithdrawRequest(req.reason()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
