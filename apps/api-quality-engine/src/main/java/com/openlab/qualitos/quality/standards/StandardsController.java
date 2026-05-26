package com.openlab.qualitos.quality.standards;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/standards")
public class StandardsController {

    private final StandardsService service;
    private final CertificationDossierService dossierService;
    private final CertificationBlancService certificationBlancService;

    public StandardsController(StandardsService service,
                               CertificationDossierService dossierService,
                               CertificationBlancService certificationBlancService) {
        this.service = service;
        this.dossierService = dossierService;
        this.certificationBlancService = certificationBlancService;
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
}
