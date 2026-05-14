package com.openlab.qualitos.quality.docs;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) { this.service = service; }

    @GetMapping
    public Page<DocumentDto.DocumentResponse> list(
            @RequestParam(required = false) DocumentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.findAll(status, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDto.DocumentResponse create(@Valid @RequestBody DocumentDto.CreateDocumentRequest req) {
        return service.createDocument(req);
    }

    @GetMapping("/{id}")
    public DocumentDto.DocumentResponse get(@PathVariable UUID id) { return service.findById(id); }

    @PatchMapping("/{id}")
    public DocumentDto.DocumentResponse update(@PathVariable UUID id,
                                               @Valid @RequestBody DocumentDto.UpdateDocumentRequest req) {
        return service.updateDocument(id, req);
    }

    @PatchMapping("/{id}/archive")
    public DocumentDto.DocumentResponse archive(@PathVariable UUID id) {
        return service.archiveDocument(id);
    }

    // versions
    @PostMapping("/{id}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDto.VersionResponse createVersion(
            @PathVariable UUID id, @Valid @RequestBody DocumentDto.CreateVersionRequest req) {
        return service.createVersion(id, req);
    }

    @PatchMapping("/{id}/versions/{vid}")
    public DocumentDto.VersionResponse updateVersion(
            @PathVariable UUID id, @PathVariable UUID vid,
            @RequestBody DocumentDto.UpdateVersionRequest req) {
        return service.updateVersion(id, vid, req);
    }

    @PatchMapping("/{id}/versions/{vid}/submit")
    public DocumentDto.VersionResponse submit(@PathVariable UUID id, @PathVariable UUID vid) {
        return service.submitForReview(id, vid);
    }

    @PatchMapping("/{id}/versions/{vid}/approve")
    public DocumentDto.VersionResponse approve(@PathVariable UUID id, @PathVariable UUID vid,
                                               @Valid @RequestBody DocumentDto.ApprovalRequest req) {
        return service.approveVersion(id, vid, req);
    }

    @PatchMapping("/{id}/versions/{vid}/publish")
    public DocumentDto.VersionResponse publish(@PathVariable UUID id, @PathVariable UUID vid) {
        return service.publishVersion(id, vid);
    }

    @PatchMapping("/{id}/versions/{vid}/blockchain")
    public DocumentDto.VersionResponse setBlockchainTx(
            @PathVariable UUID id, @PathVariable UUID vid,
            @RequestBody Map<String, String> body) {
        return service.setBlockchainTx(id, vid, body.get("txHash"));
    }

    // acknowledgments
    @PostMapping("/{id}/versions/{vid}/acknowledge")
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
