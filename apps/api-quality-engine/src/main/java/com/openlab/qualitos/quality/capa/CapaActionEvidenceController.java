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
 * Preuves rattachées à UNE action d'un dossier CAPA (§4.2, ADR 0052).
 *
 * <p>Façade distincte de {@link CapaEvidenceController} — même service, mêmes
 * bornes, mêmes codes de refus — parce que les chemins diffèrent : une pièce de
 * dossier et une pièce d'action ne se listent, ne se déposent ni ne se retirent
 * au même endroit, et confondre les deux rendrait la charge utile ambiguë.
 *
 * <p>Le tenant vient toujours du jeton, via le service (§18.2 #2). Quand le
 * stockage objet est coupé, toutes les routes répondent 503 plutôt que de laisser
 * croire à une action sans pièce.
 */
@RestController
@RequestMapping("/api/v1/capa/cases/{id}")
public class CapaActionEvidenceController {

    private final CapaEvidenceService service;

    public CapaActionEvidenceController(CapaEvidenceService service) {
        this.service = service;
    }

    /**
     * Toutes les pièces d'actions du dossier, en un appel.
     *
     * <p>Le chemin est {@code /action-evidences} et non {@code /actions/evidences}
     * pour ne pas concurrencer {@code /actions/{actionId}} : « evidences » n'est
     * pas un UUID, mais compter sur l'échec de conversion pour arbitrer une route
     * serait fragile.
     */
    @GetMapping("/action-evidences")
    public List<CapaEvidenceDto.ListItem> listAll(@PathVariable UUID id) {
        return service.listForActions(id);
    }

    @PostMapping(path = "/actions/{actionId}/evidences", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CapaEvidenceDto.Response upload(@PathVariable UUID id,
                                           @PathVariable UUID actionId,
                                           @RequestParam("file") MultipartFile file,
                                           @AuthenticationPrincipal Jwt jwt) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new CapaEvidenceValidationException("Missing or empty 'file' part");
        }
        return service.uploadForAction(id, actionId, file.getContentType(), file.getOriginalFilename(),
                file.getBytes(), parseActor(jwt));
    }

    @DeleteMapping("/actions/{actionId}/evidences/{evidenceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @PathVariable UUID actionId,
                       @PathVariable UUID evidenceId, @AuthenticationPrincipal Jwt jwt) {
        service.deleteForAction(id, actionId, evidenceId, parseActor(jwt));
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
