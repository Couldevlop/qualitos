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
 * Les projets APQP d'un client.
 *
 * <p><b>Lire est ouvert à tout utilisateur authentifié, écrire ne l'est pas.</b>
 * Chacun doit pouvoir consulter les programmes en cours pour savoir ce qu'on
 * attend de lui ; ouvrir ou supprimer un projet est un acte de pilotage qualité.
 *
 * <p>Les autorisations sont posées AVANT la lecture du corps (ADR 0065) : le
 * {@code @PreAuthorize} de méthode s'évalue avant que Spring ne désérialise le
 * {@code @RequestBody}.
 */
@RestController
@RequestMapping("/api/v1/apqp/projects")
@PreAuthorize("isAuthenticated()")
@Tag(name = "APQP", description = "Advanced product quality planning projects")
public class ApqpProjectController {

    /**
     * Qui peut ouvrir, corriger ou fermer un projet.
     *
     * <p>Exactement la liste du reste du module. `DIRECTOR_QUALITY` est la forme
     * employée dans les expressions ; `QUALITY_DIRECTOR` n'en est qu'un alias posé
     * par la configuration de sécurité, et l'écrire ici aurait introduit une
     * variante de plus à tenir.
     */
    private static final String ROLES_ECRITURE =
            "hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')";

    private final ApqpProjectService service;

    public ApqpProjectController(ApqpProjectService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "The tenant's APQP projects, with their completion")
    public List<ApqpDto.ProjectResponse> lister() {
        return service.lister();
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "One APQP project")
    public ApqpDto.ProjectResponse lire(@PathVariable UUID projectId) {
        return service.lire(projectId);
    }

    /** Le cycle du référentiel est posé dans la foulée : un projet naît rempli. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Open a project, seeded with the reference cycle")
    public ApqpDto.ProjectResponse creer(@Valid @RequestBody ApqpDto.CreateProjectRequest requete,
                                         @AuthenticationPrincipal Jwt jwt) {
        return service.creer(requete, ApqpActor.de(jwt));
    }

    @PutMapping("/{projectId}")
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Correct a project's heading")
    public ApqpDto.ProjectResponse modifier(@PathVariable UUID projectId,
                                            @Valid @RequestBody
                                            ApqpDto.UpdateProjectRequest requete) {
        return service.modifier(projectId, requete);
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Remove a project, its cycle, its deliverables and their evidence")
    public void supprimer(@PathVariable UUID projectId) {
        service.supprimer(projectId);
    }
}
