package com.openlab.qualitos.quality.nonconformity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NcPhotoRepository extends JpaRepository<NcPhoto, UUID> {

    List<NcPhoto> findByTenantIdAndNcIdOrderByCreatedAtAsc(UUID tenantId, UUID ncId);

    Optional<NcPhoto> findByIdAndTenantIdAndNcId(UUID id, UUID tenantId, UUID ncId);

    /**
     * Existence par clé d'objet, sans filtre tenant — pour le balayage des
     * orphelins, qui s'exécute hors requête et donc sans contexte tenant. La
     * clé porte déjà le tenant dans son chemin, et la question posée ici est
     * « quelqu'un revendique-t-il cet objet ? », qui ne dépend d'aucun tenant.
     */
    boolean existsByObjectKey(String objectKey);
}
