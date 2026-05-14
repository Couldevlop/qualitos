package com.openlab.qualitos.quality.ishikawa;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ishikawa")
public class IshikawaController {

    private final IshikawaService ishikawaService;

    public IshikawaController(IshikawaService ishikawaService) {
        this.ishikawaService = ishikawaService;
    }

    @GetMapping("/diagrams")
    public Page<IshikawaDto.DiagramResponse> listDiagrams(
            @RequestParam(required = false) IshikawaStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ishikawaService.findAll(status, pageable);
    }

    @PostMapping("/diagrams")
    @ResponseStatus(HttpStatus.CREATED)
    public IshikawaDto.DiagramResponse createDiagram(
            @Valid @RequestBody IshikawaDto.CreateDiagramRequest request) {
        return ishikawaService.createDiagram(request);
    }

    @GetMapping("/diagrams/{id}")
    public IshikawaDto.DiagramResponse getDiagram(@PathVariable UUID id) {
        return ishikawaService.findById(id);
    }

    @PatchMapping("/diagrams/{id}")
    public IshikawaDto.DiagramResponse updateDiagram(
            @PathVariable UUID id,
            @Valid @RequestBody IshikawaDto.UpdateDiagramRequest request) {
        return ishikawaService.updateDiagram(id, request);
    }

    @DeleteMapping("/diagrams/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDiagram(@PathVariable UUID id) {
        ishikawaService.deleteDiagram(id);
    }

    @PostMapping("/diagrams/{id}/causes")
    @ResponseStatus(HttpStatus.CREATED)
    public IshikawaDto.CauseResponse addCause(
            @PathVariable UUID id,
            @Valid @RequestBody IshikawaDto.CauseRequest request) {
        return ishikawaService.addCause(id, request);
    }

    @PatchMapping("/diagrams/{id}/causes/{causeId}")
    public IshikawaDto.CauseResponse updateCause(
            @PathVariable UUID id,
            @PathVariable UUID causeId,
            @Valid @RequestBody IshikawaDto.UpdateCauseRequest request) {
        return ishikawaService.updateCause(id, causeId, request);
    }

    @DeleteMapping("/diagrams/{id}/causes/{causeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCause(@PathVariable UUID id, @PathVariable UUID causeId) {
        ishikawaService.deleteCause(id, causeId);
    }
}
