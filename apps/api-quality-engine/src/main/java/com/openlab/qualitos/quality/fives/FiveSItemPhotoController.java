package com.openlab.qualitos.quality.fives;

import com.openlab.qualitos.quality.common.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Les photos de terrain d'un audit 5S.
 *
 * <p><b>Ouvert à tout utilisateur authentifié, dépôt compris.</b> C'est une
 * saisie de terrain : celui qui parcourt l'atelier est un opérateur, pas un
 * manager qualité, et réserver la prise de vue au pilotage reviendrait à
 * supprimer la fonction. Même choix que la notation des piliers et que les
 * photos de non-conformité.
 *
 * <p>Le pilier est dans le chemin, pas la ligne de notation : hors ligne, cette
 * ligne n'a pas encore d'identifiant serveur (voir {@link FiveSItemPhotoService}).
 */
@RestController
@RequestMapping("/api/v1/fives/audits/{auditId}/photos")
@PreAuthorize("isAuthenticated()")
@Tag(name = "5S", description = "Field photos attached to the pillars of a 5S audit")
public class FiveSItemPhotoController {

    private final FiveSItemPhotoService service;

    public FiveSItemPhotoController(FiveSItemPhotoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "The field photos of the audit, with short-lived read URLs")
    public List<FiveSItemPhotoDto.ListItem> list(@PathVariable UUID auditId,
                                                 @RequestParam(required = false) FiveSPillar pillar) {
        return pillar == null
                ? service.listForAudit(auditId)
                : service.listForPillar(auditId, pillar);
    }

    @PostMapping(path = "/{pillar}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Attach a photo (jpeg, png, webp, heic) to a scored pillar")
    public FiveSItemPhotoDto.Response upload(@PathVariable UUID auditId,
                                             @PathVariable FiveSPillar pillar,
                                             @RequestParam("image") MultipartFile image)
            throws IOException {
        if (image == null || image.isEmpty()) {
            throw new FiveSItemPhotoValidationException("Missing or empty 'image' part");
        }
        return service.upload(auditId, pillar, image.getContentType(),
                image.getOriginalFilename(), image.getBytes(),
                CurrentUser.userId().orElse(null));
    }

    @DeleteMapping("/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a photo from a 5S audit that is still open")
    public void delete(@PathVariable UUID auditId, @PathVariable UUID photoId) {
        service.delete(auditId, photoId, CurrentUser.userId().orElse(null));
    }
}
