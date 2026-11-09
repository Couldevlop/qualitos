package com.openlab.qualitos.quality.settings.catalog;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Les listes déroulantes paramétrables par le client.
 *
 * <p><b>Les lire est ouvert à tout authentifié</b> : chaque formulaire de saisie
 * en a besoin, et un utilisateur qui ne verrait pas la liste ne pourrait pas
 * saisir. <b>Les changer est réservé à l'administrateur du client</b>
 * ({@code authz.manage}) : tout paramétrage hors gestion des utilisateurs lui
 * revient.
 *
 * <p>Le client courant vient du jeton, jamais du chemin ni du corps (§18.2 #2).
 */
@RestController
@RequestMapping("/api/v1/settings/catalogs")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Tenant catalogs", description = "Per-tenant dropdown lists")
public class TenantCatalogController {

    private final TenantCatalogService service;

    public TenantCatalogController(TenantCatalogService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Every catalogue open to tenant settings, with its state")
    public List<TenantCatalogService.Summary> list() {
        return service.summaries();
    }

    /**
     * Les listes réglées par ce client, et elles seules. Un seul appel au
     * démarrage de l'application, au lieu de douze.
     */
    @GetMapping("/custom")
    @Operation(summary = "Only the catalogues this tenant has set, in one call")
    public Map<String, List<CatalogEntry>> customised() {
        return service.customised();
    }

    @GetMapping("/{catalogue}")
    @Operation(summary = "A catalogue: the tenant's values, or the platform defaults")
    public CatalogView get(@PathVariable String catalogue) {
        return CatalogView.of(service.view(resolve(catalogue)));
    }

    @PutMapping("/{catalogue}")
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    @Operation(summary = "Replace a catalogue; the order received is the display order")
    public CatalogView replace(@PathVariable String catalogue,
                               @Valid @RequestBody CatalogForm form) {
        return CatalogView.of(service.replace(resolve(catalogue),
                form.entries().stream().map(EntryForm::toDomain).toList()));
    }

    @PostMapping("/{catalogue}/reset")
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    @Operation(summary = "Go back to the platform values")
    public CatalogView reset(@PathVariable String catalogue) {
        return CatalogView.of(service.reset(resolve(catalogue)));
    }

    private CatalogId resolve(String catalogue) {
        return CatalogId.bySlug(catalogue)
                .orElseThrow(() -> new CatalogNotFoundException(catalogue));
    }

    /** Une entrée telle qu'elle arrive de l'écran de paramétrage. */
    public record EntryForm(@NotBlank @Size(max = CatalogEntry.CODE_MAX) String code,
                            @NotBlank @Size(max = CatalogEntry.LABEL_MAX) String label,
                            Boolean active) {
        CatalogEntry toDomain() {
            return new CatalogEntry(code, label, active == null || active);
        }
    }

    public record CatalogForm(
            @NotNull @Size(min = 1, max = CatalogId.MAX_ENTRIES)
            List<@Valid @NotNull EntryForm> entries) {
    }

    /** Une liste rendue aux écrans : son nom, son mode, son origine, ses entrées. */
    public record CatalogView(String catalogue, CatalogMode mode, boolean defaults,
                              List<CatalogEntry> entries) {
        static CatalogView of(TenantCatalogService.View view) {
            return new CatalogView(view.id().slug(), view.id().mode(), view.defaults(), view.entries());
        }
    }
}
