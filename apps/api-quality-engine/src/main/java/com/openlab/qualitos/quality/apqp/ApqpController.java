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
 * Le cycle APQP d'un client.
 *
 * <p><b>Lire est ouvert à tout utilisateur authentifié, écrire ne l'est pas.</b>
 * Le cycle décrit la méthode de l'organisation : chacun doit pouvoir la
 * consulter pour savoir ce qu'on attend de lui, mais la refondre est un acte de
 * pilotage qualité. Un opérateur qui supprimerait une phase effacerait ses
 * livrables avec elle.
 */
@RestController
@RequestMapping("/api/v1/apqp/phases")
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
    @Operation(summary = "The tenant's APQP cycle and PPAP completion, seeded on first read")
    public ApqpDto.CycleResponse cycle() {
        return service.cycle();
    }

    /**
     * Rend au client le cycle du référentiel, en effaçant le sien.
     *
     * <p>Destructif : c'est pourquoi l'écran le demande deux fois. Sans cette
     * porte, un client dont le cycle a été laissé intact par la reprise n'aurait
     * aucun moyen d'adopter la nouvelle liste, sinon en supprimant ses phases une
     * à une.
     */
    @PostMapping("/reset")
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Discard the tenant's cycle and seed it again from the reference")
    public ApqpDto.CycleResponse reinitialiser() {
        return service.reinitialiser();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Add a phase at the end of the cycle")
    public ApqpDto.PhaseResponse creer(@Valid @RequestBody ApqpDto.CreatePhaseRequest requete) {
        return service.creerPhase(requete);
    }

    @PutMapping("/{phaseId}")
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Rename a phase or restate what it establishes")
    public ApqpDto.PhaseResponse modifier(
            @PathVariable UUID phaseId,
            @Valid @RequestBody ApqpDto.UpdatePhaseRequest requete) {
        return service.modifierPhase(phaseId, requete);
    }

    @DeleteMapping("/{phaseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Remove a phase and its deliverables, then close the ranks")
    public void supprimer(@PathVariable UUID phaseId) {
        service.supprimerPhase(phaseId);
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
    public List<ApqpDto.PhaseResponse> reorganiser(@RequestBody ApqpDto.ReorderRequest requete) {
        return service.reorganiser(requete);
    }

    @PostMapping("/{phaseId}/deliverables")
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Add a deliverable to a phase")
    public ApqpDto.PhaseResponse ajouterLivrable(
            @PathVariable UUID phaseId,
            @Valid @RequestBody ApqpDto.DeliverableRequest requete) {
        return service.ajouterLivrable(phaseId, requete);
    }

    @PutMapping("/{phaseId}/deliverables/{deliverableId}")
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Reword a deliverable")
    public ApqpDto.PhaseResponse modifierLivrable(
            @PathVariable UUID phaseId,
            @PathVariable UUID deliverableId,
            @Valid @RequestBody ApqpDto.DeliverableRequest requete) {
        return service.modifierLivrable(phaseId, deliverableId, requete);
    }

    /**
     * Déclare où en est un livrable.
     *
     * <p>Rend le cycle entier : cocher un livrable change le compte du dossier
     * PPAP affiché sous le schéma. L'acteur vient du jeton, jamais du corps.
     */
    @PutMapping("/{phaseId}/deliverables/{deliverableId}/completion")
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Declare where a deliverable stands, and what proves it")
    public ApqpDto.CycleResponse completerLivrable(
            @PathVariable UUID phaseId,
            @PathVariable UUID deliverableId,
            @Valid @RequestBody ApqpDto.CompletionRequest requete,
            @AuthenticationPrincipal Jwt jwt) {
        return service.completerLivrable(phaseId, deliverableId, requete, acteur(jwt));
    }

    @DeleteMapping("/{phaseId}/deliverables/{deliverableId}")
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Remove a deliverable and close the ranks")
    public ApqpDto.PhaseResponse supprimerLivrable(
            @PathVariable UUID phaseId,
            @PathVariable UUID deliverableId) {
        return service.supprimerLivrable(phaseId, deliverableId);
    }

    /**
     * Qui coche, d'après le sujet du jeton.
     *
     * <p>Si le sujet n'est pas un UUID, le champ reste vide : mieux vaut un
     * achèvement sans auteur qu'un auteur inventé. Jamais lu du corps, qui est
     * falsifiable.
     */
    private static UUID acteur(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            return null;
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
