package com.openlab.qualitos.quality.controlplan.web;

import com.openlab.qualitos.quality.common.StepUpGuard;
import com.openlab.qualitos.quality.controlplan.application.ControlPlanDto;
import com.openlab.qualitos.quality.controlplan.application.ControlPlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
 * Endpoints — control plan, imbriqués sous le produit qu'ils couvrent.
 *
 * <p>L'approbation est la seule écriture réservée à la direction qualité : c'est
 * elle qui rend le document opposable, et elle engage l'organisation devant
 * l'auditeur. Le reste de l'édition revient aux rôles qui pilotent le système
 * qualité, lecture ouverte à tout utilisateur authentifié (OWASP A01).
 */
@RestController
@RequestMapping("/api/v1/products/{productId}/control-plans")
@Validated
@PreAuthorize("isAuthenticated()")
public class ControlPlanController {

    private static final String EDIT_ROLES =
            "hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')";
    private static final String APPROVE_ROLES =
            "hasAnyRole('DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')";

    private final ControlPlanService service;
    private final StepUpGuard stepUp;

    public ControlPlanController(ControlPlanService service, StepUpGuard stepUp) {
        this.service = service;
        this.stepUp = stepUp;
    }

    @GetMapping
    public List<ControlPlanDto.View> list(@PathVariable UUID productId) {
        return service.listForProduct(productId);
    }

    @GetMapping("/{planId}")
    public ControlPlanDto.Detail get(@PathVariable UUID productId, @PathVariable UUID planId) {
        return service.get(productId, planId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(EDIT_ROLES)
    public ControlPlanDto.View create(@PathVariable UUID productId,
                                      @Valid @RequestBody ControlPlanWebDto.CreateRequest req) {
        return service.createDraft(productId,
                new ControlPlanDto.CreateCommand(req.phase(), req.code(), req.ownerUserId()));
    }

    @PostMapping("/{planId}/revision")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(EDIT_ROLES)
    public ControlPlanDto.View openRevision(@PathVariable UUID productId, @PathVariable UUID planId) {
        return service.openRevision(productId, planId);
    }

    /**
     * Approuver rend le document opposable : c'est une action critique au sens de
     * la règle 18.2 §5, et le rôle seul ne prouve pas qu'un second facteur a été
     * présenté. Le jeton, lui, le prouve.
     */
    @PostMapping("/{planId}/approve")
    @PreAuthorize(APPROVE_ROLES)
    public ControlPlanDto.View approve(@PathVariable UUID productId, @PathVariable UUID planId) {
        stepUp.require("approuver un control plan");
        return service.approve(productId, planId);
    }

    @GetMapping("/{planId}/lines")
    public List<ControlPlanDto.LineView> lines(@PathVariable UUID productId,
                                               @PathVariable UUID planId) {
        return service.get(productId, planId).lines();
    }

    @PostMapping("/{planId}/lines")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(EDIT_ROLES)
    public ControlPlanDto.LineView addLine(@PathVariable UUID productId,
                                           @PathVariable UUID planId,
                                           @Valid @RequestBody ControlPlanWebDto.LineRequest req) {
        return service.addLine(productId, planId, toCommand(req));
    }

    @PutMapping("/{planId}/lines/{lineId}")
    @PreAuthorize(EDIT_ROLES)
    public ControlPlanDto.LineView updateLine(@PathVariable UUID productId,
                                              @PathVariable UUID planId,
                                              @PathVariable UUID lineId,
                                              @Valid @RequestBody ControlPlanWebDto.LineRequest req) {
        return service.updateLine(productId, planId, lineId, toCommand(req));
    }

    @DeleteMapping("/{planId}/lines/{lineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(EDIT_ROLES)
    public void deleteLine(@PathVariable UUID productId, @PathVariable UUID planId,
                           @PathVariable UUID lineId) {
        service.deleteLine(productId, planId, lineId);
    }

    private static ControlPlanDto.LineCommand toCommand(ControlPlanWebDto.LineRequest req) {
        return new ControlPlanDto.LineCommand(
                req.sequenceNo(), req.operationId(), req.machine(), req.characteristicNo(),
                req.characteristicLabel(), req.characteristicType(), req.specialClass(),
                req.specification(), req.toleranceLower(), req.toleranceUpper(), req.unit(),
                req.measurementTechnique(), req.sampleSize(), req.sampleFrequency(),
                req.controlMethod(), req.reactionPlan(), req.fmeaItemId(),
                req.sopReference(), req.inputOutput(), req.whoMeasures(),
                req.recordingLocation());
    }
}
