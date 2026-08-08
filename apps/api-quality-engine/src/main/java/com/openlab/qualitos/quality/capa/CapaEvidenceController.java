package com.openlab.qualitos.quality.capa;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Preuves jointes à un dossier CAPA (§4.2). Le tenant vient toujours du jeton,
 * via le service (§18.2 #2). Quand le stockage objet est coupé, toutes les routes
 * répondent 503 plutôt que de laisser croire à un dossier sans pièce.
 */
@RestController
@RequestMapping("/api/v1/capa/cases/{id}/evidences")
public class CapaEvidenceController {

    private final CapaEvidenceService service;

    public CapaEvidenceController(CapaEvidenceService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CapaEvidenceDto.Response upload(@PathVariable UUID id,
                                           @RequestParam("file") MultipartFile file,
                                           @AuthenticationPrincipal Jwt jwt) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new CapaEvidenceValidationException("Missing or empty 'file' part");
        }
        return service.upload(id, file.getContentType(), file.getOriginalFilename(),
                file.getBytes(), parseActor(jwt));
    }

    @GetMapping
    public List<CapaEvidenceDto.ListItem> list(@PathVariable UUID id) {
        return service.list(id);
    }

    @DeleteMapping("/{evidenceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @PathVariable UUID evidenceId,
                       @AuthenticationPrincipal Jwt jwt) {
        service.delete(id, evidenceId, parseActor(jwt));
    }

    /**
     * L'auteur du dépôt vient du sujet du jeton. Si le sujet n'est pas un UUID,
     * on laisse le champ vide : mieux vaut une preuve sans auteur qu'un auteur
     * inventé, qu'un audit prendrait pour argent comptant.
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
