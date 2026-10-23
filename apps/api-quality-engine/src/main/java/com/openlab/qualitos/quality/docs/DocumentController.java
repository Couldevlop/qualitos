package com.openlab.qualitos.quality.docs;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService service;
    private final DocumentFileService files;

    public DocumentController(DocumentService service, DocumentFileService files) {
        this.service = service;
        this.files = files;
    }

    /**
     * La liste de la GED (ADR 0094). {@code status} reste accepté pour les appels
     * d'avant les vues ; {@code view} le remplace dès qu'il est fourni.
     */
    @GetMapping
    public Page<DocumentDto.DocumentResponse> list(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) DocumentView view,
            @RequestParam(required = false) UUID folderId,
            @RequestParam(required = false) @Size(max = 200) String q,
            @RequestParam(required = false) DisplayStatus displayStatus,
            @RequestParam(required = false) DocumentType type,
            @RequestParam(required = false) UUID processId,
            @RequestParam(required = false) UUID ownerId,
            @PageableDefault(size = 20) Pageable pageable) {
        boolean anyNewFilter = view != null || folderId != null || q != null || displayStatus != null
                || type != null || processId != null || ownerId != null;
        if (!anyNewFilter) {
            return service.findAll(status, pageable);
        }
        DocumentView effective = view != null ? view : DocumentDto.ListFilter.of(status).view();
        return service.search(new DocumentDto.ListFilter(effective, folderId, q, displayStatus, type,
                processId, ownerId), pageable);
    }

    /** Les compteurs des vues rapides. */
    @GetMapping("/views")
    public DocumentDto.ViewCounts views() {
        return service.viewCounts();
    }

    /**
     * Un lien présigné vers le fichier d'une version, valable dix minutes :
     * {@code download=true} pour l'enregistrer, sinon pour l'aperçu.
     */
    @GetMapping("/{id}/versions/{vid}/file")
    public DocumentDto.FileLink fileLink(@PathVariable UUID id, @PathVariable UUID vid,
                                         @RequestParam(defaultValue = "false") boolean download) {
        return files.link(id, vid, download);
    }

    @PostMapping
    @RequiresPermission(Permission.DOCUMENT_EDIT)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDto.DocumentResponse create(@Valid @RequestBody DocumentDto.CreateDocumentRequest req) {
        return service.createDocument(req);
    }

    @GetMapping("/{id}")
    public DocumentDto.DocumentResponse get(@PathVariable UUID id) { return service.findById(id); }

    @PatchMapping("/{id}")
    @RequiresPermission(Permission.DOCUMENT_EDIT)
    public DocumentDto.DocumentResponse update(@PathVariable UUID id,
                                               @Valid @RequestBody DocumentDto.UpdateDocumentRequest req) {
        return service.updateDocument(id, req);
    }

    @PatchMapping("/{id}/archive")
    @RequiresPermission(Permission.DOCUMENT_EDIT)
    public DocumentDto.DocumentResponse archive(@PathVariable UUID id) {
        return service.archiveDocument(id);
    }

    // versions
    @PostMapping("/{id}/versions")
    @RequiresPermission(Permission.DOCUMENT_EDIT)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDto.VersionResponse createVersion(
            @PathVariable UUID id, @Valid @RequestBody DocumentDto.CreateVersionRequest req) {
        return service.createVersion(id, req);
    }

    @PatchMapping("/{id}/versions/{vid}")
    @RequiresPermission(Permission.DOCUMENT_EDIT)
    public DocumentDto.VersionResponse updateVersion(
            @PathVariable UUID id, @PathVariable UUID vid,
            @RequestBody DocumentDto.UpdateVersionRequest req) {
        return service.updateVersion(id, vid, req);
    }

    /** La soumission désigne le circuit de signature : un à trois signataires, dans l'ordre (ADR 0099). */
    @PatchMapping("/{id}/versions/{vid}/submit")
    @RequiresPermission(Permission.DOCUMENT_SUBMIT)
    public DocumentDto.VersionResponse submit(@PathVariable UUID id, @PathVariable UUID vid,
                                              @Valid @RequestBody DocumentDto.SubmitRequest req) {
        return service.submitForReview(id, vid, req);
    }

    /**
     * Signer et refuser ne passent pas par un droit de rôle : la désignation fait
     * le droit, et le service vérifie que c'est le tour de l'appelant (ADR 0099).
     * Une version soumise avant le circuit de signature exige encore
     * {@code document.approve}, vérifié par le service.
     */
    @PatchMapping("/{id}/versions/{vid}/approve")
    public DocumentDto.VersionResponse approve(@PathVariable UUID id, @PathVariable UUID vid,
                                               @Valid @RequestBody DocumentDto.ApprovalRequest req) {
        return service.approveVersion(id, vid, req);
    }

    @PatchMapping("/{id}/versions/{vid}/reject")
    public DocumentDto.VersionResponse reject(@PathVariable UUID id, @PathVariable UUID vid,
                                              @Valid @RequestBody DocumentDto.RejectionRequest req) {
        return service.rejectVersion(id, vid, req);
    }

    @PatchMapping("/{id}/versions/{vid}/publish")
    @RequiresPermission(Permission.DOCUMENT_PUBLISH)
    public DocumentDto.VersionResponse publish(@PathVariable UUID id, @PathVariable UUID vid) {
        return service.publishVersion(id, vid);
    }

    @PatchMapping("/{id}/versions/{vid}/blockchain")
    @RequiresPermission(Permission.DOCUMENT_PUBLISH)
    public DocumentDto.VersionResponse setBlockchainTx(
            @PathVariable UUID id, @PathVariable UUID vid,
            @RequestBody Map<String, String> body) {
        return service.setBlockchainTx(id, vid, body.get("txHash"));
    }

    // acknowledgments
    @PostMapping("/{id}/versions/{vid}/acknowledge")
    @RequiresPermission(Permission.DOCUMENT_ACKNOWLEDGE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDto.AcknowledgmentResponse acknowledge(
            @PathVariable UUID id, @PathVariable UUID vid,
            @Valid @RequestBody DocumentDto.AcknowledgeRequest req) {
        return service.acknowledge(id, vid, req);
    }

    @GetMapping("/{id}/versions/{vid}/acknowledgments/count")
    public Map<String, Long> countAcks(@PathVariable UUID id, @PathVariable UUID vid) {
        return Map.of("count", service.countAcknowledgments(id, vid));
    }
}
