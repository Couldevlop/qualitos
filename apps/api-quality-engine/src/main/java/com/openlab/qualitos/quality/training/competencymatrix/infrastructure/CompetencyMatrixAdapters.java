package com.openlab.qualitos.quality.training.competencymatrix.infrastructure;

import com.openlab.qualitos.quality.training.CompetencySource;
import com.openlab.qualitos.quality.training.Skill;
import com.openlab.qualitos.quality.training.SkillRepository;
import com.openlab.qualitos.quality.training.UserSkillAssignment;
import com.openlab.qualitos.quality.training.UserSkillAssignmentRepository;
import com.openlab.qualitos.quality.training.competencymatrix.application.CompetencyMatrixPorts;
import com.openlab.qualitos.quality.training.competencymatrix.domain.LevelCell;
import com.openlab.qualitos.quality.training.competencymatrix.domain.MatrixOperator;
import com.openlab.qualitos.quality.training.competencymatrix.domain.MatrixSkill;
import com.openlab.qualitos.quality.training.competencymatrix.domain.SkillCode;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Les adaptateurs JPA des ports de la matrice.
 *
 * <p>Chaque requête porte le client : une inscription, une compétence ou un
 * niveau d'un autre client n'est jamais lu ni écrit, même avec son identifiant
 * exact. Classes simples, déclarées comme beans par
 * {@link CompetencyMatrixBeanConfiguration}.
 */
public final class CompetencyMatrixAdapters {

    /** Au-delà, la recherche d'un code libre s'arrête : un catalogue n'a pas mille homonymes. */
    static final int MAX_CODE_ATTEMPTS = 1000;

    private CompetencyMatrixAdapters() {}

    // ---------------------------------------------------------------- opérateurs

    public static final class Operators implements CompetencyMatrixPorts.Operators {

        private final MatrixOperatorJpaRepository jpa;

        public Operators(MatrixOperatorJpaRepository jpa) {
            this.jpa = jpa;
        }

        @Override
        public List<MatrixOperator> findAll(UUID tenantId) {
            return jpa.findByTenantId(tenantId).stream().map(Operators::toDomain).toList();
        }

        @Override
        public Optional<MatrixOperator> findById(UUID tenantId, UUID operatorId) {
            return jpa.findByIdAndTenantId(operatorId, tenantId).map(Operators::toDomain);
        }

        @Override
        public Optional<MatrixOperator> findByUser(UUID tenantId, UUID userId) {
            return jpa.findByTenantIdAndUserId(tenantId, userId).map(Operators::toDomain);
        }

        @Override
        public MatrixOperator add(UUID tenantId, UUID userId, String name, String workshop, UUID actor, Instant at) {
            MatrixOperatorJpaEntity e = new MatrixOperatorJpaEntity();
            e.setTenantId(tenantId);
            e.setUserId(userId);
            e.setUserName(name);
            e.setWorkshop(workshop);
            e.setCreatedBy(actor);
            e.setCreatedAt(at);
            return toDomain(jpa.save(e));
        }

        @Override
        public void remove(UUID tenantId, UUID operatorId) {
            jpa.findByIdAndTenantId(operatorId, tenantId).ifPresent(jpa::delete);
        }

        private static MatrixOperator toDomain(MatrixOperatorJpaEntity e) {
            return new MatrixOperator(e.getId(), e.getUserId(), e.getUserName(), e.getWorkshop(), e.getCreatedAt());
        }
    }

    // ---------------------------------------------------------------- compétences

    public static final class Skills implements CompetencyMatrixPorts.Skills {

        private final MatrixSkillJpaRepository jpa;
        private final SkillRepository catalogue;

        public Skills(MatrixSkillJpaRepository jpa, SkillRepository catalogue) {
            this.jpa = jpa;
            this.catalogue = catalogue;
        }

        @Override
        public List<MatrixSkill> findAll(UUID tenantId) {
            List<MatrixSkillJpaEntity> lignes = jpa.findByTenantId(tenantId);
            Map<UUID, Skill> parId = catalogue.findAllById(lignes.stream().map(MatrixSkillJpaEntity::getSkillId).toList())
                    .stream()
                    .filter(s -> tenantId.equals(s.getTenantId()))
                    .collect(Collectors.toMap(Skill::getId, Function.identity()));
            List<MatrixSkill> out = new ArrayList<>(lignes.size());
            for (MatrixSkillJpaEntity l : lignes) {
                Skill s = parId.get(l.getSkillId());
                if (s != null) {
                    out.add(toDomain(l, s));
                }
            }
            return out;
        }

        @Override
        public Optional<MatrixSkill> find(UUID tenantId, UUID skillId) {
            return jpa.findByTenantIdAndSkillId(tenantId, skillId)
                    .flatMap(l -> catalogue.findById(l.getSkillId())
                            .filter(s -> tenantId.equals(s.getTenantId()))
                            .map(s -> toDomain(l, s)));
        }

        @Override
        public Optional<UUID> findCatalogueSkillByName(UUID tenantId, String name) {
            return catalogue.findFirstByTenantIdAndNameIgnoreCaseOrderByCreatedAtAsc(tenantId, name).map(Skill::getId);
        }

        @Override
        public UUID createCatalogueSkill(UUID tenantId, String name, String category) {
            Skill s = new Skill();
            s.setTenantId(tenantId);
            s.setCode(freeCode(tenantId, SkillCode.from(name)));
            s.setName(name);
            s.setCategory(category);
            return catalogue.save(s).getId();
        }

