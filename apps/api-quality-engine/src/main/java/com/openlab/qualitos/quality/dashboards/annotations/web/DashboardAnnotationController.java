package com.openlab.qualitos.quality.dashboards.annotations.web;

import com.openlab.qualitos.quality.dashboards.annotations.application.DashboardAnnotationDto;
import com.openlab.qualitos.quality.dashboards.annotations.application.DashboardAnnotationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Collaborative dashboard annotations (CLAUDE.md §7.3).
 *
 * <p>tenantId and authorId come from the JWT (never the body). Any authenticated
 * tenant member can post and read; deletion is enforced at the use-case layer
 * (author or tenant admin only).</p>
 */
@RestController
@RequestMapping("/api/v1/dashboards/annotations")
@Validated
@PreAuthorize("isAuthenticated()")
public class DashboardAnnotationController {

    private final DashboardAnnotationService service;

    public DashboardAnnotationController(DashboardAnnotationService service) {
        this.service = service;
    }

    @GetMapping
    public List<DashboardAnnotationDto.View> list(
            @RequestParam("chartKey")
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[a-z0-9]+(?:[._-][a-z0-9]+){0,5}$") String chartKey) {
        return service.listByChart(chartKey);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DashboardAnnotationDto.View create(
            @Valid @RequestBody DashboardAnnotationWebDto.CreateRequest req) {
        return service.create(new DashboardAnnotationDto.CreateRequest(
                req.chartKey(), req.anchorLabel(), req.body()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
