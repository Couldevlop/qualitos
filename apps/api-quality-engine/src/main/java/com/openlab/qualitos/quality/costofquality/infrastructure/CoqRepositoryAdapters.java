package com.openlab.qualitos.quality.costofquality.infrastructure;

import com.openlab.qualitos.quality.costofquality.domain.CoqEntry;
import com.openlab.qualitos.quality.costofquality.domain.CoqLabel;
import com.openlab.qualitos.quality.costofquality.domain.CoqNotFoundException;
import com.openlab.qualitos.quality.costofquality.domain.CoqRepositories;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Les adaptateurs JPA des trois ports.
 *
 * <p>Déclarés comme beans par {@link CostOfQualityBeanConfiguration} : ce sont
 * des classes simples, testables sans contexte Spring.
 */
public final class CoqRepositoryAdapters {

    private CoqRepositoryAdapters() {}

    public static final class Entries implements CoqRepositories.Entries {

        private final CoqEntryJpaRepository jpa;

        public Entries(CoqEntryJpaRepository jpa) {
            this.jpa = jpa;
        }

        /**
         * Une ligne déjà identifiée est relue dans SON tenant avant d'être
         * écrite : si elle a disparu entre la lecture et l'écriture, on rend
         * 404 plutôt que de la ressusciter sous un identifiant neuf.
         */
        @Override
        public CoqEntry save(CoqEntry entry) {
            CoqEntryJpaEntity cible;
            if (entry.getId() == null) {
                cible = new CoqEntryJpaEntity();
                cible.setTenantId(entry.getTenantId());
                cible.setCreatedBy(entry.getCreatedBy());
                cible.setCreatedAt(entry.getCreatedAt());
            } else {
                cible = jpa.findByIdAndTenantId(entry.getId(), entry.getTenantId())
                        .orElseThrow(() -> new CoqNotFoundException("Entry", entry.getId()));
            }
            cible.setLabelId(entry.getLabelId());
            cible.setCategory(entry.getCategory());
            cible.setAmount(entry.getAmount());
            cible.setResponsible(entry.getResponsible());
            cible.setImputationDate(entry.getImputationDate());
            cible.setComment(entry.getComment());
            cible.setPartControl(entry.isPartControl());
            cible.setPartReference(entry.getPartReference());
            cible.setPartQuantity(entry.getPartQuantity());
            cible.setLot(entry.getLot());
            cible.setReceivedOrMadeOn(entry.getReceivedOrMadeOn());
            cible.setUpdatedAt(entry.getUpdatedAt());
            return toDomain(jpa.save(cible));
        }

        @Override
        public Optional<CoqEntry> findByIdAndTenant(UUID id, UUID tenantId) {
            return jpa.findByIdAndTenantId(id, tenantId).map(Entries::toDomain);
        }

        @Override
        public List<CoqEntry> findByTenantBetween(UUID tenantId, LocalDate from, LocalDate to) {
            return jpa.findByTenantIdAndImputationDateBetween(tenantId, from, to).stream()
                    .map(Entries::toDomain).toList();
        }

        @Override
        public void delete(CoqEntry entry) {
            jpa.findByIdAndTenantId(entry.getId(), entry.getTenantId()).ifPresent(jpa::delete);
        }

        static CoqEntry toDomain(CoqEntryJpaEntity e) {
            return new CoqEntry(e.getId(), e.getTenantId(), e.getLabelId(), e.getCategory(),
                    e.getAmount(), e.getResponsible(), e.getImputationDate(), e.getComment(),
                    e.isPartControl(), e.getPartReference(), e.getPartQuantity(), e.getLot(),
                    e.getReceivedOrMadeOn(), e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
        }
    }

    public static final class Labels implements CoqRepositories.Labels {

        private final CoqLabelJpaRepository jpa;
        private final Clock clock;

        public Labels(CoqLabelJpaRepository jpa, Clock clock) {
            this.jpa = jpa;
            this.clock = clock;
        }

        @Override
        public List<CoqLabel> findVisible(UUID tenantId) {
            return jpa.findVisible(tenantId).stream().map(Labels::toDomain).toList();
        }

        @Override
        public Optional<CoqLabel> findVisibleById(UUID id, UUID tenantId) {
            return jpa.findVisibleById(id, tenantId).map(Labels::toDomain);
        }

        /** N'écrit que des libellés NEUFS : un libellé ne se renomme ni ne change de famille. */
        @Override
        public CoqLabel save(CoqLabel label) {
            CoqLabelJpaEntity e = new CoqLabelJpaEntity();
            e.setTenantId(label.getTenantId());
            e.setCategory(label.getCategory());
            e.setCode(label.getCode());
            e.setName(label.getName());
            e.setPartControl(label.isPartControl());
            e.setPosition(label.getPosition());
            e.setCreatedAt(clock.instant());
            return toDomain(jpa.save(e));
        }

        static CoqLabel toDomain(CoqLabelJpaEntity e) {
            return new CoqLabel(e.getId(), e.getTenantId(), e.getCategory(), e.getCode(),
                    e.getName(), e.isPartControl(), e.getPosition());
        }
    }

    public static final class Settings implements CoqRepositories.Settings {

        private final CoqSettingsJpaRepository jpa;
        private final Clock clock;

        public Settings(CoqSettingsJpaRepository jpa, Clock clock) {
            this.jpa = jpa;
            this.clock = clock;
        }

        @Override
        public Optional<String> currency(UUID tenantId) {
            return jpa.findById(tenantId).map(CoqSettingsJpaEntity::getCurrency);
        }

        @Override
        public void saveCurrency(UUID tenantId, String currency) {
            CoqSettingsJpaEntity e = jpa.findById(tenantId).orElseGet(() -> {
                CoqSettingsJpaEntity neuf = new CoqSettingsJpaEntity();
                neuf.setTenantId(tenantId);
                return neuf;
            });
            e.setCurrency(currency);
            e.setUpdatedAt(clock.instant());
            jpa.save(e);
        }
    }
}
