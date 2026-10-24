package com.openlab.qualitos.quality.supplier;

import com.openlab.qualitos.quality.common.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Documents joints aux NC fournisseur (8D, preuves de CAPA) et aux certificats.
 * Le tenant et l'auteur viennent du jeton (§18.2 #2). Stockage objet coupé :
 * toutes les routes répondent 503 plutôt que de laisser croire à un dossier vide.
 *
 * <p>Joindre est ouvert à tout authentifié, comme la saisie d'une NC ; retirer
 * tombe sous la règle générale des suppressions (manager qualité ou
 * administrateur, cf. {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierDocumentController {

    private final SupplierDocumentService service;

    public SupplierDocumentController(SupplierDocumentService service) {
        this.service = service;
    }

    /**
     * Tous les documents fournisseur du tenant — la vue « Documents fournisseurs »
     * de la GED. Le segment littéral l'emporte sur {@code /{id}}.
     */
    @GetMapping("/documents")
    public Page<SupplierDocumentDto.CatalogItem> listAll(
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) SupplierDocumentOwnerType ownerType,
            @RequestParam(required = false) SupplierDocumentCategory category,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.listAll(supplierId, ownerType, category, q, pageable);
    }

    // ---- NC ----

    @GetMapping("/{id}/non-conformities/{ncId}/documents")
    public List<SupplierDocumentDto.ListItem> listNc(@PathVariable UUID id, @PathVariable UUID ncId) {
        return service.list(id, SupplierDocumentOwnerType.NON_CONFORMITY, ncId);
    }

    @PostMapping(value = "/{id}/non-conformities/{ncId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierDocumentDto.Response uploadNc(@PathVariable UUID id, @PathVariable UUID ncId,
                                                 @RequestParam(required = false) SupplierDocumentCategory category,
                                                 @RequestParam("file") MultipartFile file) throws IOException {
        return upload(id, SupplierDocumentOwnerType.NON_CONFORMITY, ncId, category, file);
    }

    @DeleteMapping("/{id}/non-conformities/{ncId}/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNc(@PathVariable UUID id, @PathVariable UUID ncId, @PathVariable UUID documentId) {
        service.delete(id, SupplierDocumentOwnerType.NON_CONFORMITY, ncId, documentId,
                CurrentUser.userId().orElse(null));
    }

    // ---- Certificats ----

    @GetMapping("/{id}/certificates/{certId}/documents")
    public List<SupplierDocumentDto.ListItem> listCert(@PathVariable UUID id, @PathVariable UUID certId) {
        return service.list(id, SupplierDocumentOwnerType.CERTIFICATE, certId);
    }

    @PostMapping(value = "/{id}/certificates/{certId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierDocumentDto.Response uploadCert(@PathVariable UUID id, @PathVariable UUID certId,
                                                   @RequestParam(required = false) SupplierDocumentCategory category,
                                                   @RequestParam("file") MultipartFile file) throws IOException {
        return upload(id, SupplierDocumentOwnerType.CERTIFICATE, certId, category, file);
    }

    @DeleteMapping("/{id}/certificates/{certId}/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCert(@PathVariable UUID id, @PathVariable UUID certId, @PathVariable UUID documentId) {
        service.delete(id, SupplierDocumentOwnerType.CERTIFICATE, certId, documentId,
                CurrentUser.userId().orElse(null));
    }

    private SupplierDocumentDto.Response upload(UUID supplierId, SupplierDocumentOwnerType ownerType, UUID ownerId,
                                                SupplierDocumentCategory category, MultipartFile file)
            throws IOException {
        if (file == null || file.isEmpty()) {
            throw new SupplierDocumentValidationException("Missing or empty 'file' part");
        }
        // Un sujet non exploitable laisse l'auteur vide : mieux vaut un document
        // sans auteur qu'un auteur inventé.
        return service.upload(supplierId, ownerType, ownerId, category, file.getContentType(),
                file.getOriginalFilename(), file.getBytes(), CurrentUser.userId().orElse(null));
    }
}
