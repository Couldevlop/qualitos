package com.openlab.qualitos.quality.training.competencymatrix.application;

import com.openlab.qualitos.quality.training.competencymatrix.domain.LevelCell;
import com.openlab.qualitos.quality.training.competencymatrix.domain.MatrixOperator;
import com.openlab.qualitos.quality.training.competencymatrix.domain.MatrixSkill;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Les ports de la matrice de compétences. Chaque méthode prend le client en
 * premier argument : c'est lui qui borne toute lecture et toute écriture.
 */
public final class CompetencyMatrixPorts {

    private CompetencyMatrixPorts() {}

    /** Les personnes inscrites dans la matrice. */
    public interface Operators {

        List<MatrixOperator> findAll(UUID tenantId);

        Optional<MatrixOperator> findById(UUID tenantId, UUID operatorId);

        Optional<MatrixOperator> findByUser(UUID tenantId, UUID userId);

        MatrixOperator add(UUID tenantId, UUID userId, String name, String workshop, UUID actor, Instant at);

        void remove(UUID tenantId, UUID operatorId);
    }

    /**
     * Les compétences suivies par la matrice, adossées au catalogue des
     * compétences du client.
     */
    public interface Skills {

        List<MatrixSkill> findAll(UUID tenantId);

        Optional<MatrixSkill> find(UUID tenantId, UUID skillId);

        /**
         * La compétence du catalogue qui porte cet intitulé (casse ignorée), si
         * elle existe : on la reprend plutôt que d'en créer un doublon.
         */
        Optional<UUID> findCatalogueSkillByName(UUID tenantId, String name);

        /** Crée la compétence au catalogue ; rend son identifiant. */
        UUID createCatalogueSkill(UUID tenantId, String name, String category);

        /** Donne une catégorie à une compétence du catalogue qui n'en a pas. */
        void categorizeIfMissing(UUID tenantId, UUID skillId, String category);

        MatrixSkill add(UUID tenantId, UUID skillId, UUID trainerUserId, String trainerName, UUID actor, Instant at);

        void remove(UUID tenantId, UUID skillId);
    }

    /**
     * Les niveaux. Ce sont les évaluations de compétence du client
     * ({@code training_user_skills}) : poser un niveau dans la matrice, c'est
     * évaluer la personne.
     */
    public interface Levels {

        List<LevelCell> findAll(UUID tenantId);

        /**
         * Pose le niveau ; rend le niveau précédent, vide si la personne n'avait
         * jamais été évaluée sur cette compétence.
         */
        OptionalInt set(UUID tenantId, UUID userId, String userName, UUID skillId, int level, UUID actor,
                        LocalDate on);
    }

    /** Les ateliers réglés par l'administrateur du client. */
    public interface WorkshopSettings {

        /** Vide quand le client n'a rien réglé. */
        Optional<List<String>> find(UUID tenantId);

        void replace(UUID tenantId, List<String> names, UUID actor);
    }

    /** Qui agit, et pour quel client : les deux viennent du jeton validé (§18.2). */
    public interface Context {

        UUID requireTenantId();

        UUID requireActorId();
    }

    /** Toute écriture de la matrice laisse une trace dans le journal d'audit chaîné (OWASP A09). */
    public interface Audit {

        void operatorAdded(UUID tenantId, MatrixOperator operator, UUID actor);

        void operatorRemoved(UUID tenantId, MatrixOperator operator, UUID actor);

        void skillAdded(UUID tenantId, MatrixSkill skill, UUID actor);

        void skillRemoved(UUID tenantId, MatrixSkill skill, UUID actor);

        void levelSet(UUID tenantId, LevelCell cell, OptionalInt previous, UUID actor);

        void workshopsChanged(UUID tenantId, List<String> names, UUID actor);
    }
}
