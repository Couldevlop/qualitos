package com.openlab.qualitos.quality.fives;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FiveSItemPhotoRepository extends JpaRepository<FiveSItemPhoto, UUID> {

    List<FiveSItemPhoto> findByTenantIdAndAuditIdOrderByCreatedAtAsc(UUID tenantId, UUID auditId);

    List<FiveSItemPhoto> findByTenantIdAndAuditIdAndPillarOrderByCreatedAtAsc(
            UUID tenantId, UUID auditId, FiveSPillar pillar);

    Optional<FiveSItemPhoto> findByIdAndTenantIdAndAuditId(UUID id, UUID tenantId, UUID auditId);

    long countByTenantIdAndAuditIdAndPillar(UUID tenantId, UUID auditId, FiveSPillar pillar);

    /**
     * Poids déjà porté par cet audit, additionné en base.
     *
     * <p>Le plafond se compte par audit : c'est l'unité qu'un auditeur ouvre et
     * multiplie. Charger les métadonnées pour additionner un entier serait payer
     * un aller-retour pour rien.
     */
    @Query("select coalesce(sum(p.sizeBytes), 0) from FiveSItemPhoto p"
            + " where p.tenantId = :tenantId and p.auditId = :auditId")
    long sumSizeBytes(UUID tenantId, UUID auditId);

    /**
     * Existence par clé d'objet, sans filtre de client — pour le balayage des
     * orphelins, qui s'exécute hors requête et donc sans contexte. La clé porte
     * déjà le client dans son chemin, et la question posée ici est « quelqu'un
     * revendique-t-il cet objet ? », qui ne dépend d'aucun client.
     */
    boolean existsByObjectKey(String objectKey);
}
