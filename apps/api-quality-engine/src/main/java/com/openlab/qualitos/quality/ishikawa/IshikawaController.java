package com.openlab.qualitos.quality.ishikawa;

import com.openlab.qualitos.quality.pdca.PdcaDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ishikawa")
public class IshikawaController {

    private final IshikawaService ishikawaService;

    public IshikawaController(IshikawaService ishikawaService) {
        this.ishikawaService = ishikawaService;
    }

    @GetMapping("/diagrams")
    public Page<IshikawaDto.DiagramResponse> listDiagrams(
            @RequestParam(required = false) IshikawaStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ishikawaService.findAll(status, pageable);
    }

    @PostMapping("/diagrams")
    @ResponseStatus(HttpStatus.CREATED)
    public IshikawaDto.DiagramResponse createDiagram(
            @Valid @RequestBody IshikawaDto.CreateDiagramRequest request) {
        return ishikawaService.createDiagram(request);
    }

    @GetMapping("/diagrams/{id}")
    public IshikawaDto.DiagramResponse getDiagram(@PathVariable UUID id) {
        return ishikawaService.findById(id);
    }

    @PatchMapping("/diagrams/{id}")
    public IshikawaDto.DiagramResponse updateDiagram(
            @PathVariable UUID id,
            @Valid @RequestBody IshikawaDto.UpdateDiagramRequest request) {
        return ishikawaService.updateDiagram(id, request);
    }

    @DeleteMapping("/diagrams/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDiagram(@PathVariable UUID id) {
        ishikawaService.deleteDiagram(id);
    }

    @PostMapping("/diagrams/{id}/causes")
    @ResponseStatus(HttpStatus.CREATED)
    public IshikawaDto.CauseResponse addCause(
            @PathVariable UUID id,
            @Valid @RequestBody IshikawaDto.CauseRequest request) {
        return ishikawaService.addCause(id, request);
    }

    // Suggestion de causes par l'IA (§3.5) — POST : appel LLM (effet de bord), pas de persistance.
    @PostMapping("/diagrams/{id}/suggest-causes")
    public java.util.List<IshikawaDto.SuggestedCause> suggestCauses(@PathVariable UUID id) {
        return ishikawaService.suggestCauses(id);
    }

    // Conversion en cycle PDCA (§3.6 — référentiel commun). causeId optionnel :
    // si fourni, la cause-racine ciblée est référencée dans le cycle créé.
    @PostMapping("/diagrams/{id}/convert-to-pdca")
    @ResponseStatus(HttpStatus.CREATED)
    public PdcaDto.CycleResponse convertToPdca(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID causeId) {
        return ishikawaService.convertToPdca(id, causeId);
    }

    @PatchMapping("/diagrams/{id}/causes/{causeId}")
    public IshikawaDto.CauseResponse updateCause(
            @PathVariable UUID id,
            @PathVariable UUID causeId,
            @Valid @RequestBody IshikawaDto.UpdateCauseRequest request) {
        return ishikawaService.updateCause(id, causeId, request);
    }

    @DeleteMapping("/diagrams/{id}/causes/{causeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCause(@PathVariable UUID id, @PathVariable UUID causeId) {
        ishikawaService.deleteCause(id, causeId);
    }
}
