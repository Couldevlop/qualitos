package com.openlab.qualitos.quality.pdca;

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
 * Preuves rattachées aux étapes d'un cycle PDCA (§3.1, ADR 0061).
 *
 * <p>Façade distincte de {@link PdcaController} : ces routes transportent du
 * multipart et non du JSON, et les mêler au contrôleur du cycle aurait obligé
 * ce dernier à connaître le stockage objet.
 *
 * <p>Le tenant vient toujours du jeton, via le service (§18.2 #2). Quand le
 * stockage objet est coupé, toutes les routes répondent 503 plutôt que de
 * laisser croire à une étape sans pièce.
 */
@RestController
@RequestMapping("/api/v1/pdca/cycles/{id}")
public class PdcaStepEvidenceController {

    private final PdcaStepEvidenceService service;

    public PdcaStepEvidenceController(PdcaStepEvidenceService service) {
        this.service = service;
    }

    /**
     * Toutes les pièces d'étapes du cycle, en un appel.
     *
     * <p>Le chemin est {@code /step-evidences} et non {@code /steps/evidences}
     * pour ne pas concurrencer {@code /steps/{stepId}} : « evidences » n'est pas
     * un UUID, mais compter sur l'échec de conversion pour arbitrer une route
     * serait fragile.
     */
    @GetMapping("/step-evidences")
    public List<PdcaStepEvidenceDto.ListItem> list(@PathVariable UUID id) {
        return service.listForCycle(id);
    }

    @PostMapping(path = "/steps/{stepId}/evidences", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PdcaStepEvidenceDto.Response upload(@PathVariable UUID id,
                                               @PathVariable UUID stepId,
                                               @RequestParam("file") MultipartFile file,
                                               @AuthenticationPrincipal Jwt jwt) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new PdcaStepEvidenceValidationException("Missing or empty 'file' part");
        }
        return service.upload(id, stepId, file.getContentType(), file.getOriginalFilename(),
                file.getBytes(), parseActor(jwt));
    }

    @DeleteMapping("/steps/{stepId}/evidences/{evidenceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @PathVariable UUID stepId,
                       @PathVariable UUID evidenceId, @AuthenticationPrincipal Jwt jwt) {
        service.delete(id, stepId, evidenceId, parseActor(jwt));
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
