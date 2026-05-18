package com.openlab.qualitos.quality.dashboards.web;

import com.openlab.qualitos.quality.dashboards.application.DashboardLayoutDto;
import com.openlab.qualitos.quality.dashboards.application.DashboardLayoutService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Drag &amp; drop custom dashboards endpoint (CLAUDE.md §7.3).
 */
@RestController
@RequestMapping("/api/v1/dashboards/custom")
@Validated
public class DashboardLayoutController {

    private final DashboardLayoutService service;

    public DashboardLayoutController(DashboardLayoutService service) {
        this.service = service;
    }

    @GetMapping
    public List<DashboardLayoutDto.View> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public DashboardLayoutDto.View get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DashboardLayoutDto.View create(@Valid @RequestBody DashboardLayoutWebDto.SaveRequest req) {
        return service.create(new DashboardLayoutDto.SaveRequest(
                req.name(), req.description(), req.layoutJson(), req.shared()));
    }

    @PutMapping("/{id}")
    public DashboardLayoutDto.View update(@PathVariable UUID id,
                                          @Valid @RequestBody DashboardLayoutWebDto.SaveRequest req) {
        return service.update(id, new DashboardLayoutDto.SaveRequest(
                req.name(), req.description(), req.layoutJson(), req.shared()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
