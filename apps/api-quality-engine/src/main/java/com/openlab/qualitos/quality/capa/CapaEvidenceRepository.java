package com.openlab.qualitos.quality.capa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CapaEvidenceRepository extends JpaRepository<CapaEvidence, UUID> {

    List<CapaEvidence> findByTenantIdAndCapaIdOrderByCreatedAtAsc(UUID tenantId, UUID capaId);

    Optional<CapaEvidence> findByIdAndTenantIdAndCapaId(UUID id, UUID tenantId, UUID capaId);

    long countByTenantIdAndCapaId(UUID tenantId, UUID capaId);

    /**
     * Poids déjà versé au dossier. La somme est calculée en base plutôt que sur
     * une liste chargée : le plafond doit être vérifié même si le dossier porte
     * ses dix pièces, et charger les métadonnées pour additionner un entier
     * serait payer un aller-retour pour rien.
     */
    @Query("select coalesce(sum(e.sizeBytes), 0) from CapaEvidence e "
            + "where e.tenantId = :tenantId and e.capaId = :capaId")
    long sumSizeBytes(UUID tenantId, UUID capaId);
}
