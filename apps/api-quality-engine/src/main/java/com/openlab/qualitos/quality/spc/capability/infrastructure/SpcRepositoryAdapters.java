package com.openlab.qualitos.quality.spc.capability.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.spc.capability.domain.CapabilityStudy;
import com.openlab.qualitos.quality.spc.capability.domain.CapabilityThresholds;
import com.openlab.qualitos.quality.spc.capability.domain.SpcNotFoundException;
import com.openlab.qualitos.quality.spc.capability.domain.SpcRepositories;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Les adaptateurs JPA des deux ports.
 *
 * <p>Déclarés comme beans par {@link SpcBeanConfiguration} : ce sont des
 * classes simples, testables sans contexte Spring.
 */
public final class SpcRepositoryAdapters {

    private SpcRepositoryAdapters() {}

    public static final class Studies implements SpcRepositories.Studies {

        private static final TypeReference<List<List<Double>>> GRILLE = new TypeReference<>() {};

        private final SpcStudyJpaRepository jpa;
        private final ObjectMapper json;

        public Studies(SpcStudyJpaRepository jpa, ObjectMapper json) {
            this.jpa = jpa;
            this.json = json;
        }

        /**
         * Une étude déjà identifiée est relue dans SON tenant avant d'être
         * écrite : si elle a disparu entre la lecture et l'écriture, on rend
         * 404 plutôt que de la ressusciter sous un identifiant neuf.
         */
        @Override
        public CapabilityStudy save(CapabilityStudy study) {
            SpcStudyJpaEntity cible;
            if (study.getId() == null) {
                cible = new SpcStudyJpaEntity();
                cible.setTenantId(study.getTenantId());
                cible.setCapableThreshold(study.getThresholds().capable());
                cible.setWatchThreshold(study.getThresholds().watch());
                cible.setCreatedBy(study.getCreatedBy());
                cible.setCreatedAt(study.getCreatedAt());
            } else {
                cible = jpa.findByIdAndTenantId(study.getId(), study.getTenantId())
                        .orElseThrow(() -> new SpcNotFoundException(study.getId()));
            }
            cible.setPartReference(study.getPartReference());
            cible.setCharacteristic(study.getCharacteristic());
            cible.setSpecification(study.getSpecification());
            cible.setMachine(study.getMachine());
            cible.setLeaderUserId(study.getLeaderUserId());
            cible.setLeaderName(study.getLeaderName());
            cible.setStudyDate(study.getStudyDate());
            cible.setActionPlan(study.isActionPlan());
            cible.setLsl(study.getLsl());
            cible.setUsl(study.getUsl());
            cible.setSubgroupSize(study.getSubgroupSize());
            cible.setMeasurements(ecrire(study.getMeasurements()));
            cible.setCp(study.getCp());
            cible.setCpk(study.getCpk());
            cible.setUpdatedAt(study.getUpdatedAt());
            return toDomain(jpa.save(cible));
        }

        @Override
        public Optional<CapabilityStudy> findByIdAndTenant(UUID id, UUID tenantId) {
            return jpa.findByIdAndTenantId(id, tenantId).map(this::toDomain);
        }

        @Override
        public List<CapabilityStudy> findByTenant(UUID tenantId) {
            return jpa.findByTenantId(tenantId).stream().map(this::toDomain).toList();
        }

        @Override
        public void delete(CapabilityStudy study) {
            jpa.findByIdAndTenantId(study.getId(), study.getTenantId()).ifPresent(jpa::delete);
        }

        CapabilityStudy toDomain(SpcStudyJpaEntity e) {
            return new CapabilityStudy(e.getId(), e.getTenantId(), e.getPartReference(), e.getCharacteristic(),
                    e.getSpecification(), e.getMachine(), e.getLeaderUserId(), e.getLeaderName(),
                    e.getStudyDate(), e.isActionPlan(), e.getLsl(), e.getUsl(), e.getSubgroupSize(),
                    lire(e.getMeasurements()), e.getCp(), e.getCpk(),
                    new CapabilityThresholds(e.getCapableThreshold(), e.getWatchThreshold()),
                    e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
        }

        private String ecrire(List<List<Double>> grille) {
            try {
                return json.writeValueAsString(grille);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("SPC grid could not be serialized", e);
            }
        }

        private List<List<Double>> lire(String grille) {
            try {
                return json.readValue(grille, GRILLE);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("SPC grid could not be read", e);
            }
        }
    }

    public static final class Settings implements SpcRepositories.Settings {

        private final SpcSettingsJpaRepository jpa;
        private final Clock clock;

        public Settings(SpcSettingsJpaRepository jpa, Clock clock) {
            this.jpa = jpa;
            this.clock = clock;
        }

        @Override
        public Optional<CapabilityThresholds> thresholds(UUID tenantId) {
            return jpa.findById(tenantId)
                    .map(e -> new CapabilityThresholds(e.getCapableThreshold(), e.getWatchThreshold()));
        }

        @Override
        public void saveThresholds(UUID tenantId, CapabilityThresholds thresholds, UUID actor) {
            SpcSettingsJpaEntity e = jpa.findById(tenantId).orElseGet(() -> {
                SpcSettingsJpaEntity neuf = new SpcSettingsJpaEntity();
                neuf.setTenantId(tenantId);
                return neuf;
            });
            e.setCapableThreshold(thresholds.capable());
            e.setWatchThreshold(thresholds.watch());
            e.setUpdatedBy(actor);
            e.setUpdatedAt(clock.instant());
            jpa.save(e);
        }
    }
}
