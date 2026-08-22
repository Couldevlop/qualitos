package com.openlab.qualitos.quality.revisionrequests.infrastructure;

import com.openlab.qualitos.quality.nonconformity.NonConformity;
import com.openlab.qualitos.quality.nonconformity.NonConformityRepository;
import com.openlab.qualitos.quality.revisionrequests.application.NcHistoryPort;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Compte les non-conformités d'un produit rattachées à un mode de défaillance sur
 * une fenêtre glissante. Le comptage se fait en base : ramener les lignes pour les
 * compter en Java ferait grossir la requête avec l'ancienneté du tenant.
 */
@Component
public class NcHistoryAdapter implements NcHistoryPort {

    private final NonConformityRepository repository;

    public NcHistoryAdapter(NonConformityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public int countForProductAndFailureMode(UUID tenantId, UUID productId, UUID fmeaItemId,
                                             Instant since) {
        Specification<NonConformity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            predicates.add(cb.equal(root.get("productId"), productId));
            predicates.add(cb.equal(root.get("fmeaItemId"), fmeaItemId));
            predicates.add(cb.greaterThanOrEqualTo(root.get("detectedAt"), since));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return (int) repository.count(spec);
    }
}
