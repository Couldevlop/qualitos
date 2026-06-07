package com.openlab.qualitos.quality.ehrconnector;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EhrImportedResourceRepository extends JpaRepository<EhrImportedResource, UUID> {

    /** Idempotence : la ressource FHIR a-t-elle déjà été importée pour cette connexion ? */
    boolean existsByConnectionIdAndFhirResourceId(UUID connectionId, String fhirResourceId);
}
