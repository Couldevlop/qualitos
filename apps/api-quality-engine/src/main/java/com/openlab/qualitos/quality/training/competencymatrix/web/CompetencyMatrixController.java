package com.openlab.qualitos.quality.training.competencymatrix.web;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import com.openlab.qualitos.quality.training.competencymatrix.application.CompetencyMatrixDto;
import com.openlab.qualitos.quality.training.competencymatrix.application.CompetencyMatrixService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * La matrice de compétences éditable (ADR 0098).
 *
 * <p><b>La lire est ouvert à tout authentifié</b> : savoir qui sait faire quoi
 * est une information d'équipe. <b>L'écrire demande l'action
 * {@code training.manage}</b>, accordée par défaut aux profils de pilotage et
 * réglable dans la matrice des droits du client (ADR 0078). Le module
 * « formation » appartient au socle : il n'est pas soumis à abonnement.
 *
 * <p>Les écritures sont transactionnelles ICI : l'écriture et sa trace d'audit
 * sont validées ensemble ou pas du tout. Le client vient du jeton, jamais d'un
 * paramètre.
 */
@RestController
@RequestMapping("/api/v1/training/competencies/matrix")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Competency matrix", description = "Editable skill matrix: operators, competences, trainers, levels")
public class CompetencyMatrixController {

    private final CompetencyMatrixService service;

    public CompetencyMatrixController(CompetencyMatrixService service) {
        this.service = service;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "The matrix: workshops, operators, competences with their coverage, and the set levels")
    public CompetencyMatrixDto.MatrixView get() {
        return service.view();
    }

    @PostMapping("/operators")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(Permission.TRAINING_MANAGE)
    @Transactional
    @Operation(summary = "Add a directory user to the matrix, in a workshop; returns the whole matrix")
    public CompetencyMatrixDto.MatrixView addOperator(@Valid @RequestBody CompetencyMatrixWebDto.AddOperatorRequest r) {
        return service.addOperator(new CompetencyMatrixDto.AddOperatorCommand(r.userId(), r.userName(), r.workshop()));
    }

    @DeleteMapping("/operators/{operatorId}")
    @RequiresPermission(Permission.TRAINING_MANAGE)
    @Transactional
    @Operation(summary = "Remove an operator from the matrix; their assessments stay on file")
    public CompetencyMatrixDto.MatrixView removeOperator(@PathVariable UUID operatorId) {
        return service.removeOperator(operatorId);
    }

    @PostMapping("/skills")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(Permission.TRAINING_MANAGE)
    @Transactional
    @Operation(summary = "Add a competence (catalogue skill reused by name, or created) with its trainer")
    public CompetencyMatrixDto.MatrixView addSkill(@Valid @RequestBody CompetencyMatrixWebDto.AddSkillRequest r) {
        return service.addSkill(new CompetencyMatrixDto.AddSkillCommand(r.name(), r.category(), r.trainerUserId(),
                r.trainerName()));
    }

    @DeleteMapping("/skills/{skillId}")
    @RequiresPermission(Permission.TRAINING_MANAGE)
    @Transactional
    @Operation(summary = "Remove a competence from the matrix; it stays in the catalogue")
    public CompetencyMatrixDto.MatrixView removeSkill(@PathVariable UUID skillId) {
        return service.removeSkill(skillId);
    }

    @PutMapping("/levels")
    @RequiresPermission(Permission.TRAINING_MANAGE)
    @Transactional
    @Operation(summary = "Set a level (0-4) — it is the competency assessment of that person")
    public CompetencyMatrixDto.CellView setLevel(@Valid @RequestBody CompetencyMatrixWebDto.SetLevelRequest r) {
        return service.setLevel(new CompetencyMatrixDto.SetLevelCommand(r.userId(), r.skillId(), r.level()));
    }
}
