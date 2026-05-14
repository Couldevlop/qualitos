package com.openlab.qualitos.quality.pdca;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pdca")
public class PdcaController {

    private final PdcaService pdcaService;

    public PdcaController(PdcaService pdcaService) {
        this.pdcaService = pdcaService;
    }

    @GetMapping("/cycles")
    public Page<PdcaDto.CycleResponse> listCycles(
            @RequestParam(required = false) PdcaStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return pdcaService.findAll(status, pageable);
    }

    @PostMapping("/cycles")
    @ResponseStatus(HttpStatus.CREATED)
    public PdcaDto.CycleResponse createCycle(@Valid @RequestBody PdcaDto.CreateCycleRequest request) {
        return pdcaService.createCycle(request);
    }

    @GetMapping("/cycles/{id}")
    public PdcaDto.CycleResponse getCycle(@PathVariable UUID id) {
        return pdcaService.findById(id);
    }

    @PatchMapping("/cycles/{id}/advance")
    public PdcaDto.CycleResponse advanceCycle(@PathVariable UUID id) {
        return pdcaService.advanceCycle(id);
    }

    @PatchMapping("/cycles/{id}/cancel")
    public PdcaDto.CycleResponse cancelCycle(@PathVariable UUID id) {
        return pdcaService.cancelCycle(id);
    }

    @PostMapping("/cycles/{id}/steps")
    @ResponseStatus(HttpStatus.CREATED)
    public PdcaDto.StepResponse addStep(
            @PathVariable UUID id,
            @Valid @RequestBody PdcaDto.StepRequest request) {
        return pdcaService.addStep(id, request);
    }

    @PatchMapping("/cycles/{id}/steps/{stepId}")
    public PdcaDto.StepResponse updateStep(
            @PathVariable UUID id,
            @PathVariable UUID stepId,
            @RequestBody PdcaDto.StepRequest request) {
        return pdcaService.updateStep(id, stepId, request);
    }

    @DeleteMapping("/cycles/{id}/steps/{stepId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStep(@PathVariable UUID id, @PathVariable UUID stepId) {
        pdcaService.deleteStep(id, stepId);
    }
}
