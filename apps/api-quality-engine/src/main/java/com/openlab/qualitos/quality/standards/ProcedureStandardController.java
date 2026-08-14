package com.openlab.qualitos.quality.standards;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
