package com.openlab.qualitos.quality.dpia.web;

import com.openlab.qualitos.quality.dpia.application.DpiaDto;
import com.openlab.qualitos.quality.dpia.application.DpiaService;
import com.openlab.qualitos.quality.dpia.domain.DpiaStatus;
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
 * Endpoints — DPIA (RGPD Art. 35) avec workflow DPO.
 */
@RestController
@RequestMapping("/api/v1/gdpr/dpias")
@Validated
public class DpiaController {

    private final DpiaService service;

    public DpiaController(DpiaService service) { this.service = service; }

    @GetMapping
    public List<DpiaDto.View> list(@RequestParam(required = false) DpiaStatus status) {
        return service.list(status);
    }

    @GetMapping("/requiring-consultation")
    public List<DpiaDto.View> requiringConsultation() {
        return service.requiringConsultation();
    }

    @GetMapping("/{id}")
    public DpiaDto.View get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping("/by-reference")
    public DpiaDto.View getByReference(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$") String reference) {
        return service.getByReference(reference);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DpiaDto.View create(@Valid @RequestBody DpiaWebDto.CreateRequest req) {
        return service.create(new DpiaDto.CreateRequest(
                req.reference(), req.title(), req.description(),
                req.linkedProcessingActivityIds(),
                req.initialRiskLevel(), req.createdByUserId()));
    }

    @PutMapping("/{id}")
    public DpiaDto.View edit(@PathVariable UUID id,
                             @Valid @RequestBody DpiaWebDto.EditRequest req) {
        return service.edit(id, new DpiaDto.EditRequest(
                req.title(), req.description(),
                req.linkedProcessingActivityIds(),
                req.necessityAndProportionalityNotes(),
                req.risksToRightsAndFreedoms(),
                req.mitigationMeasures(),
                req.overallRiskLevel(),
                req.consultationRequired(), req.consultationNotes()));
    }

    @PostMapping("/{id}/start")
    public DpiaDto.View start(@PathVariable UUID id,
                              @Valid @RequestBody DpiaWebDto.StartRequest req) {
        return service.start(id, new DpiaDto.StartRequest(req.handledByUserId()));
    }

    @PostMapping("/{id}/return-to-draft")
    public DpiaDto.View returnToDraft(@PathVariable UUID id) {
        return service.returnToDraft(id);
    }

    @PostMapping("/{id}/submit-to-dpo")
    public DpiaDto.View submitToDpo(@PathVariable UUID id) {
        return service.submitToDpo(id);
    }

    @PostMapping("/{id}/approve")
    public DpiaDto.View approve(@PathVariable UUID id,
                                @Valid @RequestBody DpiaWebDto.OpinionRequest req) {
        return service.approve(id,
                new DpiaDto.OpinionRequest(req.dpoUserId(), req.dpoOpinion()));
    }

    @PostMapping("/{id}/reject")
    public DpiaDto.View reject(@PathVariable UUID id,
                               @Valid @RequestBody DpiaWebDto.OpinionRequest req) {
        return service.reject(id,
                new DpiaDto.OpinionRequest(req.dpoUserId(), req.dpoOpinion()));
    }

    @PostMapping("/{id}/archive")
    public DpiaDto.View archive(@PathVariable UUID id) {
        return service.archive(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
