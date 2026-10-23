package com.openlab.qualitos.quality.spc.gagerr.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.spc.capability.domain.SpcNotFoundException;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrDecision;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrOperator;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrRepository;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrStudy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * L'adaptateur JPA du port {@link GageRrRepository}. Déclaré comme bean par
 * {@link GageRrBeanConfiguration} : une classe simple, testable sans Spring.
 */
public final class GageRrRepositoryAdapter implements GageRrRepository {

    private static final TypeReference<List<List<List<Double>>>> GRILLE = new TypeReference<>() {};
    private static final TypeReference<List<GageRrOperator>> OPERATEURS = new TypeReference<>() {};

    private final GageRrStudyJpaRepository jpa;
    private final ObjectMapper json;

    public GageRrRepositoryAdapter(GageRrStudyJpaRepository jpa, ObjectMapper json) {
        this.jpa = jpa;
        this.json = json;
    }

    /**
     * Une étude déjà identifiée est relue dans SON tenant avant d'être écrite :
     * disparue entre-temps, elle rend 404 plutôt que de renaître sous un
     * identifiant neuf.
     */
    @Override
    public GageRrStudy save(GageRrStudy study) {
        GageRrStudyJpaEntity cible;
        if (study.getId() == null) {
            cible = new GageRrStudyJpaEntity();
            cible.setTenantId(study.getTenantId());
            cible.setCreatedBy(study.getCreatedBy());
            cible.setCreatedAt(study.getCreatedAt());
        } else {
            cible = jpa.findByIdAndTenantId(study.getId(), study.getTenantId())
                    .orElseThrow(() -> new SpcNotFoundException(study.getId()));
        }
        cible.setPartReference(study.getPartReference());
        cible.setCharacteristic(study.getCharacteristic());
        cible.setInstrument(study.getInstrument());
        cible.setLsl(study.getLsl());
        cible.setUsl(study.getUsl());
        cible.setOperatorCount(study.getOperatorCount());
        cible.setPartCount(study.getPartCount());
        cible.setTrialCount(study.getTrialCount());
        cible.setSigmaMultiplier(study.getSigmaMultiplier());
        cible.setStudyDate(study.getStudyDate());
        cible.setResponsibleUserId(study.getResponsibleUserId());
        cible.setResponsibleName(study.getResponsibleName());
        cible.setOperators(ecrire(study.getOperators()));
        cible.setMeasurements(ecrire(study.getMeasurements()));
        cible.setGrrPercent(study.getGrrPercent());
        cible.setGrrTolerancePercent(study.getGrrTolerancePercent());
        cible.setNdc(study.getNdc());
        cible.setDecision(study.getDecision().name());
        cible.setAnovaGrrPercent(study.getAnovaGrrPercent());
        cible.setAnovaNdc(study.getAnovaNdc());
        cible.setUpdatedAt(study.getUpdatedAt());
        return toDomain(jpa.save(cible));
    }

    @Override
    public Optional<GageRrStudy> findByIdAndTenant(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId).map(this::toDomain);
    }

    @Override
    public List<GageRrStudy> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(GageRrStudy study) {
        jpa.findByIdAndTenantId(study.getId(), study.getTenantId()).ifPresent(jpa::delete);
    }

    GageRrStudy toDomain(GageRrStudyJpaEntity e) {
        return new GageRrStudy(e.getId(), e.getTenantId(), e.getPartReference(), e.getCharacteristic(),
                e.getInstrument(), e.getLsl(), e.getUsl(), e.getOperatorCount(), e.getPartCount(),
                e.getTrialCount(), e.getSigmaMultiplier(), e.getStudyDate(), e.getResponsibleUserId(),
                e.getResponsibleName(), lire(e.getOperators(), OPERATEURS), lire(e.getMeasurements(), GRILLE),
                e.getGrrPercent(), e.getGrrTolerancePercent(), e.getNdc(), GageRrDecision.valueOf(e.getDecision()),
                e.getAnovaGrrPercent(), e.getAnovaNdc(), e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private String ecrire(Object valeur) {
        try {
            return json.writeValueAsString(valeur);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Gage R&R study could not be serialized", e);
        }
    }

    private <T> T lire(String texte, TypeReference<T> type) {
        try {
            return json.readValue(texte, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Gage R&R study could not be read", e);
        }
    }
}
