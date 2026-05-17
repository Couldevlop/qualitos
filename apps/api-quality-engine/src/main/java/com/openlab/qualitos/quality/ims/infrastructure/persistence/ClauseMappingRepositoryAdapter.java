package com.openlab.qualitos.quality.ims.infrastructure.persistence;

import com.openlab.qualitos.quality.ims.domain.model.ClauseMapping;
import com.openlab.qualitos.quality.ims.domain.model.ClauseRef;
import com.openlab.qualitos.quality.ims.domain.model.RelationType;
import com.openlab.qualitos.quality.ims.domain.port.ClauseMappingRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Adapter JPA → port domain {@link ClauseMappingRepository}.
 */
@Component
public class ClauseMappingRepositoryAdapter implements ClauseMappingRepository {

    private final NormClauseMappingJpaRepository jpaRepo;

    public ClauseMappingRepositoryAdapter(NormClauseMappingJpaRepository jpaRepo) {
        this.jpaRepo = Objects.requireNonNull(jpaRepo);
    }

    @Override
    public List<ClauseMapping> findMappingsBetween(List<String> standardCodes) {
        if (standardCodes == null || standardCodes.isEmpty()) {
            return List.of();
        }
        return jpaRepo.findBetween(standardCodes).stream()
                .map(this::toDomain)
                .toList();
    }

    private ClauseMapping toDomain(NormClauseMappingEntity e) {
        return new ClauseMapping(
                new ClauseRef(e.getSourceStandardCode(), e.getSourceClauseCode()),
                new ClauseRef(e.getTargetStandardCode(), e.getTargetClauseCode()),
                RelationType.valueOf(e.getRelationType().name()),
                e.getConfidence(),
                e.getNotes()
        );
    }
}
