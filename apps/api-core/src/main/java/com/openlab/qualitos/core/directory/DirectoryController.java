package com.openlab.qualitos.core.directory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Annuaire du client en lecture restreinte (ADR 0091).
 *
 * <p>Ouvert à TOUT membre authentifié du client : un manager qualité doit pouvoir
 * désigner le responsable d'un audit, d'une CAPA ou d'un changement sans être
 * administrateur. Distinct de {@code /api/v1/users}, dont les droits ne
 * changent pas : ici, ni rôles, ni invitation, ni compte de connexion.
 *
 * <p>Le client vient du jeton ; aucun paramètre ne permet d'en désigner un autre.
 * Les réponses portent des données personnelles : elles ne se mettent pas en cache
 * partagé.
 */
@RestController
@RequestMapping("/api/v1/directory/users")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Directory", description = "Restricted read-only directory of the current tenant's active members")
public class DirectoryController {

    private final DirectoryService directory;

    public DirectoryController(DirectoryService directory) {
        this.directory = directory;
    }

    @GetMapping
    @Operation(summary = "Search active members of the current tenant by name or e-mail (accent and case insensitive)")
    public ResponseEntity<DirectoryDto.Page> search(
            @Parameter(description = "Text contained in the name or e-mail, 100 characters max")
            @RequestParam(name = "q", required = false) String q,
            @Parameter(description = "Page index, 0 to 999")
            @RequestParam(name = "page", required = false) Integer page,
            @Parameter(description = "Page size, 1 to 50 (default 20)")
            @RequestParam(name = "size", required = false) Integer size) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(directory.search(DirectoryQuery.of(q, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Name of a member of the current tenant (404 when absent from this tenant)")
    public ResponseEntity<DirectoryDto.Entry> get(@PathVariable UUID id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(directory.get(id));
    }
}
