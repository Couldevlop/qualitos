package com.openlab.qualitos.quality.ehrconnector;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * API du connecteur EHR / HL7 FHIR (§13.3).
 *
 * <p>Role-gating : la gestion des connexions et le déclenchement de sync sont des
 * actions d'intégration sensibles, réservées ADMIN / ADMIN_TENANT / SUPER_ADMIN
 * via la couche URL ({@code SecurityConfig} : règle {@code /api/v1/ehr/**}).</p>
 */
@RestController
@RequestMapping("/api/v1/ehr")
public class EhrController {

    private final EhrConnectorService service;

    public EhrController(EhrConnectorService service) { this.service = service; }

    @GetMapping("/connections")
    public Page<EhrDto.ConnectionResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.listConnections(pageable);
    }

    @PostMapping("/connections")
    @ResponseStatus(HttpStatus.CREATED)
    public EhrDto.ConnectionResponse create(@Valid @RequestBody EhrDto.CreateConnectionRequest req) {
        return service.createConnection(req);
    }

    @GetMapping("/connections/{id}")
    public EhrDto.ConnectionResponse get(@PathVariable UUID id) {
        return service.getConnection(id);
    }

    @PatchMapping("/connections/{id}")
    public EhrDto.ConnectionResponse update(@PathVariable UUID id,
                                            @Valid @RequestBody EhrDto.UpdateConnectionRequest req) {
        return service.updateConnection(id, req);
    }

    @DeleteMapping("/connections/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.deleteConnection(id); }

    @PostMapping("/connections/{id}/sync")
    public EhrDto.SyncReport sync(@PathVariable UUID id) {
        return service.sync(id);
    }
}
