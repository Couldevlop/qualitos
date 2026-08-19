package com.openlab.qualitos.quality.revisionrequests.infrastructure;

import com.openlab.qualitos.quality.nonconformity.NonConformity;
import com.openlab.qualitos.quality.nonconformity.NonConformityRepository;
import com.openlab.qualitos.quality.revisionrequests.application.NcLookupPort;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Retrouve la non-conformité d'origine d'un dossier CAPA à partir de sa référence.
 *
 * <p>Le tenant fait partie de la recherche, pas d'un contrôle après coup : deux
 * tenants peuvent parfaitement porter la même référence « NC-2026-0001 ».
 */
@Component
public class NcLookupAdapter implements NcLookupPort {

    private final NonConformityRepository repository;

    public NcLookupAdapter(NonConformityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NcRef> findByReference(UUID tenantId, String reference) {
        Specification<NonConformity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            predicates.add(cb.equal(root.get("reference"), reference));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findOne(spec)
                .map(nc -> new NcRef(nc.getId(), nc.getProductId(), nc.getFmeaItemId()));
    }
}
