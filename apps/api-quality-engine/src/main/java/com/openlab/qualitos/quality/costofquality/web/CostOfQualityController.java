package com.openlab.qualitos.quality.costofquality.web;

import com.openlab.qualitos.quality.costofquality.application.CoqDto;
import com.openlab.qualitos.quality.costofquality.application.CostOfQualityService;
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
 * Le coût de la qualité (modèle PAF), par mois ou par année.
 *
 * <p><b>Lire est ouvert à tout authentifié ; saisir ne l'est pas.</b> Un
 * montant imputé alimente la revue de direction : c'est un acte de pilotage,
 * réservé aux mêmes profils que l'arbitrage des autres référentiels.
 *
 * <p>Les écritures sont transactionnelles ICI, pas dans le service : la ligne
 * et sa trace d'audit sont validées ensemble ou pas du tout, sans que la couche
 * application ait à connaître Spring.
 */
@RestController
@RequestMapping("/api/v1/cost-of-quality")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Cost of quality", description = "PAF cost of quality: prevention, appraisal, failures")
public class CostOfQualityController {

    private static final String ROLES_SAISIE =
            "hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')";

    private final CostOfQualityService service;

    public CostOfQualityController(CostOfQualityService service) {
        this.service = service;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "The report of a month (year + month) or of a whole year (year only)")
    public CoqDto.ReportView report(@RequestParam int year,
                                    @RequestParam(required = false) Integer month) {
        return service.report(year, month);
    }

    @GetMapping("/labels")
    @Transactional(readOnly = true)
    @Operation(summary = "The line labels offered by the drop-down: built-in, then the tenant's own")
    public List<CoqDto.LabelView> labels() {
        return service.labels();
    }

    @PostMapping("/labels")
    @PreAuthorize(ROLES_SAISIE)
    @Transactional
    @Operation(summary = "Add a free-text label; returns the existing one if the name is already taken")
    public CoqDto.LabelView createLabel(@Valid @RequestBody CostOfQualityWebDto.LabelRequest requete) {
        return service.createLabel(new CoqDto.LabelCommand(
                requete.category(), requete.name(), requete.partControl()));
    }

    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ROLES_SAISIE)
    @Transactional
    @Operation(summary = "Record a cost line")
    public CoqDto.LineView record(@Valid @RequestBody CostOfQualityWebDto.EntryRequest requete) {
        return service.record(commande(requete));
    }

    @PutMapping("/entries/{entryId}")
    @PreAuthorize(ROLES_SAISIE)
    @Transactional
    @Operation(summary = "Correct a cost line; changing its date moves it to another month")
    public CoqDto.LineView revise(@PathVariable UUID entryId,
                                  @Valid @RequestBody CostOfQualityWebDto.EntryRequest requete) {
        return service.revise(entryId, commande(requete));
    }

    @DeleteMapping("/entries/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ROLES_SAISIE)
    @Transactional
    @Operation(summary = "Delete a cost line")
    public void delete(@PathVariable UUID entryId) {
        service.delete(entryId);
    }

    @PutMapping("/currency")
    @PreAuthorize(ROLES_SAISIE)
    @Transactional
    @Operation(summary = "Set the display currency (ISO 4217); amounts are not converted")
    public CostOfQualityWebDto.CurrencyView currency(
            @Valid @RequestBody CostOfQualityWebDto.CurrencyRequest requete) {
        return new CostOfQualityWebDto.CurrencyView(service.setCurrency(requete.currency()));
    }

    private static CoqDto.EntryCommand commande(CostOfQualityWebDto.EntryRequest r) {
        return new CoqDto.EntryCommand(r.labelId(), r.amount(), r.responsible(), r.imputationDate(),
                r.comment(), r.partReference(), r.partQuantity(), r.lot(), r.receivedOrMadeOn());
    }
}
