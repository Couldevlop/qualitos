-- Matrice de compétences éditable (ADR 0098).
--
-- Les niveaux ne sont PAS stockés ici : ce sont les évaluations de compétence
-- existantes (training_user_skills, 0 à 4). Poser un niveau dans la matrice,
-- c'est évaluer la personne ; il n'existe pas deux vérités sur ce qu'elle sait.
-- Les compétences restent celles du catalogue (training_skills, avec leur
-- catégorie). Ce qui manquait, et que cette migration ajoute :
--
--   1. les ateliers du client, tenus par son administrateur ;
--   2. qui la matrice suit (opérateur + atelier) ;
--   3. quelles compétences elle suit, et qui les forme.
--
-- Les trois tables portent une colonne tenant_id : la politique d'isolation par
-- client (ADR 0086) leur est posée automatiquement après la migration.

-- ---------------------------------------------------------------------------
-- 1. Les ateliers du client
-- ---------------------------------------------------------------------------
--
-- Sans ligne, la plateforme propose Usinage, Assemblage, Qualité, Maintenance.
-- La liste se remplace d'un bloc ; position fixe l'ordre d'affichage.
CREATE TABLE competency_workshops (
    id          UUID          NOT NULL DEFAULT gen_random_uuid(),
    tenant_id   UUID          NOT NULL,
    name        VARCHAR(60)   NOT NULL,
    position    INTEGER       NOT NULL,
    updated_by  UUID,
    updated_at  TIMESTAMPTZ   NOT NULL,

    CONSTRAINT pk_competency_workshops PRIMARY KEY (id),
    CONSTRAINT uk_competency_workshops_name UNIQUE (tenant_id, name),
    CONSTRAINT chk_competency_workshops_name CHECK (length(btrim(name)) > 0),
    CONSTRAINT chk_competency_workshops_position CHECK (position >= 0)
);

-- ---------------------------------------------------------------------------
-- 2. Les opérateurs de la matrice
-- ---------------------------------------------------------------------------
--
-- Une personne de l'annuaire : son compte de connexion (sub du jeton) et son
-- nom recopié au moment du choix, pour que la ligne reste lisible si elle
-- quitte l'annuaire. L'atelier est recopié en clair : retirer un atelier de la
-- liste ne réaffecte personne en son absence.
CREATE TABLE competency_matrix_operators (
    id          UUID          NOT NULL DEFAULT gen_random_uuid(),
    tenant_id   UUID          NOT NULL,
    user_id     UUID          NOT NULL,
    user_name   VARCHAR(250)  NOT NULL,
    workshop    VARCHAR(60)   NOT NULL,
    created_by  UUID,
    created_at  TIMESTAMPTZ   NOT NULL,

    CONSTRAINT pk_competency_matrix_operators PRIMARY KEY (id),
    CONSTRAINT uk_competency_matrix_operators_user UNIQUE (tenant_id, user_id),
    CONSTRAINT chk_competency_matrix_operators_name CHECK (length(btrim(user_name)) > 0)
);

-- ---------------------------------------------------------------------------
-- 3. Les compétences de la matrice et leur formateur
-- ---------------------------------------------------------------------------
--
-- Une compétence du catalogue, et la personne désignée pour la transmettre.
-- Supprimer la compétence du catalogue la retire de la matrice.
CREATE TABLE competency_matrix_skills (
    id               UUID          NOT NULL DEFAULT gen_random_uuid(),
    tenant_id        UUID          NOT NULL,
    skill_id         UUID          NOT NULL,
    trainer_user_id  UUID          NOT NULL,
    trainer_name     VARCHAR(250)  NOT NULL,
    created_by       UUID,
    created_at       TIMESTAMPTZ   NOT NULL,

    CONSTRAINT pk_competency_matrix_skills PRIMARY KEY (id),
    CONSTRAINT uk_competency_matrix_skills_skill UNIQUE (tenant_id, skill_id),
    CONSTRAINT fk_competency_matrix_skills_skill FOREIGN KEY (skill_id)
        REFERENCES training_skills (id) ON DELETE CASCADE,
    CONSTRAINT chk_competency_matrix_skills_trainer CHECK (length(btrim(trainer_name)) > 0)
);

CREATE INDEX ix_competency_matrix_skills_skill ON competency_matrix_skills (skill_id);
