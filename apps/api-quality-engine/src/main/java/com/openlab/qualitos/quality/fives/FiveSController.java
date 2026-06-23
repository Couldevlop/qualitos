package com.openlab.qualitos.quality.fives;

import com.openlab.qualitos.quality.visiongateway.VisionDto;
import com.openlab.qualitos.quality.visiongateway.VisionGatewayClient;
import com.openlab.qualitos.quality.visiongateway.VisionImageValidator;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fives")
public class FiveSController {

    private final FiveSService service;
    private final VisionGatewayClient visionGateway;

    public FiveSController(FiveSService service, VisionGatewayClient visionGateway) {
        this.service = service;
        this.visionGateway = visionGateway;
    }

    @GetMapping("/audits")
    public Page<FiveSDto.AuditResponse> list(
            @RequestParam(required = false) FiveSAuditStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.findAll(status, pageable);
    }

    @PostMapping("/audits")
    @ResponseStatus(HttpStatus.CREATED)
    public FiveSDto.AuditResponse create(@Valid @RequestBody FiveSDto.CreateAuditRequest request) {
        return service.createAudit(request);
    }

    @GetMapping("/audits/{id}")
    public FiveSDto.AuditResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PatchMapping("/audits/{id}")
    public FiveSDto.AuditResponse update(@PathVariable UUID id,
                                         @Valid @RequestBody FiveSDto.UpdateAuditRequest request) {
        return service.updateAudit(id, request);
    }

    @PatchMapping("/audits/{id}/start")
    public FiveSDto.AuditResponse start(@PathVariable UUID id) { return service.startAudit(id); }

    @PatchMapping("/audits/{id}/complete")
    public FiveSDto.AuditResponse complete(@PathVariable UUID id) { return service.completeAudit(id); }

    @PatchMapping("/audits/{id}/cancel")
    public FiveSDto.AuditResponse cancel(@PathVariable UUID id) { return service.cancelAudit(id); }

    @PutMapping("/audits/{id}/score")
    public FiveSDto.ItemResponse score(@PathVariable UUID id,
                                       @Valid @RequestBody FiveSDto.ScoreRequest request) {
        return service.scorePillar(id, request);
    }

    @DeleteMapping("/audits/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.deleteAudit(id); }

    /**
     * Analyse Vision CV (YOLOv8) d'une photo de zone pour un audit 5S donné
     * (CLAUDE.md §3.2 — détection auto d'encombrement / EPI / étiquetage manquant).
     *
     * <p>L'audit doit appartenir au tenant courant ({@code findById} → 404 sinon,
     * multi-tenant §18.2 #2). L'image est validée (taille / type / magic bytes) puis
     * relayée au service ai-vision-5s via {@link VisionGatewayClient} (tenant dérivé
     * du JWT côté passerelle). Renvoie le score 5S par pilier + les findings ; un
     * service indisponible remonte proprement (503) sans bloquer l'audit.
     */
    @PostMapping(value = "/audits/{id}/vision", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VisionDto.VisionAnalysis analyzeVision(
            @PathVariable UUID id,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        service.findById(id); // 404 si l'audit n'existe pas pour ce tenant
        VisionImageValidator.ValidatedImage img = VisionImageValidator.validate(image, file);
        return visionGateway.analyze(img.contentType(), img.filename(), img.bytes());
    }
}
