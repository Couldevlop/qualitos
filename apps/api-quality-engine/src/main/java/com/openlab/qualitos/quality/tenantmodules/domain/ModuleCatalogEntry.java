package com.openlab.qualitos.quality.tenantmodules.domain;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Définition statique d'un module — code, libellé, tier minimum requis,
 * dépendances. Le catalogue est figé dans {@link ModuleCatalog}.
 */
public record ModuleCatalogEntry(
        String code,
        String name,
        String category,
        BillingTier minimumTier,
        Set<String> dependencies,
        boolean coreModule
) {
    private static final Pattern CODE = Pattern.compile("^[a-z][a-z0-9-]{1,49}$");

    public ModuleCatalogEntry {
        if (code == null || !CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("Invalid module code: " + code);
        }
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        if (minimumTier == null) throw new IllegalArgumentException("minimumTier required");
        dependencies = dependencies == null ? Set.of() : Set.copyOf(dependencies);
    }

    public static ModuleCatalogEntry of(String code, String name, String category,
                                        BillingTier tier, List<String> deps, boolean core) {
        return new ModuleCatalogEntry(code, name, category, tier,
                deps == null ? Set.of() : Set.copyOf(deps), core);
    }
}
