package com.openlab.qualitos.quality.stakeholders.web;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import com.openlab.qualitos.quality.stakeholders.application.StakeholderDto;
import com.openlab.qualitos.quality.stakeholders.application.StakeholderPorts;
import com.openlab.qualitos.quality.stakeholders.application.StakeholderService;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderSide;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Le registre des parties intéressées (ISO 9001 §4.2) : identification,
 * analyse, matrice d'évaluation, exigences, et les trois assistants IA.
 *
 * <p><b>Lire est ouvert à tout authentifié ; écrire ne l'est pas.</b> Déterminer
 * les parties intéressées pertinentes, c'est fixer le périmètre du système de
 * management : un acte de pilotage ({@code stakeholder.manage}). La lecture
 * reste ouverte parce que chacun doit pouvoir savoir à quoi l'organisme s'est
 * engagé.
 *
 * <p><b>Les assistants IA sont en POST et sont des LECTURES.</b> Ils ne
 * modifient rien : POST parce qu'ils reçoivent un corps — le texte d'un contrat
 * ne passe pas dans une URL. L'inscription d'une suggestion, l'ajout d'une
 * exigence extraite et la réévaluation après un changement sont trois appels
 * distincts, faits par quelqu'un (§12.3).
 *
 * <p>Les écritures sont transactionnelles ICI : la fiche, ses exigences et la
 * CAPA qu'elle ouvre sont validées ensemble ou pas du tout.
 */
@RestController
@RequestMapping("/api/v1/stakeholders")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Stakeholders", description = "Interested parties register (ISO 9001 4.2)")
public class StakeholderController {

    private final StakeholderService service;

    public StakeholderController(StakeholderService service) {
        this.service = service;
    }

