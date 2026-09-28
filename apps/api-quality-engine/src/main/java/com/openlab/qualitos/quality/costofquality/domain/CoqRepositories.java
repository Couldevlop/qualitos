package com.openlab.qualitos.quality.costofquality.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Les ports de persistance du coût de la qualité.
 *
 * <p>Chaque lecture prend le tenant en argument : l'isolation n'est pas une
 * option qu'on pourrait oublier d'appeler, elle est dans la signature.
 */
public final class CoqRepositories {

    private CoqRepositories() {}

    public interface Entries {
        CoqEntry save(CoqEntry entry);

        Optional<CoqEntry> findByIdAndTenant(UUID id, UUID tenantId);

        /** Les lignes dont la date d'imputation tombe dans [from, to], bornes comprises. */
        List<CoqEntry> findByTenantBetween(UUID tenantId, LocalDate from, LocalDate to);

        void delete(CoqEntry entry);
    }

    public interface Labels {
        /** Le catalogue livré, plus les libellés propres à ce tenant. */
        List<CoqLabel> findVisible(UUID tenantId);

        Optional<CoqLabel> findVisibleById(UUID id, UUID tenantId);

        CoqLabel save(CoqLabel label);
    }

    public interface Settings {
        Optional<String> currency(UUID tenantId);

        void saveCurrency(UUID tenantId, String currency);
    }
}