        @Override
        public void categorizeIfMissing(UUID tenantId, UUID skillId, String category) {
            catalogue.findById(skillId)
                    .filter(s -> tenantId.equals(s.getTenantId()))
                    .filter(s -> s.getCategory() == null || s.getCategory().isBlank())
                    .ifPresent(s -> {
                        s.setCategory(category);
                        catalogue.save(s);
                    });
        }

        @Override
        public MatrixSkill add(UUID tenantId, UUID skillId, UUID trainerUserId, String trainerName, UUID actor,
                               Instant at) {
            Skill s = catalogue.findById(skillId)
                    .filter(c -> tenantId.equals(c.getTenantId()))
                    .orElseThrow(() -> new IllegalStateException("Catalogue skill vanished: " + skillId));
            MatrixSkillJpaEntity e = new MatrixSkillJpaEntity();
            e.setTenantId(tenantId);
            e.setSkillId(skillId);
            e.setTrainerUserId(trainerUserId);
            e.setTrainerName(trainerName);
            e.setCreatedBy(actor);
            e.setCreatedAt(at);
            return toDomain(jpa.save(e), s);
        }

        @Override
        public void remove(UUID tenantId, UUID skillId) {
            jpa.findByTenantIdAndSkillId(tenantId, skillId).ifPresent(jpa::delete);
        }

        /** Le code tiré de l'intitulé, suffixé {@code -2}, {@code -3}… s'il est déjà pris dans ce client. */
        private String freeCode(UUID tenantId, String base) {
            for (int i = 1; i <= MAX_CODE_ATTEMPTS; i++) {
                String code = SkillCode.candidate(base, i);
                if (!catalogue.existsByTenantIdAndCode(tenantId, code)) {
                    return code;
                }
            }
            throw new IllegalStateException("No free catalogue code for " + base);
        }

        private static MatrixSkill toDomain(MatrixSkillJpaEntity l, Skill s) {
            return new MatrixSkill(s.getId(), s.getName(), s.getCategory(), l.getTrainerUserId(), l.getTrainerName(),
                    l.getCreatedAt());
        }
    }

    // ---------------------------------------------------------------- niveaux

    /**
     * Les niveaux sont les évaluations de compétence du client : la même ligne
     * que celle qu'écrit {@code POST /training/competencies/assess}, avec la
     * source « manager » (une évaluation posée par l'encadrement).
     */
    public static final class Levels implements CompetencyMatrixPorts.Levels {

        private final UserSkillAssignmentRepository jpa;

        public Levels(UserSkillAssignmentRepository jpa) {
            this.jpa = jpa;
        }

        @Override
        public List<LevelCell> findAll(UUID tenantId) {
            return jpa.findByTenantId(tenantId).stream()
                    .map(a -> new LevelCell(a.getUserId(), a.getSkillId(), a.getLevel()))
                    .toList();
        }

        /**
         * L'échéance éventuelle de l'évaluation est conservée : la matrice dit un
         * niveau, pas une durée de validité, et n'a pas à effacer ce qu'un
         * parcours de formation a fixé.
         */
        @Override
        public OptionalInt set(UUID tenantId, UUID userId, String userName, UUID skillId, int level, UUID actor,
                               LocalDate on) {
            Optional<UserSkillAssignment> existante = jpa.findByTenantIdAndUserIdAndSkillId(tenantId, userId, skillId);
            OptionalInt avant = existante.map(a -> OptionalInt.of(a.getLevel())).orElse(OptionalInt.empty());
            UserSkillAssignment a = existante.orElseGet(() -> {
                UserSkillAssignment fresh = new UserSkillAssignment();
                fresh.setTenantId(tenantId);
                fresh.setUserId(userId);
                fresh.setSkillId(skillId);
                return fresh;
            });
            a.setLevel(level);
            a.setUserName(userName);
            a.setSource(CompetencySource.MANAGER);
            a.setAssessedBy(actor);
            a.setAssessedAt(on);
            jpa.save(a);
            return avant;
        }
    }

    // ---------------------------------------------------------------- ateliers

    public static final class WorkshopSettings implements CompetencyMatrixPorts.WorkshopSettings {

        private final CompetencyWorkshopJpaRepository jpa;
        private final Clock clock;

        public WorkshopSettings(CompetencyWorkshopJpaRepository jpa, Clock clock) {
            this.jpa = jpa;
            this.clock = clock;
        }

        @Override
        public Optional<List<String>> find(UUID tenantId) {
            List<String> noms = jpa.findByTenantIdOrderByPositionAsc(tenantId).stream()
                    .map(CompetencyWorkshopJpaEntity::getName)
                    .toList();
            return noms.isEmpty() ? Optional.empty() : Optional.of(noms);
        }

        @Override
        public void replace(UUID tenantId, List<String> names, UUID actor) {
            jpa.deleteByTenant(tenantId);
            Instant maintenant = clock.instant();
            List<CompetencyWorkshopJpaEntity> lignes = new ArrayList<>(names.size());
            for (int i = 0; i < names.size(); i++) {
                CompetencyWorkshopJpaEntity e = new CompetencyWorkshopJpaEntity();
                e.setTenantId(tenantId);
                e.setName(names.get(i));
                e.setPosition(i);
                e.setUpdatedBy(actor);
                e.setUpdatedAt(maintenant);
                lignes.add(e);
            }
            jpa.saveAll(lignes);
        }
    }
}
