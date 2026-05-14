package com.openlab.qualitos.core.user;

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
 * REST controller pour la gestion des utilisateurs applicatifs.
 * Accessible aux ADMIN et SUPER_ADMIN.
 *
 * <p>Le tenantId n'est JAMAIS accepté depuis le body : il est résolu dans UserService
 * depuis TenantContext (ThreadLocal alimenté par TenantJwtFilter depuis le JWT).
 */
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Users", description = "User management — Admin only")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List users in the current tenant (paginated)")
    public ResponseEntity<Page<UserDto.Response>> listUsers(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(userService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserDto.Response> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping("/by-keycloak/{keycloakId}")
    @Operation(summary = "Get user by Keycloak ID")
    public ResponseEntity<UserDto.Response> getUserByKeycloakId(@PathVariable String keycloakId) {
        return ResponseEntity.ok(userService.findByKeycloakId(keycloakId));
    }

    @PostMapping
    @Operation(summary = "Create user in the current tenant (tenantId from JWT, not body)")
    public ResponseEntity<UserDto.Response> createUser(
            @Valid @RequestBody UserDto.CreateRequest request) {
        UserDto.Response created = userService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user roles or active status")
    public ResponseEntity<UserDto.Response> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserDto.UpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a user (soft delete)")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID id) {
        userService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
