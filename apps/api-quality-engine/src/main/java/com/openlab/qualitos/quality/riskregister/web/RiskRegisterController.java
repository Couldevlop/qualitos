package com.openlab.qualitos.quality.riskregister.web;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import com.openlab.qualitos.quality.config.RequiresModule;
import com.openlab.qualitos.quality.riskregister.application.RiskRegisterDto;
import com.openlab.qualitos.quality.riskregister.application.RiskRegisterService;
import com.openlab.qualitos.quality.riskregister.domain.RegisterOrigin;
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
 * Le registre des risques et opportunités (ISO 9001 §6.1).
 *
 * <p><b>Lire est ouvert à tout authentifié ; écrire ne l'est pas.</b> Coter un
 * risque, c'est décider de ce qu'on traitera d'abord : un acte de pilotage,
 * réservé aux mêmes profils que le coût de la qualité.
 *
 * <p><b>Module « risk ».</b> Le registre relève du module Risques (§4.5), comme
 * l'AMDEC : sans abonnement, les écritures sont refusées ; les lectures restent
 * permises, pour qu'une résiliation ne rende pas une preuve d'audit illisible.
 *
 * <p>Les écritures sont transactionnelles ICI : la fiche, son suivi, la CAPA
 * qu'elle ouvre et la trace d'audit sont validés ensemble ou pas du tout.
 */
@RestController
@RequestMapping("/api/v1/risk-register")
@PreAuthorize("isAuthenticated()")
@RequiresModule("risk")
@Tag(name = "Risk register", description = "Risks and opportunities register (ISO 9001 6.1)")
public class RiskRegisterController {

    private final RiskRegisterService service;

    public RiskRegisterController(RiskRegisterService service) {
        this.service = service;
    }

    // ---------- risques ----------

    @GetMapping("/risks")
    @Transactional(readOnly = true)
    @Operation(summary = "The whole risk register, by reference")
    public List<RiskRegisterDto.RiskView> risks() {
        return service.risks();
    }

