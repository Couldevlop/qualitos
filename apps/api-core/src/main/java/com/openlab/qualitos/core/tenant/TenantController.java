package com.openlab.qualitos.core.tenant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST controller pour la gestion des tenants.
 * Accessible uniquement aux SUPER_ADMIN (protégé via SecurityConfig + @PreAuthorize).
 * Le tenant_id n'est jamais lu depuis le body — géré au niveau sécurité.
 */
@RestController
@RequestMapping("/api/v1/tenants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Tenants", description = "Tenant management — Super Admin only")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    @Operation(summary = "List all tenants (paginated)")
    public ResponseEntity<Page<TenantDto.Response>> listTenants(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(tenantService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tenant by ID")
    public ResponseEntity<TenantDto.Response> getTenant(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantService.findById(id));
    }

    @GetMapping("/by-slug/{slug}")
    @Operation(summary = "Get tenant by slug")
    public ResponseEntity<TenantDto.Response> getTenantBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(tenantService.findBySlug(slug));
    }

    @PostMapping
    @Operation(summary = "Create a new tenant")
    public ResponseEntity<TenantDto.Response> createTenant(
            @Valid @RequestBody TenantDto.CreateRequest request) {
        TenantDto.Response created = tenantService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update tenant name, plan, or active status")
    public ResponseEntity<TenantDto.Response> updateTenant(
            @PathVariable UUID id,
            @Valid @RequestBody TenantDto.UpdateRequest request) {
        return ResponseEntity.ok(tenantService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a tenant (soft delete)")
    public ResponseEntity<Void> deactivateTenant(@PathVariable UUID id) {
        tenantService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
