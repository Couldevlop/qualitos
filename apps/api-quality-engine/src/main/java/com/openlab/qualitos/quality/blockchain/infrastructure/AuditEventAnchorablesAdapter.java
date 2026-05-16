package com.openlab.qualitos.quality.blockchain.infrastructure;

import com.openlab.qualitos.quality.auditlog.AuditEvent;
import com.openlab.qualitos.quality.auditlog.AuditEventRepository;
import com.openlab.qualitos.quality.blockchain.domain.Anchorable;
import com.openlab.qualitos.quality.blockchain.domain.AnchorablesPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Adapter qui mappe le port {@link AnchorablesPort} vers le repo du module audit.
 * Sépare proprement les deux modules : blockchain ne touche pas l'entité JPA
 * AuditEvent en dehors de ce fichier.
 */
@Component
public class AuditEventAnchorablesAdapter implements AnchorablesPort {

    private final AuditEventRepository repo;

    public AuditEventAnchorablesAdapter(AuditEventRepository repo) { this.repo = repo; }

    @Override
    @Transactional(readOnly = true)
    public List<Anchorable> loadUnanchored(UUID tenantId, int limit) {
        return repo.findByTenantIdAndBlockchainTxRefIsNullOrderBySequenceNoAsc(
                        tenantId, PageRequest.of(0, limit))
                .stream()
                .map(e -> new Anchorable(e.getId(), e.getSequenceNo(), e.getIntegrityHash()))
                .toList();
    }

    @Override
    @Transactional
    public int markAnchored(UUID tenantId, List<UUID> eventIds, String txRef) {
        int updated = 0;
        for (UUID id : eventIds) {
            AuditEvent e = repo.findById(id).orElse(null);
            if (e == null || !e.getTenantId().equals(tenantId)) continue;
            if (e.getBlockchainTxRef() != null) continue; // idempotent
            e.setBlockchainTxRef(txRef);
            repo.save(e);
            updated++;
        }
        return updated;
    }
}