    // ---------- registre ----------

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "The register, most critical first")
    public List<StakeholderDto.Row> list(@RequestParam(required = false) StakeholderSide side,
                                         @RequestParam(required = false) String search) {
        return service.list(side, search);
    }

    @GetMapping("/statistics")
    @Transactional(readOnly = true)
    @Operation(summary = "Counts for the register header tiles")
    public StakeholderDto.Statistics statistics() {
        return service.statistics();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "One sheet: identification, analysis, steering, requirements")
    public StakeholderDto.Sheet get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @RequiresPermission(Permission.STAKEHOLDER_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    @Operation(summary = "Register a stakeholder: name, category, internal or external")
    public StakeholderDto.Sheet create(@Valid @RequestBody StakeholderWebDto.CreateRequest request) {
        return service.create(request.name(), request.category(), request.side());
    }

    @PutMapping("/{id}")
    @RequiresPermission(Permission.STAKEHOLDER_MANAGE)
    @Transactional
    @Operation(summary = "Save the sheet as a whole")
    public StakeholderDto.Sheet update(@PathVariable UUID id,
                                       @Valid @RequestBody StakeholderWebDto.UpdateRequest request) {
        return service.update(id, request.toDomain());
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(Permission.STAKEHOLDER_MANAGE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    @Operation(summary = "Remove a sheet; refused once it carries a requirement")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PostMapping("/{id}/review")
    @RequiresPermission(Permission.STAKEHOLDER_MANAGE)
    @Transactional
    @Operation(summary = "Mark the review as done today")
    public StakeholderDto.Sheet markReviewed(@PathVariable UUID id) {
        return service.markReviewed(id);
    }

    // ---------- matrice d'évaluation ----------

    @PutMapping("/ratings")
    @RequiresPermission(Permission.STAKEHOLDER_MANAGE)
    @Transactional
    @Operation(summary = "Rate several stakeholders at once; criticality is recomputed")
    public List<StakeholderDto.Row> rate(@Valid @RequestBody StakeholderWebDto.RateRequest request) {
        return service.rate(request.toDomain());
    }

    // ---------- exigences ----------

    @PostMapping("/{id}/requirements")
    @RequiresPermission(Permission.STAKEHOLDER_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    @Operation(summary = "Add a requirement to a stakeholder")
    public StakeholderDto.Requirement addRequirement(
            @PathVariable UUID id,
            @Valid @RequestBody StakeholderWebDto.RequirementRequest request) {
        return service.addRequirement(id, request.toDomain());
    }

    @PutMapping("/{id}/requirements/{requirementId}/status")
    @RequiresPermission(Permission.STAKEHOLDER_MANAGE)
    @Transactional
    @Operation(summary = "Change the status of a requirement")
    public StakeholderDto.Requirement setStatus(
            @PathVariable UUID id, @PathVariable UUID requirementId,
            @Valid @RequestBody StakeholderWebDto.StatusRequest request) {
        return service.setRequirementStatus(id, requirementId, request.status());
    }

    @PutMapping("/{id}/requirements/{requirementId}/link")
    @RequiresPermission(Permission.STAKEHOLDER_MANAGE)
    @Transactional
    @Operation(summary = "Link a requirement to a risk, a non-conformity, an audit or a document")
    public StakeholderDto.Requirement link(
            @PathVariable UUID id, @PathVariable UUID requirementId,
            @Valid @RequestBody StakeholderWebDto.LinkRequest request) {
        return service.linkRequirement(id, requirementId, request.link(), request.target());
    }

    @PostMapping("/{id}/requirements/{requirementId}/capa")
    @RequiresPermission(Permission.STAKEHOLDER_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    @Operation(summary = "Open a CAPA case for a requirement that is not met")
    public StakeholderDto.Requirement openCapa(
            @PathVariable UUID id, @PathVariable UUID requirementId,
            @Valid @RequestBody StakeholderWebDto.OpenCapaRequest request) {
        return service.openCapa(id, requirementId, request.title(), request.ownerId(),
                request.ownerName(), request.dueDate());
    }

    @PostMapping("/{id}/requirements/{requirementId}/risk")
    @RequiresPermission(Permission.STAKEHOLDER_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    @Operation(summary = "Register the risk a requirement would create if it were not met")
    public StakeholderDto.Requirement openRisk(
            @PathVariable UUID id, @PathVariable UUID requirementId,
            @Valid @RequestBody StakeholderWebDto.OpenRiskRequest request) {
        return service.openRisk(id, requirementId, request.type(), request.process(),
                request.severity(), request.probability(), request.ownerId(), request.ownerName());
    }

    @DeleteMapping("/{id}/requirements/{requirementId}")
    @RequiresPermission(Permission.STAKEHOLDER_MANAGE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    @Operation(summary = "Remove a requirement")
    public void removeRequirement(@PathVariable UUID id, @PathVariable UUID requirementId) {
        service.removeRequirement(id, requirementId);
    }

    // ---------- assistant IA : propose, n'écrit pas ----------

    @PostMapping("/assistant/suggestions")
    @RequiresPermission(Permission.STAKEHOLDER_MANAGE)
    @Transactional(readOnly = true)
    @Operation(summary = "Suggest stakeholders for a sector; nothing is registered")
    public List<StakeholderPorts.SuggestedStakeholder> suggest(
            @Valid @RequestBody StakeholderWebDto.SuggestRequest request) {
        return service.suggest(request.sector(), request.characteristics());
    }

    @PostMapping("/assistant/requirements")
    @RequiresPermission(Permission.STAKEHOLDER_MANAGE)
    @Transactional(readOnly = true)
    @Operation(summary = "Extract the requirements a contract carries, each quoted; nothing is registered")
    public List<StakeholderPorts.ExtractedRequirement> extract(
            @Valid @RequestBody StakeholderWebDto.ExtractRequest request) {
        return service.extract(request.documentName(), request.documentText());
    }

    @PostMapping("/assistant/changes")
    @RequiresPermission(Permission.STAKEHOLDER_MANAGE)
    @Transactional(readOnly = true)
    @Operation(summary = "Detect what changed between two versions; nothing is registered")
    public List<StakeholderPorts.DetectedChange> changes(
            @Valid @RequestBody StakeholderWebDto.ChangesRequest request) {
        return service.changes(request.documentName(), request.previousText(), request.currentText());
    }

    @PostMapping("/{id}/requirements/reassess")
    @RequiresPermission(Permission.STAKEHOLDER_MANAGE)
    @Transactional
    @Operation(summary = "Mark the requirements a change calls into question; the human decides which")
    public List<StakeholderDto.Requirement> reassess(
            @PathVariable UUID id, @Valid @RequestBody StakeholderWebDto.ReassessRequest request) {
        return service.markToReassess(id, request.requirementIds());
    }
}
