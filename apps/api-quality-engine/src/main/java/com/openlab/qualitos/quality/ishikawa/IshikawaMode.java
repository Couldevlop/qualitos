package com.openlab.qualitos.quality.ishikawa;

import java.util.EnumSet;
import java.util.Set;

/**
 * Mode du diagramme d'Ishikawa: nombre de branches autorisées.
 *
 * - SIX_M : METHODS, MANPOWER, MACHINES, MATERIALS, MEASUREMENTS, ENVIRONMENT
 * - SEVEN_M : 6M + MANAGEMENT
 * - EIGHT_M : 7M + MONEY
 */
public enum IshikawaMode {
    SIX_M(EnumSet.of(
            CauseCategory.METHODS,
            CauseCategory.MANPOWER,
            CauseCategory.MACHINES,
            CauseCategory.MATERIALS,
            CauseCategory.MEASUREMENTS,
            CauseCategory.ENVIRONMENT)),
    SEVEN_M(EnumSet.of(
            CauseCategory.METHODS,
            CauseCategory.MANPOWER,
            CauseCategory.MACHINES,
            CauseCategory.MATERIALS,
            CauseCategory.MEASUREMENTS,
            CauseCategory.ENVIRONMENT,
            CauseCategory.MANAGEMENT)),
    EIGHT_M(EnumSet.allOf(CauseCategory.class));

    private final Set<CauseCategory> allowedCategories;

    IshikawaMode(Set<CauseCategory> allowedCategories) {
        this.allowedCategories = allowedCategories;
    }

    public Set<CauseCategory> allowedCategories() {
        return allowedCategories;
    }

    public boolean allows(CauseCategory category) {
        return allowedCategories.contains(category);
    }
}
