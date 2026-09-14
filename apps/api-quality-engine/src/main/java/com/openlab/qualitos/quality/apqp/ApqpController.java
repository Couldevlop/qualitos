package com.openlab.qualitos.quality.apqp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
 * Le cycle APQP d'un PROJET.
 *
 * <p><b>Lire est ouvert à tout utilisateur authentifié, écrire ne l'est pas.</b>
 * Le cycle décrit la méthode de l'organisation : chacun doit pouvoir la
 * consulter pour savoir ce qu'on attend de lui, mais la refondre est un acte de
 * pilotage qualité. Un opérateur qui supprimerait une phase effacerait ses
 * livrables avec elle.
 *
 * <p>Le projet est dans le CHEMIN et le client dans le jeton : les deux filtres
 * s'appliquent ensemble à chaque opération, si bien qu'un identifiant de phase
 * emprunté à un autre projet — ou à un autre client — rend 404 et non 403 (OWASP
 * A01 : ne rien dire de l'existence de la ressource).
 */
@RestController
@RequestMapping("/api/v1/apqp/projects/{projectId}/phases")
@PreAuthorize("isAuthenticated()")
@Tag(name = "APQP", description = "Advanced product quality planning cycle")
public class ApqpController {

    /**
     * Qui peut refondre le cycle.
     *
     * <p>Exactement la liste des quatorze autres référentiels de méthode de ce
     * module. `DIRECTOR_QUALITY` est la forme employée dans les expressions ;
     * `QUALITY_DIRECTOR` n'en est qu'un alias posé par la configuration de
     * sécurité, et l'écrire ici aurait introduit une variante de plus à tenir.
     */
    private static final String ROLES_ECRITURE =
            "hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')";

    private final ApqpService service;

    public ApqpController(ApqpService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "A project's APQP cycle and PPAP completion")
    public ApqpDto.CycleResponse cycle(@PathVariable UUID projectId) {
        return service.cycle(projectId);
    }

    /**
     * Rend au projet le cycle du référentiel, en effaçant le sien.
     *
     * <p>Destructif : c'est pourquoi l'écran le demande deux fois. Sans cette
     * porte, un projet dont le cycle a été adapté n'aurait aucun moyen d'adopter
     * la nouvelle liste, sinon en supprimant ses phases une à une.
     */
    @PostMapping("/reset")
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Discard the project's cycle and seed it again from the reference")
    public ApqpDto.CycleResponse reinitialiser(@PathVariable UUID projectId) {
        return service.reinitialiser(projectId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Add a phase at the end of the cycle")
    public ApqpDto.PhaseResponse creer(@PathVariable UUID projectId,
                                       @Valid @RequestBody ApqpDto.CreatePhaseRequest requete) {
        return service.creerPhase(projectId, requete);
    }

    @PutMapping("/{phaseId}")
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Rename a phase or restate what it establishes")
    public ApqpDto.PhaseResponse modifier(
            @PathVariable UUID projectId,
            @PathVariable UUID phaseId,
            @Valid @RequestBody ApqpDto.UpdatePhaseRequest requete) {
        return service.modifierPhase(projectId, phaseId, requete);
    }

    @DeleteMapping("/{phaseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Remove a phase and its deliverables, then close the ranks")
    public void supprimer(@PathVariable UUID projectId, @PathVariable UUID phaseId) {
        service.supprimerPhase(projectId, phaseId);
    }

    /**
     * Réorganise le cycle entier.
     *
     * <p>{@code PUT} sur la collection et non sur chaque phase : l'ordre est une
     * propriété de l'ensemble, et une suite de déplacements unitaires laisserait
     * le V dans des états intermédiaires qui n'ont pas de sens.
     */
    @PutMapping("/order")
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Reorder the whole cycle")
    public List<ApqpDto.PhaseResponse> reorganiser(
            @PathVariable UUID projectId,
            @RequestBody ApqpDto.ReorderRequest requete) {
        return service.reorganiser(projectId, requete);
    }

    @PostMapping("/{phaseId}/deliverables")
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Add a deliverable to a phase")
    public ApqpDto.PhaseResponse ajouterLivrable(
            @PathVariable UUID projectId,
            @PathVariable UUID phaseId,
            @Valid @RequestBody ApqpDto.DeliverableRequest requete) {
        return service.ajouterLivrable(projectId, phaseId, requete);
    }

    @PutMapping("/{phaseId}/deliverables/{deliverableId}")
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Reword a deliverable or restate the artifact it must produce")
    public ApqpDto.PhaseResponse modifierLivrable(
            @PathVariable UUID projectId,
            @PathVariable UUID phaseId,
            @PathVariable UUID deliverableId,
            @Valid @RequestBody ApqpDto.DeliverableRequest requete) {
        return service.modifierLivrable(projectId, phaseId, deliverableId, requete);
    }

    /**
     * Déclare où en est un livrable — le formulaire UNIQUE (ADR 0072).
     *
     * <p>Rend le cycle entier : cocher un livrable change le compte du dossier
     * PPAP. L'acteur vient du jeton, jamais du corps.
     */
    @PutMapping("/{phaseId}/deliverables/{deliverableId}/completion")
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Declare where a deliverable stands, and what proves it")
    public ApqpDto.CycleResponse completerLivrable(
            @PathVariable UUID projectId,
            @PathVariable UUID phaseId,
            @PathVariable UUID deliverableId,
            @Valid @RequestBody ApqpDto.CompletionRequest requete,
            @AuthenticationPrincipal Jwt jwt) {
        return service.completerLivrable(
                projectId, phaseId, deliverableId, requete, ApqpActor.de(jwt));
    }

    @DeleteMapping("/{phaseId}/deliverables/{deliverableId}")
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Remove a deliverable and close the ranks")
    public ApqpDto.PhaseResponse supprimerLivrable(
            @PathVariable UUID projectId,
            @PathVariable UUID phaseId,
            @PathVariable UUID deliverableId) {
        return service.supprimerLivrable(projectId, phaseId, deliverableId);
    }
}
