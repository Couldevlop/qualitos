package com.openlab.qualitos.quality.spc.capability.web;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import com.openlab.qualitos.quality.config.RequiresModule;
import com.openlab.qualitos.quality.spc.capability.application.SpcStudyDto;
import com.openlab.qualitos.quality.spc.capability.application.SpcStudyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Le registre des études de capabilité SPC (Cp / Cpk, cartes X̄ – R).
 *
 * <p><b>Lire est ouvert à tout authentifié ; écrire demande l'action
 * {@code spc.manage}</b>, accordée par défaut aux profils de pilotage et
 * réglable dans la matrice des droits du client (ADR 0078).
 *
 * <p><b>Module « dmaic ».</b> La capabilité procédé relève du pilotage Six
 * Sigma (§3.4) : sans abonnement, les écritures sont refusées ; les lectures
 * restent permises, pour qu'une résiliation ne rende pas une preuve illisible.
 *
 * <p>Les écritures sont transactionnelles ICI : l'étude et sa trace d'audit
 * sont validées ensemble ou pas du tout.
 */
@RestController
@RequestMapping("/api/v1/spc/studies")
@PreAuthorize("isAuthenticated()")
@RequiresModule("dmaic")
@Tag(name = "SPC studies", description = "Process capability studies (Cp / Cpk) on X-bar / R charts")
public class SpcStudyController {

    private final SpcStudyService service;

    public SpcStudyController(SpcStudyService service) {
        this.service = service;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "The study register, most recent first, without the measurement grids")
    public List<SpcStudyDto.StudySummary> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "A study: identification, measurement grid and the full calculation")
    public SpcStudyDto.StudySheet get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(Permission.SPC_MANAGE)
    @Transactional
    @Operation(summary = "Record a study; the server computes Cp and Cpk and freezes the tenant thresholds")
    public SpcStudyDto.StudySheet create(@Valid @RequestBody SpcStudyWebDto.StudyRequest requete) {
        return service.create(commande(requete));
    }

    @PutMapping("/{id}")
    @RequiresPermission(Permission.SPC_MANAGE)
    @Transactional
    @Operation(summary = "Correct a study; Cp and Cpk are recomputed, its thresholds are kept")
    public SpcStudyDto.StudySheet revise(@PathVariable UUID id,
                                         @Valid @RequestBody SpcStudyWebDto.StudyRequest requete) {
        return service.revise(id, commande(requete));
    }

    @PatchMapping("/{id}/action-plan")
    @RequiresPermission(Permission.SPC_MANAGE)
    @Transactional
    @Operation(summary = "Tick or untick the action plan from the register")
    public SpcStudyDto.StudySummary actionPlan(@PathVariable UUID id,
                                               @Valid @RequestBody SpcStudyWebDto.ActionPlanRequest requete) {
        return service.setActionPlan(id, requete.actionPlan());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission(Permission.SPC_MANAGE)
    @Transactional
    @Operation(summary = "Delete a study")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    private static SpcStudyDto.StudyCommand commande(SpcStudyWebDto.StudyRequest r) {
        return new SpcStudyDto.StudyCommand(r.partReference(), r.characteristic(), r.specification(),
                r.machine(), r.leaderUserId(), r.leaderName(), r.studyDate(), r.actionPlan(),
                r.lsl(), r.usl(), r.subgroupSize(), r.measurements());
    }
}
