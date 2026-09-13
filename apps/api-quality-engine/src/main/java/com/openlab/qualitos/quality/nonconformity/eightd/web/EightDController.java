package com.openlab.qualitos.quality.nonconformity.eightd.web;

import com.openlab.qualitos.quality.common.StepUpGuard;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDDto;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Le rapport 8D d'une non-conformité.
 *
 * <p><b>Lire est ouvert à tout utilisateur authentifié, écrire ne l'est pas.</b>
 * Le rapport raconte ce que l'organisation a fait d'un écart : l'opérateur qui a
 * signalé le défaut doit pouvoir lire la suite qu'on y a donnée. Le renseigner et
 * surtout l'ÉMETTRE sont autre chose : l'émission produit un document signé,
 * ancré, remis au client — un acte de pilotage qualité.
 *
 * <p>L'autorisation est portée par {@code @PreAuthorize} sur la méthode, donc
 * évaluée AVANT la liaison et la validation du corps (ADR 0065) : sans cela, un
 * appelant sans habilitation recevrait 400 ou 403 selon la charge utile envoyée, et
 * le point d'entrée fermé servirait d'oracle de validation.
 */
@RestController
@RequestMapping("/api/v1/nc/{ncId}/8d")
@PreAuthorize("isAuthenticated()")
@Validated
@Tag(name = "8D", description = "Eight disciplines report extracted at non-conformity closure")
public class EightDController {

    /**
     * Qui peut renseigner et émettre.
     *
     * <p>Exactement la liste des autres référentiels de méthode du module.
     * `DIRECTOR_QUALITY` est la forme employée dans les expressions ;
     * `QUALITY_DIRECTOR` n'en est qu'un alias posé par la configuration de sécurité.
     */
    private static final String ROLES_ECRITURE =
            "hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')";

    private final EightDService service;
    private final StepUpGuard stepUp;

    public EightDController(EightDService service, StepUpGuard stepUp) {
        this.service = service;
        this.stepUp = stepUp;
    }

    @GetMapping
    @Operation(summary = "The 8D report of a non-conformity: eight disciplines, aggregated or sealed")
    public EightDDto.ReportView consulter(@PathVariable UUID ncId) {
        return service.consulter(ncId);
    }

    @PutMapping
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Fill in the three disciplines that have no source (D1, D3, D8)")
    public EightDDto.ReportView enregistrer(@PathVariable UUID ncId,
                                            @Valid @RequestBody EightDWebDto.SaveRequest requete) {
        return service.enregistrer(ncId, new EightDDto.SaveCommand(
                requete.team(), requete.containment(), requete.recognition()));
    }

    /**
     * Émet le rapport : contenu figé, empreinte signée et ancrée.
     *
     * <p>Second facteur exigé, comme pour l'approbation d'un control plan : l'émission
     * produit une preuve opposable, et §18.2 #5 ne laisse pas le choix. Le garde est
     * appelé dans le corps de la méthode plutôt que par une annotation maison : on lit
     * ici que l'action l'exige, et un intercepteur mal configuré ne peut pas le
     * désactiver en silence.
     */
    @PostMapping("/issue")
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Issue the report: freeze the content, sign and anchor its fingerprint")
    public EightDDto.ReportView emettre(@PathVariable UUID ncId) {
        stepUp.require("émettre un rapport 8D");
        return service.emettre(ncId);
    }

    /**
     * Remet le PDF du rapport émis.
     *
     * <p>Ouvert en lecture : le document est la restitution de ce que le tenant a
     * déjà le droit de lire à l'écran, et un auditeur interne doit pouvoir l'obtenir
     * sans habilitation d'écriture.
     */
    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Download the issued report as a signed, anchored PDF")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID ncId) {
        EightDDto.PdfResult resultat = service.pdf(ncId);
        HttpHeaders entetes = new HttpHeaders();
        entetes.setContentType(MediaType.APPLICATION_PDF);
        // C'est le SERVEUR qui nomme le fichier : le refabriquer côté navigateur
        // ferait diverger les deux noms à la première évolution.
        entetes.setContentDispositionFormData("attachment", resultat.fileName());
        entetes.add("X-EightD-Sha256", resultat.sha256Hex());
        entetes.add("X-EightD-Verification-Code", resultat.verificationCode());
        entetes.setContentLength(resultat.pdf().length);
        return new ResponseEntity<>(resultat.pdf(), entetes, 200);
    }
}
