package com.openlab.qualitos.quality.training.competencymatrix.web;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import com.openlab.qualitos.quality.training.competencymatrix.application.CompetencyMatrixDto;
import com.openlab.qualitos.quality.training.competencymatrix.application.CompetencyMatrixService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Les ateliers de la matrice de compétences.
 *
 * <p><b>Les lire est ouvert à tout authentifié</b> : la matrice les propose
 * pour ranger et filtrer les opérateurs. <b>Les changer est réservé à
 * l'administrateur du client</b> ({@code authz.manage}), comme tout paramétrage.
 */
@RestController
@RequestMapping("/api/v1/training/competencies/workshops")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Competency matrix workshops", description = "Tenant workshops used by the competency matrix")
public class CompetencyWorkshopsController {

    private final CompetencyMatrixService service;

    public CompetencyWorkshopsController(CompetencyMatrixService service) {
        this.service = service;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "The tenant workshops, or the platform defaults when none is set")
    public CompetencyMatrixDto.WorkshopsView get() {
        return service.workshops();
    }

    @PutMapping
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    @Transactional
    @Operation(summary = "Replace the workshop list; operators keep the workshop they were filed under")
    public CompetencyMatrixDto.WorkshopsView replace(@Valid @RequestBody CompetencyMatrixWebDto.WorkshopsRequest r) {
        return service.replaceWorkshops(r.names());
    }
}
