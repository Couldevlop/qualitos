package com.openlab.qualitos.quality.standards;

import jakarta.validation.Valid;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/standards")
public class StandardsController {

    private final StandardsService service;
    private final CertificationDossierService dossierService;
    private final CertificationBlancService certificationBlancService;
    private final AiDraftService aiDraftService;

    public StandardsController(StandardsService service,
                               CertificationDossierService dossierService,
                               CertificationBlancService certificationBlancService,
                               AiDraftService aiDraftService) {
        this.service = service;
        this.dossierService = dossierService;
        this.certificationBlancService = certificationBlancService;
        this.aiDraftService = aiDraftService;
    }

    // ---- Catalog ----

    @GetMapping
    public Page<StandardsDto.StandardSummary> list(
            @RequestParam(required = false) StandardStatus status,
            @RequestParam(required = false) String family,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.listStandards(status, family, pageable);
    }

    @GetMapping("/{id}")
    public StandardsDto.StandardDetail get(@PathVariable UUID id) {
        return service.getStandard(id);
    }

    @GetMapping("/by-code/{code}")
    public StandardsDto.StandardDetail getByCode(@PathVariable String code) {
        return service.getStandardByCode(code);
    }

    // ---- Adoptions ----

    @GetMapping("/adoptions")
    public Page<StandardsDto.AdoptionResponse> listAdoptions(
            @RequestParam(required = false) AdoptionStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.listAdoptions(status, pageable);
    }

    @PostMapping("/adoptions")
    @ResponseStatus(HttpStatus.CREATED)
    public StandardsDto.AdoptionResponse adopt(@Valid @RequestBody StandardsDto.AdoptRequest request) {
        return service.adopt(request);
    }

    @GetMapping("/adoptions/{id}")
    public StandardsDto.AdoptionResponse getAdoption(@PathVariable UUID id) {
        return service.getAdoption(id);
    }

    @PatchMapping("/adoptions/{id}")
    public StandardsDto.AdoptionResponse update(@PathVariable UUID id,
                                                @Valid @RequestBody StandardsDto.UpdateAdoptionRequest request) {
        return service.updateAdoption(id, request);
    }

    @PatchMapping("/adoptions/{id}/start")
    public StandardsDto.AdoptionResponse start(@PathVariable UUID id) {
        return service.startProgress(id);
    }

    @PatchMapping("/adoptions/{id}/certify")
    public StandardsDto.AdoptionResponse certify(@PathVariable UUID id,
                                                 @Valid @RequestBody StandardsDto.CertifyRequest request) {
        return service.certify(id, request);
    }

    @PatchMapping("/adoptions/{id}/surveillance")
    public StandardsDto.AdoptionResponse markSurveillance(@PathVariable UUID id) {
        return service.markSurveillance(id);
    }

    @PatchMapping("/adoptions/{id}/withdraw")
    public StandardsDto.AdoptionResponse withdraw(@PathVariable UUID id) {
        return service.withdraw(id);
    }

    @DeleteMapping("/adoptions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.deleteAdoption(id);
    }

    // ---- Evidence ----

    @PostMapping("/adoptions/{id}/evidence")
    @ResponseStatus(HttpStatus.CREATED)
    public StandardsDto.EvidenceResponse linkEvidence(
            @PathVariable UUID id,
            @Valid @RequestBody StandardsDto.LinkEvidenceRequest request) {
        return service.linkEvidence(id, request);
    }

    @GetMapping("/adoptions/{id}/evidence")
    public List<StandardsDto.EvidenceResponse> listEvidence(@PathVariable UUID id) {
        return service.listEvidence(id);
    }

    @DeleteMapping("/adoptions/{id}/evidence/{eid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvidence(@PathVariable UUID id, @PathVariable UUID eid) {
        service.deleteEvidence(id, eid);
    }

    // ---- Alignment ----

    @GetMapping("/adoptions/{id}/alignment")
    public StandardsDto.AlignmentReport alignment(@PathVariable UUID id) {
        return service.computeAlignment(id);
    }

    // ---- Audit blanc (§8.7) ----

    @GetMapping("/adoptions/{id}/audit-blanc")
    public StandardsDto.AuditBlancReport auditBlanc(@PathVariable UUID id) {
        return service.computeAuditBlanc(id);
    }

    // ---- Roadmap de certification (§8.5) ----

    @GetMapping("/adoptions/{id}/roadmap")
    public StandardsDto.RoadmapSummary roadmap(@PathVariable UUID id) {
        return service.getRoadmap(id);
    }

    @PatchMapping("/adoptions/{id}/roadmap/{stageId}")
    public StandardsDto.RoadmapStageResponse updateStage(
            @PathVariable UUID id,
            @PathVariable UUID stageId,
            @Valid @RequestBody StandardsDto.UpdateStageRequest request) {
        return service.updateStage(id, stageId, request);
    }

    // ---- Dossier de certification (§8.4 onglet 6) ----
    // POST : la génération produit un artefact ancré (effet de bord), d'où POST.

    @PostMapping("/adoptions/{id}/dossier")
    public StandardsDto.DossierResponse generateDossier(@PathVariable UUID id) {
        return dossierService.generate(id);
    }

    // ---- Certification à blanc (§8.5 étapes 14-15 ; ISO/IEC 17021-1) ----
    // POST : la simulation produit un verdict signé + ancré (effet de bord), d'où POST.

    @PostMapping("/adoptions/{id}/certification-blanc")
    public StandardsDto.CertificationBlancReport certificationBlanc(@PathVariable UUID id) {
        return certificationBlancService.simulate(id);
    }

    // ---- Catalogue : bibliothèque documentaire / processus / veille (§8.4) ----
    // Lecture du référentiel platform-level (par standardId).

    @GetMapping("/{id}/document-templates")
    public List<StandardsDto.DocumentTemplateResponse> documentTemplates(@PathVariable UUID id) {
        return service.listDocumentTemplates(id);
    }

    @GetMapping("/{id}/document-templates/{templateId}/download")
    public ResponseEntity<Resource> downloadDocumentTemplate(
            @PathVariable UUID id, @PathVariable UUID templateId) {
        String uri = service.resolveTemplateUri(id, templateId);
        String path = uri.startsWith("/") ? uri.substring(1) : uri;
        // Défense en profondeur : confiner au dossier des modèles (OWASP A01/A03).
        if (path.contains("..") || !path.startsWith("standards/templates/")) {
            return ResponseEntity.notFound().build();
        }
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        String filename = path.substring(path.lastIndexOf('/') + 1);
        MediaType type = filename.endsWith(".md")
                ? MediaType.parseMediaType("text/markdown;charset=UTF-8")
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/process-templates")
    public List<StandardsDto.ProcessTemplateResponse> processTemplates(@PathVariable UUID id) {
        return service.listProcessTemplates(id);
    }

    // Génération IA d'un brouillon de document (POST : effet de bord côté LLM). §8.8
    @PostMapping("/{id}/document-templates/{templateId}/ai-draft")
    public StandardsDto.AiDraftResponse aiDraft(@PathVariable UUID id, @PathVariable UUID templateId) {
        return aiDraftService.generate(id, templateId);
    }

    @GetMapping("/{id}/revisions")
    public List<StandardsDto.RevisionResponse> revisions(@PathVariable UUID id) {
        return service.listRevisions(id);
    }
}
