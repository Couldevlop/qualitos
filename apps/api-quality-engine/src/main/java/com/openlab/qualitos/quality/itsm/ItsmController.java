package com.openlab.qualitos.quality.itsm;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/itsm")
public class ItsmController {

    private final ItsmConnectorService service;

    public ItsmController(ItsmConnectorService service) { this.service = service; }

    @GetMapping("/connections")
    public Page<ItsmDto.ConnectionResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.listConnections(pageable);
    }

    @PostMapping("/connections")
    @ResponseStatus(HttpStatus.CREATED)
    public ItsmDto.ConnectionResponse create(@Valid @RequestBody ItsmDto.CreateConnectionRequest req) {
        return service.createConnection(req);
    }

    @GetMapping("/connections/{id}")
    public ItsmDto.ConnectionResponse get(@PathVariable UUID id) {
        return service.getConnection(id);
    }

    @PatchMapping("/connections/{id}")
    public ItsmDto.ConnectionResponse update(@PathVariable UUID id,
                                             @Valid @RequestBody ItsmDto.UpdateConnectionRequest req) {
        return service.updateConnection(id, req);
    }

    @DeleteMapping("/connections/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.deleteConnection(id); }

    @PostMapping("/connections/{id}/sync")
    public ItsmDto.SyncReport sync(@PathVariable UUID id) {
        return service.syncConnection(id);
    }

    @GetMapping("/mappings")
    public Page<ItsmDto.MappingResponse> mappings(
            @RequestParam(required = false) UUID connectionId,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.listMappings(connectionId, pageable);
    }
}
