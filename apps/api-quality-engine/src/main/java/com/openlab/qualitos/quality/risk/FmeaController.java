package com.openlab.qualitos.quality.risk;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fmea")
public class FmeaController {

    private final FmeaService service;

    public FmeaController(FmeaService service) { this.service = service; }

    // ---- Projects ----

    @GetMapping("/projects")
    public Page<FmeaDto.ProjectResponse> list(
            @RequestParam(required = false) FmeaStatus status,
            @RequestParam(required = false) FmeaType type,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.listProjects(status, type, pageable);
    }

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public FmeaDto.ProjectResponse create(@Valid @RequestBody FmeaDto.CreateProjectRequest req) {
        return service.createProject(req);
    }

    @GetMapping("/projects/{id}")
    public FmeaDto.ProjectResponse get(@PathVariable UUID id) { return service.getProject(id); }

    @PatchMapping("/projects/{id}")
    public FmeaDto.ProjectResponse update(@PathVariable UUID id,
                                          @Valid @RequestBody FmeaDto.UpdateProjectRequest req) {
        return service.updateProject(id, req);
    }

    @DeleteMapping("/projects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.deleteProject(id); }

    @PostMapping("/projects/{id}/activate")
    public FmeaDto.ProjectResponse activate(@PathVariable UUID id) { return service.activate(id); }

    @PostMapping("/projects/{id}/reopen")
    public FmeaDto.ProjectResponse reopen(@PathVariable UUID id) { return service.reopen(id); }

    @PostMapping("/projects/{id}/archive")
    public FmeaDto.ProjectResponse archive(@PathVariable UUID id) { return service.archive(id); }

    @GetMapping("/projects/{id}/statistics")
    public FmeaDto.ProjectStatistics statistics(@PathVariable UUID id) { return service.statistics(id); }

    // ---- Items ----

    @GetMapping("/projects/{projectId}/items")
    public Page<FmeaDto.ItemResponse> listItems(@PathVariable UUID projectId,
                                                @PageableDefault(size = 100) Pageable pageable) {
        return service.listItems(projectId, pageable);
    }

    @PostMapping("/projects/{projectId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public FmeaDto.ItemResponse addItem(@PathVariable UUID projectId,
                                        @Valid @RequestBody FmeaDto.CreateItemRequest req) {
        return service.addItem(projectId, req);
    }

    @PatchMapping("/projects/{projectId}/items/{itemId}")
    public FmeaDto.ItemResponse updateItem(@PathVariable UUID projectId,
                                           @PathVariable UUID itemId,
                                           @Valid @RequestBody FmeaDto.UpdateItemRequest req) {
        return service.updateItem(projectId, itemId, req);
    }

    @DeleteMapping("/projects/{projectId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable UUID projectId, @PathVariable UUID itemId) {
        service.deleteItem(projectId, itemId);
    }
}
