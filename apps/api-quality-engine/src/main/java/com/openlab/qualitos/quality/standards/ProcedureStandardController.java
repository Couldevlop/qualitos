package com.openlab.qualitos.quality.standards;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Écriture des référentiels de procédure (§8).
 *
 * <p>OWASP A01 : {@code @PreAuthorize} restreint l'écriture aux rôles qui pilotent
 * le système qualité. Le tenant vient du jeton, jamais du corps (§18.2-2) — la
 * requête ne porte donc que l'identifiant du document source.
 */
@RestController
@RequestMapping("/api/v1/standards")
@PreAuthorize("hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')")
public class ProcedureStandardController {

    private final ProcedureStandardService service;

    public ProcedureStandardController(ProcedureStandardService service) {
        this.service = service;
    }

    public record CreateFromDocumentRequest(@NotNull UUID documentId) {}

    @PostMapping("/from-document")
    public ResponseEntity<Void> createFromDocument(@Valid @RequestBody CreateFromDocumentRequest req) {
        Standard created = service.createFromDocument(req.documentId());
        return ResponseEntity.created(URI.create("/api/v1/standards/" + created.getId())).build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStandard(@PathVariable UUID id) {
        service.deleteStandard(id);
    }

    // ---- Arborescence : Sections → Clauses → Exigences ----
    //
    // Aucune de ces écritures ne rend le nœud créé : l'identifiant d'un enfant
    // n'est attribué qu'à l'écriture en base, après le retour du service. Le
    // client relit donc la fiche — ce qu'il ferait de toute façon pour rester
    // fidèle à l'ordre et aux codes tels que le serveur les a retenus.

    @PostMapping("/{id}/sections")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addSection(@PathVariable UUID id,
                           @Valid @RequestBody ProcedureStandardDto.SectionRequest req) {
        service.addSection(id, req);
    }

    @PatchMapping("/{id}/sections/{sectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateSection(@PathVariable UUID id, @PathVariable UUID sectionId,
                              @Valid @RequestBody ProcedureStandardDto.SectionRequest req) {
        service.updateSection(id, sectionId, req);
    }

    @DeleteMapping("/{id}/sections/{sectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSection(@PathVariable UUID id, @PathVariable UUID sectionId) {
        service.deleteSection(id, sectionId);
    }

    @PostMapping("/{id}/sections/{sectionId}/clauses")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addClause(@PathVariable UUID id, @PathVariable UUID sectionId,
                          @Valid @RequestBody ProcedureStandardDto.ClauseRequest req) {
        service.addClause(id, sectionId, req);
    }

    @PatchMapping("/{id}/clauses/{clauseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateClause(@PathVariable UUID id, @PathVariable UUID clauseId,
                             @Valid @RequestBody ProcedureStandardDto.ClauseRequest req) {
        service.updateClause(id, clauseId, req);
    }

    @DeleteMapping("/{id}/clauses/{clauseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClause(@PathVariable UUID id, @PathVariable UUID clauseId) {
        service.deleteClause(id, clauseId);
    }

    @PostMapping("/{id}/clauses/{clauseId}/requirements")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addRequirement(@PathVariable UUID id, @PathVariable UUID clauseId,
                               @Valid @RequestBody ProcedureStandardDto.RequirementRequest req) {
        service.addRequirement(id, clauseId, req);
    }

    @PatchMapping("/{id}/requirements/{requirementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRequirement(@PathVariable UUID id, @PathVariable UUID requirementId,
                                  @Valid @RequestBody ProcedureStandardDto.RequirementRequest req) {
        service.updateRequirement(id, requirementId, req);
    }

    @DeleteMapping("/{id}/requirements/{requirementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRequirement(@PathVariable UUID id, @PathVariable UUID requirementId) {
        service.deleteRequirement(id, requirementId);
    }
}
