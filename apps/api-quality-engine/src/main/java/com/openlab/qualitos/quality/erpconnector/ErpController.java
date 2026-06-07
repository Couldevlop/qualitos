package com.openlab.qualitos.quality.erpconnector;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * API connecteur ERP (CLAUDE.md §13.3). Le role-gating (ADMIN / ADMIN_TENANT /
 * SUPER_ADMIN) est appliqué au niveau URL dans {@code config.SecurityConfig}
 * (matcher {@code /api/v1/erp/**}) — administration d'intégration tenant.
 */
@RestController
@RequestMapping("/api/v1/erp")
public class ErpController {

    private final ErpConnectorService service;

    public ErpController(ErpConnectorService service) { this.service = service; }

    @GetMapping("/connections")
    public Page<ErpDto.ConnectionResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.listConnections(pageable);
    }

    @PostMapping("/connections")
    @ResponseStatus(HttpStatus.CREATED)
    public ErpDto.ConnectionResponse create(@Valid @RequestBody ErpDto.CreateConnectionRequest req) {
        return service.createConnection(req);
    }

    @GetMapping("/connections/{id}")
    public ErpDto.ConnectionResponse get(@PathVariable UUID id) {
        return service.getConnection(id);
    }

    @PatchMapping("/connections/{id}")
    public ErpDto.ConnectionResponse update(@PathVariable UUID id,
                                            @Valid @RequestBody ErpDto.UpdateConnectionRequest req) {
        return service.updateConnection(id, req);
    }

    @DeleteMapping("/connections/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.deleteConnection(id); }

    @PostMapping("/connections/{id}/sync")
    public ErpDto.SyncReport sync(@PathVariable UUID id) {
        return service.sync(id);
    }
}
