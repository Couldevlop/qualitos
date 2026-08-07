package com.openlab.qualitos.quality.fivewhys;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Analyses des 5 Pourquoi (§3.5). Le tenant est toujours résolu par le service
 * depuis le jeton : aucune route ne l'accepte en paramètre (§18.2 #2).
 */
@RestController
@RequestMapping("/api/v1/five-whys")
public class FiveWhysController {

    private final FiveWhysService service;

    public FiveWhysController(FiveWhysService service) {
        this.service = service;
    }

    @GetMapping
    public Page<FiveWhysDto.AnalysisResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.findAll(pageable);
    }

    /** Analyses ouvertes sur une non-conformité donnée. */
    @GetMapping(params = "ncId")
    public List<FiveWhysDto.AnalysisResponse> listForNc(@RequestParam UUID ncId) {
        return service.findByNc(ncId);
    }

    @GetMapping("/{id}")
    public FiveWhysDto.AnalysisResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FiveWhysDto.AnalysisResponse create(@Valid @RequestBody FiveWhysDto.CreateRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{id}/problem")
    public FiveWhysDto.AnalysisResponse updateProblem(
            @PathVariable UUID id,
            @Valid @RequestBody FiveWhysDto.UpdateProblemRequest request) {
        return service.updateProblem(id, request);
    }

    @PostMapping("/{id}/steps")
    @ResponseStatus(HttpStatus.CREATED)
    public FiveWhysDto.StepResponse addStep(
            @PathVariable UUID id,
            @Valid @RequestBody FiveWhysDto.AddStepRequest request) {
        return service.addStep(id, request);
    }

    @PatchMapping("/steps/{stepId}")
    public FiveWhysDto.StepResponse updateStep(
            @PathVariable UUID stepId,
            @Valid @RequestBody FiveWhysDto.AddStepRequest request) {
        return service.updateStep(stepId, request);
    }

    @DeleteMapping("/steps/{stepId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStep(@PathVariable UUID stepId) {
        service.deleteStep(stepId);
    }

    @PutMapping("/{id}/root-cause")
    public FiveWhysDto.AnalysisResponse setRootCause(
            @PathVariable UUID id,
            @Valid @RequestBody FiveWhysDto.RootCauseRequest request) {
        return service.setRootCause(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
