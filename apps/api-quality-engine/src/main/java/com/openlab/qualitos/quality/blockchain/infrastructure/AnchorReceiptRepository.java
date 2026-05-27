package com.openlab.qualitos.quality.blockchain.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AnchorReceiptRepository extends JpaRepository<AnchorReceiptEntity, UUID> {

    /** Dernier reçu d'un tenant (pour chaîner prev_receipt_hash + seq_no). */
    Optional<AnchorReceiptEntity> findTopByTenantIdOrderBySeqNoDesc(UUID tenantId);

    /** Chargement isolé par tenant (OWASP A01) lors de la vérification. */
    Optional<AnchorReceiptEntity> findByTenantIdAndId(UUID tenantId, UUID id);
}
