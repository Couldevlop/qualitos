package com.openlab.qualitos.quality.dmaic.resolution.web;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import com.openlab.qualitos.quality.config.RequiresModule;
import com.openlab.qualitos.quality.dmaic.resolution.application.ResolutionDto;
import com.openlab.qualitos.quality.dmaic.resolution.application.ResolutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * La fiche de résolution d'un projet DMAIC (ADR 0101).
 *
 * <p><b>La lire est ouvert à tout authentifié, l'écrire demande l'action
 * {@code dmaic.manage}</b>, que le client règle dans sa matrice des droits.
 * Module « dmaic ». Les écritures sont transactionnelles ici : la fiche et sa
 * trace d'audit ensemble, ou rien.
 */
@RestController
@RequestMapping("/api/v1/dmaic/projects/{projectId}/resolution")
@PreAuthorize("isAuthenticated()")
@RequiresModule("dmaic")
@Tag(name = "DMAIC resolution sheet", description = "Problem-solving sheet of a DMAIC project and its customer report")
public class ResolutionController {

    private final ResolutionService service;

    public ResolutionController(ResolutionService service) {
        this.service = service;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "The sheet of a project, blank until someone saves it, with the tenant's questionnaire")
    public ResolutionDto.SheetView get(@PathVariable UUID projectId) {
        return service.sheet(projectId);
    }

    @PutMapping
    @RequiresPermission(Permission.DMAIC_MANAGE)
    @Transactional
    @Operation(summary = "Save the whole sheet; answers are checked against the tenant's questionnaire")
    public ResolutionDto.SheetView save(@PathVariable UUID projectId,
                                        @Valid @RequestBody ResolutionWebDto.SheetRequest request) {
        return service.save(projectId, request.toDomain());
    }

    @PostMapping(value = "/photos/{slot}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(Permission.DMAIC_MANAGE)
    @Transactional
    @Operation(summary = "Put a photo (slots 1-6) or a phase attachment (slots 11-55) in a slot, replacing the one there")
    public ResolutionDto.PhotoView uploadPhoto(@PathVariable UUID projectId, @PathVariable int slot,
                                               @RequestParam("file") MultipartFile file,
                                               @RequestParam(value = "remark", required = false) String remark)
            throws IOException {
        return service.uploadPhoto(projectId, slot, file.getContentType(), file.getBytes(), remark,
                file.getOriginalFilename());
    }

    @PatchMapping("/photos/{slot}")
    @RequiresPermission(Permission.DMAIC_MANAGE)
    @Transactional
    @Operation(summary = "Change the remark of a photo")
    public ResolutionDto.PhotoView updateRemark(@PathVariable UUID projectId, @PathVariable int slot,
                                                @Valid @RequestBody ResolutionWebDto.RemarkRequest request) {
        return service.updateRemark(projectId, slot, request.remark());
    }

    @DeleteMapping("/photos/{slot}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission(Permission.DMAIC_MANAGE)
    @Transactional
    @Operation(summary = "Remove the photo of a slot")
    public void deletePhoto(@PathVariable UUID projectId, @PathVariable int slot) {
        service.deletePhoto(projectId, slot);
    }

    @GetMapping(value = "/claim-report", produces = MediaType.APPLICATION_PDF_VALUE)
    @Transactional(readOnly = true)
    @Operation(summary = "The customer corrective action report (steps D, M, A, I, C) as PDF")
    public ResponseEntity<byte[]> claimReport(@PathVariable UUID projectId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("rapport-client-dmaic.pdf").build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(service.claimReport(projectId));
    }
}
