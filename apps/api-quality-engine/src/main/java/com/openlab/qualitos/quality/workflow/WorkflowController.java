package com.openlab.qualitos.quality.workflow;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * CRUD du Designer de workflow BPMN no-code (§5.4).
 *
 * <p>Sécurité (cf. {@code quality/config/SecurityConfig}) : la lecture (GET) est
 * réservée aux authentifiés ; les écritures (POST/PUT/DELETE sur
 * {@code /api/v1/workflow/**}) sont réservées aux rôles qualité
 * (ADMIN / ADMIN_TENANT / SUPER_ADMIN / QUALITY_MANAGER). Le tenant et l'acteur
 * sont dérivés du JWT par le service, jamais du body.</p>
 */
@RestController
@RequestMapping("/api/v1/workflow/definitions")
public class WorkflowController {

    private final WorkflowService service;

    public WorkflowController(WorkflowService service) {
        this.service = service;
    }

    @GetMapping
    public Page<WorkflowDto.Summary> list(
            @RequestParam(required = false) WorkflowStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.findAll(status, pageable);
    }

    @GetMapping("/{id}")
    public WorkflowDto.Response get(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowDto.Response create(@Valid @RequestBody WorkflowDto.CreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public WorkflowDto.Response update(@PathVariable UUID id,
                                       @Valid @RequestBody WorkflowDto.UpdateRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/publish")
    public WorkflowDto.Response publish(@PathVariable UUID id) {
        return service.publish(id);
    }

    @PostMapping("/{id}/archive")
    public WorkflowDto.Response archive(@PathVariable UUID id) {
        return service.archive(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
