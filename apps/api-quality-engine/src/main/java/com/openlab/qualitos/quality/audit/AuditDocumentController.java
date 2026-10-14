package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.common.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Documents joints à un audit (§4.4). Le tenant et l'auteur viennent du jeton
 * (§18.2 #2). Stockage objet coupé : toutes les routes répondent 503 plutôt que
 * de laisser croire à un audit sans document.
 *
 * <p>Joindre est ouvert à tout authentifié, comme le reste de la saisie d'audit ;
 * retirer tombe sous la règle générale des suppressions (manager qualité ou
 * administrateur, cf. {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/v1/audits/plans/{id}/documents")
public class AuditDocumentController {

    private final AuditDocumentService service;

    public AuditDocumentController(AuditDocumentService service) {
        this.service = service;
    }

    @GetMapping
    public List<AuditDocumentDto.ListItem> list(@PathVariable UUID id) {
        return service.list(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AuditDocumentDto.Response upload(@PathVariable UUID id,
                                            @RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new AuditDocumentValidationException("Missing or empty 'file' part");
        }
        // Un sujet non exploitable laisse l'auteur vide : mieux vaut un document
        // sans auteur qu'un auteur inventé.
        return service.upload(id, file.getContentType(), file.getOriginalFilename(),
                file.getBytes(), CurrentUser.userId().orElse(null));
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @PathVariable UUID documentId) {
        service.delete(id, documentId, CurrentUser.userId().orElse(null));
    }
}
