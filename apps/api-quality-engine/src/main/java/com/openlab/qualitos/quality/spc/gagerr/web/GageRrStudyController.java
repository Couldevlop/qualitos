package com.openlab.qualitos.quality.spc.gagerr.web;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import com.openlab.qualitos.quality.config.RequiresModule;
import com.openlab.qualitos.quality.spc.gagerr.application.GageRrStudyDto;
import com.openlab.qualitos.quality.spc.gagerr.application.GageRrStudyService;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrOperator;
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

import java.util.List;
import java.util.UUID;

/**
 * Le registre des études Gage R&amp;R (répétabilité et reproductibilité des
 * moyens de mesure, ADR 0097).
 *
 * <p>Mêmes règles que les études de capabilité (ADR 0096) : <b>lire est ouvert
 * à tout authentifié, écrire demande l'action {@code spc.manage}</b> ; module
 * « dmaic » (la MSA relève du pilotage Six Sigma, §3.4). Les écritures sont
 * transactionnelles ici : l'étude et sa trace d'audit ensemble, ou rien.
 */
@RestController
@RequestMapping("/api/v1/spc/gage-rr")
@PreAuthorize("isAuthenticated()")
@RequiresModule("dmaic")
@Tag(name = "SPC Gage R&R", description = "Measurement system analysis: repeatability and reproducibility studies")
public class GageRrStudyController {

    private final GageRrStudyService service;

    public GageRrStudyController(GageRrStudyService service) {
        this.service = service;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "The Gage R&R register, most recent first, without the measurement grids")
    public List<GageRrStudyDto.StudySummary> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "A study: plan, people, measurement grid, average-and-range and ANOVA results")
    public GageRrStudyDto.StudySheet get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(Permission.SPC_MANAGE)
    @Transactional
    @Operation(summary = "Record a study; the server computes %GRR, ndc and the decision")
    public GageRrStudyDto.StudySheet create(@Valid @RequestBody GageRrWebDto.StudyRequest requete) {
        return service.create(commande(requete));
    }

    @PutMapping("/{id}")
    @RequiresPermission(Permission.SPC_MANAGE)
    @Transactional
    @Operation(summary = "Correct a study; everything is recomputed")
    public GageRrStudyDto.StudySheet revise(@PathVariable UUID id,
                                           @Valid @RequestBody GageRrWebDto.StudyRequest requete) {
        return service.revise(id, commande(requete));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission(Permission.SPC_MANAGE)
    @Transactional
    @Operation(summary = "Delete a study")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    private static GageRrStudyDto.StudyCommand commande(GageRrWebDto.StudyRequest r) {
        List<GageRrOperator> operateurs = r.operators().stream()
                .map(o -> new GageRrOperator(o.accountId(), o.name()))
                .toList();
        return new GageRrStudyDto.StudyCommand(r.partReference(), r.characteristic(), r.instrument(),
                r.lsl(), r.usl(), r.operatorCount(), r.partCount(), r.trialCount(), r.sigmaMultiplier(),
                r.studyDate(), r.responsibleUserId(), r.responsibleName(), operateurs, r.measurements());
    }
}
