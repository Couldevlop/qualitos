package com.openlab.qualitos.quality.apqp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Les pièces d'un livrable APQP.
 *
 * <p><b>Lire est ouvert à tout utilisateur authentifié, verser ne l'est pas.</b>
 * Chacun doit pouvoir consulter ce qui prouve un livrable — c'est la méthode de
 * l'organisation — mais verser ou retirer une pièce d'un dossier PPAP est un acte
 * de pilotage qualité.
 */
@RestController
@RequestMapping("/api/v1/apqp/phases/{phaseId}/deliverables/{deliverableId}/evidences")
@PreAuthorize("isAuthenticated()")
@Tag(name = "APQP", description = "Evidence attached to an APQP deliverable")
public class ApqpDeliverableEvidenceController {

    /** Même liste que le reste du module : une variante de plus serait à tenir. */
    private static final String ROLES_ECRITURE =
            "hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','ADMIN_TENANT','SUPER_ADMIN')";

    private final ApqpDeliverableEvidenceService service;

    public ApqpDeliverableEvidenceController(ApqpDeliverableEvidenceService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "The files proving a deliverable, with short-lived read URLs")
    public List<ApqpDeliverableEvidenceDto.ListItem> list(@PathVariable UUID phaseId,
                                                          @PathVariable UUID deliverableId) {
        return service.list(phaseId, deliverableId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Attach a file (docx, xlsx, pdf, image) to a deliverable")
    public ApqpDeliverableEvidenceDto.Response upload(
            @PathVariable UUID phaseId,
            @PathVariable UUID deliverableId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ApqpDeliverableEvidenceValidationException("Missing or empty 'file' part");
        }
        return service.upload(phaseId, deliverableId, file.getContentType(),
                file.getOriginalFilename(), file.getBytes(), parseActor(jwt));
    }

    @DeleteMapping("/{evidenceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ROLES_ECRITURE)
    @Operation(summary = "Remove a file from a deliverable")
    public void delete(@PathVariable UUID phaseId,
                       @PathVariable UUID deliverableId,
                       @PathVariable UUID evidenceId,
                       @AuthenticationPrincipal Jwt jwt) {
        service.delete(phaseId, deliverableId, evidenceId, parseActor(jwt));
    }

    /**
     * L'auteur du dépôt vient du sujet du jeton.
     *
     * <p>Si le sujet n'est pas un UUID, le champ reste vide : mieux vaut une preuve
     * sans auteur qu'un auteur inventé. Jamais lu du formulaire multipart, qui est
     * falsifiable.
     */
    private static UUID parseActor(Jwt jwt) {
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
