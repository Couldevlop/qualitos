package com.openlab.qualitos.quality.authz.web;

import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.quality.authz.application.AuthzDto;
import com.openlab.qualitos.quality.authz.domain.Permission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * L'administration des droits d'un client : le catalogue des actions, les
 * rôles et ce qu'ils accordent, les rôles attribués aux membres.
 *
 * <p>Tout authentifié peut lire SES droits ({@code /me}) : l'écran s'en sert
 * pour n'afficher que ce qui servira. Le reste exige {@code authz.manage}.
 * Le client vient du jeton ; rien dans la requête ne le désigne (§18.2).
 */
@RestController
@Validated
@RequestMapping("/api/v1/authz")
@PreAuthorize("isAuthenticated()")
public class AuthzController {

    private final AuthorizationService service;

    public AuthzController(AuthorizationService service) {
        this.service = service;
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public AuthzDto.Me me() {
        return service.me();
    }

    @GetMapping("/catalog")
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    public List<AuthzDto.CatalogEntry> catalog() {
        return service.catalog();
    }

    @GetMapping("/roles")
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    @Transactional(readOnly = true)
    public List<AuthzDto.RoleView> roles() {
        return service.roles();
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    @Transactional
    public AuthzDto.RoleView createRole(@Valid @RequestBody RoleRequest r) {
        return service.createRole(r.toCommand());
    }

    @PutMapping("/roles/{code}")
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    @Transactional
    public AuthzDto.RoleView updateRole(@PathVariable @Pattern(regexp = "[A-Z][A-Z0-9_]{1,63}") String code,
                                        @Valid @RequestBody RoleRequest r) {
        return service.updateRole(code, r.toCommand());
    }

    /** Supprime un rôle sur mesure, ou rend à un rôle système ses droits livrés. */
    @DeleteMapping("/roles/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    @Transactional
    public void deleteRole(@PathVariable @Pattern(regexp = "[A-Z][A-Z0-9_]{1,63}") String code) {
        service.deleteRole(code);
    }

    @GetMapping("/members")
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    @Transactional(readOnly = true)
    public List<AuthzDto.MemberView> members() {
        return service.members();
    }

    @PutMapping("/members/{userId}/roles")
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    @Transactional
    public AuthzDto.MemberView replaceMemberRoles(@PathVariable UUID userId,
                                                  @Valid @RequestBody MemberRolesRequest r) {
        return service.replaceMemberRoles(userId, r.roles());
    }

    // ---------- corps de requête ----------

    public record RoleRequest(
            @Size(max = 64) String code,
            @Size(max = 120) String name,
            @Size(max = 500) String description,
            @NotNull @Size(max = 200) List<@NotNull @Size(max = 80) String> permissions) {

        AuthzDto.RoleCommand toCommand() {
            return new AuthzDto.RoleCommand(code, name, description, permissions);
        }
    }

    public record MemberRolesRequest(@NotNull @Size(max = 50) List<@NotNull @Size(max = 64) String> roles) {}
}
