-- SPC : registre des études de capabilité procédé, Cp / Cpk sur cartes X̄ – R (ADR 0096).
--
-- Les deux tables portent une colonne tenant_id : la politique d'isolation par
-- client (ADR 0086) leur est posée automatiquement après la migration.

-- ---------------------------------------------------------------------------
-- 1. Les seuils de Cpk d'un client
-- ---------------------------------------------------------------------------
--
-- Une ligne par client, écrite par l'administrateur du client. Sans ligne, la
-- plateforme applique 1,67 (maîtrisé) et 1,33 (à surveiller).
CREATE TABLE spc_settings (
    tenant_id          UUID           NOT NULL,
    capable_threshold  NUMERIC(4, 2)  NOT NULL,
    watch_threshold    NUMERIC(4, 2)  NOT NULL,
    updated_by         UUID,
    updated_at         TIMESTAMPTZ    NOT NULL,

    CONSTRAINT pk_spc_settings PRIMARY KEY (tenant_id),
    CONSTRAINT chk_spc_settings_watch CHECK (watch_threshold > 0),
    CONSTRAINT chk_spc_settings_order CHECK (watch_threshold < capable_threshold)
);

-- ---------------------------------------------------------------------------
-- 2. Les études
-- ---------------------------------------------------------------------------
--
-- La grille de mesures est rangée en JSON dans une colonne TEXT : elle se lit
-- et s'écrit d'un bloc, le SGBD ne l'interroge jamais. Cp et Cpk sont calculés
-- par le serveur à chaque écriture ; les seuils sont ceux du client au jour de
-- la création, et ne bougent plus ensuite.
CREATE TABLE spc_capability_studies (
    id                 UUID              NOT NULL DEFAULT gen_random_uuid(),
    tenant_id          UUID              NOT NULL,
    part_reference     VARCHAR(120)      NOT NULL,
    characteristic     VARCHAR(150),
    specification      VARCHAR(150),
    machine            VARCHAR(80),
    leader_user_id     UUID,
    leader_name        VARCHAR(150),
    study_date         DATE              NOT NULL,
    action_plan        BOOLEAN           NOT NULL DEFAULT FALSE,
    lsl                DOUBLE PRECISION  NOT NULL,
    usl                DOUBLE PRECISION  NOT NULL,
    subgroup_size      INTEGER           NOT NULL,
    measurements       TEXT              NOT NULL,
    cp                 DOUBLE PRECISION  NOT NULL,
    cpk                DOUBLE PRECISION  NOT NULL,
    capable_threshold  NUMERIC(4, 2)     NOT NULL,
    watch_threshold    NUMERIC(4, 2)     NOT NULL,
    created_by         UUID,
    created_at         TIMESTAMPTZ       NOT NULL,
    updated_at         TIMESTAMPTZ       NOT NULL,

    CONSTRAINT pk_spc_capability_studies PRIMARY KEY (id),
    CONSTRAINT chk_spc_studies_limits CHECK (usl > lsl),
    CONSTRAINT chk_spc_studies_subgroup CHECK (subgroup_size BETWEEN 2 AND 10),
    CONSTRAINT chk_spc_studies_thresholds CHECK (watch_threshold > 0 AND watch_threshold < capable_threshold)
);

CREATE INDEX ix_spc_capability_studies_tenant ON spc_capability_studies (tenant_id, study_date DESC);
