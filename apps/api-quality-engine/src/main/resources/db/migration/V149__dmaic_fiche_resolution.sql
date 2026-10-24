-- Fiche de résolution DMAIC (ADR 0101), reprise du classeur
-- « DMAIC Résolution de Problème » : en-tête de réclamation, questionnaire par
-- phase, confinement, causes racines, 5 pourquoi, plan d'actions, photos et
-- rapport d'action corrective client.
--
-- Les trois tables portent une colonne tenant_id : la politique d'isolation par
-- client (ADR 0086) leur est posée automatiquement après la migration.

-- ---------------------------------------------------------------------------
-- 1. Le paramétrage du client
-- ---------------------------------------------------------------------------
--
-- Sans ligne, la plateforme propose les questions, les types de défaillance
-- (V01 à V122) et les trois sévérités du classeur. Les listes se remplacent
-- d'un bloc, d'où le JSON : elles se lisent et s'écrivent toujours entières.
CREATE TABLE dmaic_resolution_settings (
    id             UUID          NOT NULL DEFAULT gen_random_uuid(),
    tenant_id      UUID          NOT NULL,
    questions      TEXT          NOT NULL,
    failure_types  TEXT          NOT NULL,
    severities     TEXT          NOT NULL,
    updated_by     UUID,
    updated_at     TIMESTAMPTZ   NOT NULL,

    CONSTRAINT pk_dmaic_resolution_settings PRIMARY KEY (id),
    CONSTRAINT uk_dmaic_resolution_settings_tenant UNIQUE (tenant_id)
);

-- ---------------------------------------------------------------------------
-- 2. La fiche d'un projet
-- ---------------------------------------------------------------------------
--
-- Une par projet. Le contenu est un document : il se lit et s'écrit d'un bloc,
-- et sa forme suit le paramétrage du client (questions ajoutées ou retirées)
-- sans migration. Supprimer le projet supprime sa fiche.
CREATE TABLE dmaic_resolution_sheets (
    id          UUID          NOT NULL DEFAULT gen_random_uuid(),
    tenant_id   UUID          NOT NULL,
    project_id  UUID          NOT NULL,
    content     TEXT          NOT NULL,
    created_by  UUID,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,

    CONSTRAINT pk_dmaic_resolution_sheets PRIMARY KEY (id),
    CONSTRAINT uk_dmaic_resolution_sheets_project UNIQUE (tenant_id, project_id),
    CONSTRAINT fk_dmaic_resolution_sheets_project FOREIGN KEY (project_id)
        REFERENCES dmaic_projects (id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------------
-- 3. Les photos de la fiche
-- ---------------------------------------------------------------------------
--
-- Six emplacements, comme le classeur. Le binaire vit dans le stockage objet ;
-- la ligne n'en garde que la clé. Une clé orpheline (projet supprimé) est
-- reprise par le balayeur d'orphelins.
CREATE TABLE dmaic_resolution_photos (
    id            UUID          NOT NULL DEFAULT gen_random_uuid(),
    tenant_id     UUID          NOT NULL,
    project_id    UUID          NOT NULL,
    slot          INTEGER       NOT NULL,
    object_key    VARCHAR(400)  NOT NULL,
    content_type  VARCHAR(50)   NOT NULL,
    size_bytes    BIGINT        NOT NULL,
    remark        VARCHAR(1000),
    created_by    UUID,
    created_at    TIMESTAMPTZ   NOT NULL,

    CONSTRAINT pk_dmaic_resolution_photos PRIMARY KEY (id),
    CONSTRAINT uk_dmaic_resolution_photos_slot UNIQUE (tenant_id, project_id, slot),
    CONSTRAINT uk_dmaic_resolution_photos_key UNIQUE (object_key),
    CONSTRAINT chk_dmaic_resolution_photos_slot CHECK (slot BETWEEN 1 AND 6),
    CONSTRAINT fk_dmaic_resolution_photos_project FOREIGN KEY (project_id)
        REFERENCES dmaic_projects (id) ON DELETE CASCADE
);
