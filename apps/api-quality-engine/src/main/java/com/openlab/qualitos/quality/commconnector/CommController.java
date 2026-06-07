package com.openlab.qualitos.quality.commconnector;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * API de configuration des connexions de communication sortante (CLAUDE.md §13.3).
 *
 * <p>Role-gating : la configuration d'intégration est une action d'administration. Le
 * verrouillage URL ({@code /api/v1/comm/**} → ADMIN / ADMIN_TENANT / SUPER_ADMIN) est
 * posé dans {@code quality/config/SecurityConfig}, alignée sur les autres connecteurs
 * d'intégration (webhooks, api-keys).
 */
@RestController
@RequestMapping("/api/v1/comm")
public class CommController {

    private final CommConnectorService service;

    public CommController(CommConnectorService service) { this.service = service; }

    @GetMapping("/connections")
    public Page<CommDto.ConnectionResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.listConnections(pageable);
    }

    @PostMapping("/connections")
    @ResponseStatus(HttpStatus.CREATED)
    public CommDto.ConnectionResponse create(@Valid @RequestBody CommDto.CreateConnectionRequest req) {
        return service.createConnection(req);
    }

    @GetMapping("/connections/{id}")
    public CommDto.ConnectionResponse get(@PathVariable UUID id) {
        return service.getConnection(id);
    }

    @PatchMapping("/connections/{id}")
    public CommDto.ConnectionResponse update(@PathVariable UUID id,
                                             @Valid @RequestBody CommDto.UpdateConnectionRequest req) {
        return service.updateConnection(id, req);
    }

    @DeleteMapping("/connections/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.deleteConnection(id); }

    @PostMapping("/connections/{id}/test")
    public CommDto.TestResult test(@PathVariable UUID id) {
        return service.test(id);
    }
}
