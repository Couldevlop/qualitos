package com.openlab.qualitos.quality.spc.capability.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Les ports de persistance des études SPC.
 *
 * <p>Chaque lecture prend le tenant en argument : l'isolation n'est pas une
 * option qu'on pourrait oublier d'appeler, elle est dans la signature.
 */
public final class SpcRepositories {

    private SpcRepositories() {}

    public interface Studies {
        CapabilityStudy save(CapabilityStudy study);

        Optional<CapabilityStudy> findByIdAndTenant(UUID id, UUID tenantId);

        List<CapabilityStudy> findByTenant(UUID tenantId);

        void delete(CapabilityStudy study);
    }

    public interface Settings {
        Optional<CapabilityThresholds> thresholds(UUID tenantId);

        void saveThresholds(UUID tenantId, CapabilityThresholds thresholds, UUID actor);
    }
}
