package com.openlab.qualitos.quality.pdca;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PdcaStepEvidenceRepository extends JpaRepository<PdcaStepEvidence, UUID> {

    /**
     * Toutes les pièces d'un cycle, en une passe : le tableau des étapes les
     * range ensuite par étape. Une requête par ligne ferait autant d'allers et
     * retours que d'étapes pour remplir une seule colonne.
     */
    @Query("select e from PdcaStepEvidence e where e.tenantId = :tenantId and e.cycleId = :cycleId "
            + "order by e.createdAt asc")
    List<PdcaStepEvidence> findForCycle(UUID tenantId, UUID cycleId);

    Optional<PdcaStepEvidence> findByIdAndTenantIdAndStepId(UUID id, UUID tenantId, UUID stepId);

    long countByTenantIdAndStepId(UUID tenantId, UUID stepId);

    /**
     * Poids déjà versé au cycle, calculé en base : le plafond doit être vérifié
     * même quand le cycle porte beaucoup d'étapes, et charger les métadonnées
     * pour additionner un entier serait payer un aller-retour pour rien.
     */
    @Query("select coalesce(sum(e.sizeBytes), 0) from PdcaStepEvidence e "
            + "where e.tenantId = :tenantId and e.cycleId = :cycleId")
    long sumSizeBytes(UUID tenantId, UUID cycleId);

    /**
     * Existence par clé d'objet, sans filtre tenant — pour le balayage des
     * orphelins, qui s'exécute hors requête et donc sans contexte tenant. La clé
     * porte déjà le tenant dans son chemin, et la question posée ici est
     * « quelqu'un revendique-t-il cet objet ? », qui ne dépend d'aucun tenant.
     */
    boolean existsByObjectKey(String objectKey);
}
