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
    private final IshikawaActionService actionService;

    public IshikawaController(IshikawaService ishikawaService,
                              IshikawaActionService actionService) {
        this.ishikawaService = ishikawaService;
        this.actionService = actionService;
    }

    // ---- Plan d'actions du diagramme (§3.5) -----------------------------------

    @GetMapping("/diagrams/{diagramId}/actions")
    public java.util.List<IshikawaDto.ActionResponse> listActions(@PathVariable UUID diagramId) {
        return actionService.list(diagramId);
    }

    @PostMapping("/diagrams/{diagramId}/actions")
    @ResponseStatus(HttpStatus.CREATED)
    public IshikawaDto.ActionResponse createAction(
            @PathVariable UUID diagramId,
            @Valid @RequestBody IshikawaDto.CreateActionRequest request) {
        return actionService.create(diagramId, request);
    }

    /** Édition en ligne : un champ absent du corps signifie « inchangé ». */
    @PatchMapping("/actions/{id}")
    public IshikawaDto.ActionResponse updateAction(
            @PathVariable UUID id,
            @Valid @RequestBody IshikawaDto.UpdateActionRequest request) {
        return actionService.update(id, request);
    }

    @DeleteMapping("/actions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAction(@PathVariable UUID id) {
        actionService.delete(id);
    }

    /**
     * Diagrammes partant d'une non-conformité. Route DISTINCTE de la liste
     * paginée : elle répond à une question fermée — « qu'a-t-on déjà ouvert sur
     * cet écart ? » — dont la réponse tient en quelques éléments. La pagination
     * n'y apporterait rien, et le même chemin avec ou sans paramètre rendrait le
     * type de retour dépendant de l'appel.
     */
    @GetMapping("/diagrams/by-nc/{ncId}")
    public java.util.List<IshikawaDto.DiagramResponse> listDiagramsForNc(@PathVariable UUID ncId) {
        return ishikawaService.findByNc(ncId);
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
