package com.openlab.qualitos.quality.nonconformity.subcategory;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import com.openlab.qualitos.quality.nonconformity.NcCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Le catalogue de sous-catégories de non-conformité.
 *
 * <p><b>Le lire est ouvert à tout authentifié</b> : chaque formulaire de saisie
 * d'écart en a besoin. <b>Le changer est réservé à l'administrateur du client</b>
 * ({@code authz.manage}), comme tout paramétrage.
 */
@RestController
@RequestMapping("/api/v1/nc/subcategories")
@Tag(name = "NC subcategories", description = "Tenant catalogue of non-conformity natures")
public class NcSubcategoryController {

    private final NcSubcategoryCatalog catalog;

    public NcSubcategoryController(NcSubcategoryCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    @Operation(summary = "The tenant catalogue, or the platform defaults when none is set")
    public CatalogueView get() {
        return CatalogueView.of(catalog.view());
    }

    @PutMapping
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    @Operation(summary = "Replace the catalogue; codes already carried by past NCs are kept as-is")
    public CatalogueView replace(@Valid @RequestBody CatalogueRequest request) {
        return CatalogueView.of(catalog.replace(
                request.entries().stream().map(EntryRequest::toDomain).toList()));
    }

    @PostMapping("/reset")
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    @Operation(summary = "Go back to the platform catalogue")
    public CatalogueView reset() {
        return CatalogueView.of(catalog.reset());
    }

    /** Une entrée telle qu'elle arrive de l'écran de paramétrage. */
    public record EntryRequest(@NotBlank @Size(max = NcSubcategory.CODE_MAX) String code,
                               @NotNull NcCategory category,
                               @NotBlank @Size(max = NcSubcategory.LABEL_MAX) String label) {
        NcSubcategory toDomain() {
            return new NcSubcategory(code, category, label);
        }
    }

    public record CatalogueRequest(
            @NotNull @Size(min = 1, max = NcSubcategoryCatalog.MAX_ENTRIES)
            List<@Valid @NotNull EntryRequest> entries) {
    }

    /** Le catalogue rendu aux écrans, avec son origine. */
    public record CatalogueView(List<NcSubcategory> entries, boolean defaults) {
        static CatalogueView of(NcSubcategoryCatalog.View view) {
            return new CatalogueView(view.entries(), view.defaults());
        }
    }
}
