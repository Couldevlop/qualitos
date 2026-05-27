package com.openlab.qualitos.quality.blockchain.infrastructure;

import com.openlab.qualitos.quality.auditlog.AuditEvent;
import com.openlab.qualitos.quality.auditlog.AuditEventRepository;
import com.openlab.qualitos.quality.blockchain.domain.AnchorReadPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter JPA du port {@link AnchorReadPort} : mappe vers les repos audit + reçus.
 * Sépare proprement le domaine blockchain des entités JPA.
 */
@Component
public class JpaAnchorReadAdapter implements AnchorReadPort {

    private final AuditEventRepository audit;
    private final AnchorReceiptRepository receipts;

    public JpaAnchorReadAdapter(AuditEventRepository audit, AnchorReceiptRepository receipts) {
        this.audit = audit;
        this.receipts = receipts;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> txRefForEvent(UUID tenantId, String integrityHash) {
        return audit.findByTenantIdAndIntegrityHash(tenantId, integrityHash)
                .map(AuditEvent::getBlockchainTxRef)
                .filter(ref -> ref != null && !ref.isBlank());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> integrityHashesForTxRef(UUID tenantId, String txRef) {
        return audit.findByTenantIdAndBlockchainTxRefOrderBySequenceNoAsc(tenantId, txRef)
                .stream().map(AuditEvent::getIntegrityHash).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReceiptView> receipt(UUID tenantId, String txRef) {
        UUID id;
        try {
            id = UUID.fromString(txRef);
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // txRef non-UUID (ex: stub) → pas de reçu signé
        }
        return receipts.findByTenantIdAndId(tenantId, id)
                .map(r -> new ReceiptView(r.getMerkleRoot(), r.getSignature()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> tenantsWithUnanchoredEvents() {
        return audit.findDistinctTenantIdsWithUnanchoredEvents();
    }
}