    @GetMapping("/risks/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "A risk sheet: the risk, its linked CAPA cases and its history")
    public RiskRegisterDto.RiskSheet risk(@PathVariable UUID id) {
        return service.risk(id);
    }

    @PostMapping("/risks")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(Permission.RISK_MANAGE)
    @Transactional
    @Operation(summary = "Record a risk; the server assigns its reference")
    public RiskRegisterDto.RiskView createRisk(@Valid @RequestBody RiskRegisterWebDto.RiskRequest r) {
        return service.createRisk(commande(r));
    }

    @PutMapping("/risks/{id}")
    @RequiresPermission(Permission.RISK_MANAGE)
    @Transactional
    @Operation(summary = "Revise a risk; rating, status and decision changes are added to its history")
    public RiskRegisterDto.RiskView reviseRisk(@PathVariable UUID id,
                                               @Valid @RequestBody RiskRegisterWebDto.RiskRequest r) {
        return service.reviseRisk(id, commande(r));
    }

    @PostMapping("/risks/{id}/capa")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(Permission.RISK_MANAGE)
    @Transactional
    @Operation(summary = "Open a preventive CAPA case on this risk")
    public RiskRegisterDto.CapaView openCapa(@PathVariable UUID id,
                                             @Valid @RequestBody RiskRegisterWebDto.CapaRequest r) {
        return service.openCapa(id, new RiskRegisterDto.CapaCommand(r.title(), r.description(), r.kind(),
                r.assignee(), r.dueDate()));
    }

    /**
     * Le brouillon d'un risque issu d'une ligne d'AMDEC, d'une NC, d'un constat
     * d'audit ou d'un changement. Réservé à qui peut créer le risque : il expose
     * le texte de l'objet source.
     */
    @GetMapping("/sources/{origin}/{sourceId}")
    @RequiresPermission(Permission.RISK_MANAGE)
    @Transactional(readOnly = true)
    @Operation(summary = "Draft a risk from an FMEA line, a non-conformity, an audit finding or a change")
    public RiskRegisterDto.RiskDraft draft(@PathVariable RegisterOrigin origin, @PathVariable UUID sourceId) {
        return service.draft(origin, sourceId);
    }

    // ---------- opportunités ----------

    @GetMapping("/opportunities")
    @Transactional(readOnly = true)
    @Operation(summary = "The whole opportunity register, by reference")
    public List<RiskRegisterDto.OpportunityView> opportunities() {
        return service.opportunities();
    }

    @GetMapping("/opportunities/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "An opportunity sheet: the opportunity, its actions and its history")
    public RiskRegisterDto.OpportunitySheet opportunity(@PathVariable UUID id) {
        return service.opportunity(id);
    }

    @PostMapping("/opportunities")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(Permission.OPPORTUNITY_MANAGE)
    @Transactional
    @Operation(summary = "Record an opportunity; the server assigns its reference")
    public RiskRegisterDto.OpportunityView createOpportunity(
            @Valid @RequestBody RiskRegisterWebDto.OpportunityRequest r) {
        return service.createOpportunity(commande(r));
    }

    @PutMapping("/opportunities/{id}")
    @RequiresPermission(Permission.OPPORTUNITY_MANAGE)
    @Transactional
    @Operation(summary = "Revise an opportunity")
    public RiskRegisterDto.OpportunityView reviseOpportunity(
            @PathVariable UUID id, @Valid @RequestBody RiskRegisterWebDto.OpportunityRequest r) {
        return service.reviseOpportunity(id, commande(r));
    }

    @PostMapping("/opportunities/{id}/actions")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(Permission.OPPORTUNITY_MANAGE)
    @Transactional
    @Operation(summary = "Add an implementation action (ACT-n) to an opportunity")
    public RiskRegisterDto.ActionView addAction(@PathVariable UUID id,
                                                @Valid @RequestBody RiskRegisterWebDto.ActionRequest r) {
        return service.addAction(id, commande(r));
    }

    @PutMapping("/opportunities/{id}/actions/{actionId}")
    @RequiresPermission(Permission.OPPORTUNITY_MANAGE)
    @Transactional
    @Operation(summary = "Revise an opportunity action")
    public RiskRegisterDto.ActionView reviseAction(@PathVariable UUID id, @PathVariable UUID actionId,
                                                   @Valid @RequestBody RiskRegisterWebDto.ActionRequest r) {
        return service.reviseAction(id, actionId, commande(r));
    }

    @DeleteMapping("/opportunities/{id}/actions/{actionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission(Permission.OPPORTUNITY_MANAGE)
    @Transactional
    @Operation(summary = "Delete an opportunity action")
    public void deleteAction(@PathVariable UUID id, @PathVariable UUID actionId) {
        service.deleteAction(id, actionId);
    }

    // ---------- saisie assistée ----------

    @GetMapping("/suggestions")
    @Transactional(readOnly = true)
    @Operation(summary = "Processes, sites and owners already used in the register")
    public RiskRegisterDto.Suggestions suggestions() {
        return service.suggestions();
    }

    private static RiskRegisterDto.RiskCommand commande(RiskRegisterWebDto.RiskRequest r) {
        return new RiskRegisterDto.RiskCommand(r.title(), r.type(), r.process(), r.site(), r.owner(),
                r.cause(), r.effect(), r.origin(), r.originRef(), r.sourceId(), r.grossSeverity(),
                r.grossProbability(),
                r.residualSeverity(), r.residualProbability(), r.decision(), r.status(), r.requirements(),
                r.nextReviewOn(), r.effectivenessCriterion());
    }

    private static RiskRegisterDto.OpportunityCommand commande(RiskRegisterWebDto.OpportunityRequest r) {
        return new RiskRegisterDto.OpportunityCommand(r.title(), r.type(), r.process(), r.site(), r.owner(),
                r.targetDate(), r.context(), r.benefit(), r.origin(), r.originRef(), r.gain(),
                r.feasibility(), r.decision(), r.status(), r.requirements(), r.benefitCriterion());
    }

    private static RiskRegisterDto.ActionCommand commande(RiskRegisterWebDto.ActionRequest r) {
        return new RiskRegisterDto.ActionCommand(r.title(), r.dueDate(), r.status());
    }
}
