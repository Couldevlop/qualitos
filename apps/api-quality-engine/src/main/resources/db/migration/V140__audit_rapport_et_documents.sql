-- Rapport d'audit, documents joints et nom de l'auditeur responsable (ADR 0089).
--
-- Les deux nouvelles tables portent une colonne tenant_id : la politique
-- d'isolation par client (ADR 0086) leur est posée automatiquement après la
-- migration, comme à toute table à client. Aucune politique n'est donc écrite
-- ici, à dessein : une liste tenue à la main finit toujours par en oublier une.

-- ---------------------------------------------------------------------------
-- 1. Le nom de l'auditeur responsable, figé à la désignation
-- ---------------------------------------------------------------------------
--
-- Le moteur qualité n'a pas d'annuaire : sans nom recopié, l'écran n'avait que
-- l'identifiant du compte à montrer. Le nom est recopié au moment où l'on
-- désigne l'auditeur, comme le vérificateur d'une CAPA. Nullable : les audits
-- créés avant cette migration n'en ont pas, et en inventer un serait pire.
ALTER TABLE audit_plans
    ADD COLUMN lead_auditor_name VARCHAR(255);

-- ---------------------------------------------------------------------------
-- 2. Le rapport d'audit : un brouillon, puis une émission qui le fige
-- ---------------------------------------------------------------------------
--
-- La synthèse rédigée n'est pas ici : elle reste dans audit_plans.report_summary,
-- où l'écrivent déjà la génération par IA et la clôture. La dupliquer ferait
-- deux synthèses dont personne ne saurait laquelle fait foi.
--
-- Les chiffres clés sont recopiés À L'ÉMISSION : un constat modifié après coup
-- ne doit pas réécrire un rapport déjà remis à la direction ou à un organisme.
CREATE TABLE audit_reports (
    id                    UUID          NOT NULL DEFAULT gen_random_uuid(),
    tenant_id             UUID          NOT NULL,
    plan_id               UUID          NOT NULL,
    status                VARCHAR(10)   NOT NULL DEFAULT 'DRAFT',
    strengths             TEXT,
    improvements          TEXT,
    conclusion            VARCHAR(40),
    -- Instantané figé à l'émission (NULL tant que le rapport est un brouillon).
    major_nc_count        INTEGER,
    minor_nc_count        INTEGER,
    observation_count     INTEGER,
    opportunity_count     INTEGER,
    conformity_count      INTEGER,
    conformity_score      DOUBLE PRECISION,
    issued_summary        TEXT,
    content_sha256        VARCHAR(64),
    issued_at             TIMESTAMPTZ,
    -- Émetteur lu dans le jeton, jamais dans le corps de la requête (§18.2 #2).
    issued_by             UUID,
    issued_by_name        VARCHAR(255),
    updated_by            UUID,
    created_at            TIMESTAMPTZ   NOT NULL,
    updated_at            TIMESTAMPTZ   NOT NULL,
    -- Deux émissions simultanées : la seconde échoue au lieu d'écraser la première.
    version               BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_audit_reports PRIMARY KEY (id),
    CONSTRAINT fk_audit_reports_plan FOREIGN KEY (plan_id)
        REFERENCES audit_plans (id) ON DELETE CASCADE,
    -- Un audit, un rapport.
    CONSTRAINT uq_audit_reports_plan UNIQUE (plan_id),
    CONSTRAINT chk_audit_reports_status CHECK (status IN ('DRAFT', 'ISSUED')),
    CONSTRAINT chk_audit_reports_conclusion CHECK (conclusion IS NULL OR conclusion IN (
        'FAVORABLE', 'FAVORABLE_WITH_RESERVATIONS', 'UNFAVORABLE')),
    -- Un rapport émis porte toujours sa date, son émetteur, sa conclusion et son empreinte.
    CONSTRAINT chk_audit_reports_issued CHECK (status = 'DRAFT' OR (
        issued_at IS NOT NULL AND issued_by IS NOT NULL
        AND conclusion IS NOT NULL AND content_sha256 IS NOT NULL))
);

CREATE INDEX ix_audit_reports_tenant ON audit_reports (tenant_id);

-- ---------------------------------------------------------------------------
-- 3. Les documents joints à un audit
-- ---------------------------------------------------------------------------
--
-- Même partage que les preuves CAPA : la métadonnée ici, le binaire dans le
-- stockage objet sous une clé tenantisée. L'extension de la clé vient du type
-- validé, jamais du nom de fichier du client.
CREATE TABLE audit_documents (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id         UUID         NOT NULL,
    plan_id           UUID         NOT NULL,
    object_key        VARCHAR(512) NOT NULL,
    content_type      VARCHAR(150) NOT NULL,
    size_bytes        BIGINT       NOT NULL,
    original_filename VARCHAR(255),
    -- Nullable : un sujet de jeton non exploitable ne doit pas fabriquer un auteur.
    uploaded_by       UUID,
    created_at        TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_audit_documents PRIMARY KEY (id),
    CONSTRAINT fk_audit_documents_plan FOREIGN KEY (plan_id)
        REFERENCES audit_plans (id) ON DELETE CASCADE,
    -- Une clé, une ligne : sinon retirer l'une effacerait le binaire de l'autre.
    CONSTRAINT uq_audit_documents_object_key UNIQUE (object_key),
    CONSTRAINT ck_audit_documents_size CHECK (size_bytes > 0)
);

CREATE INDEX ix_audit_documents_tenant_plan
    ON audit_documents (tenant_id, plan_id, created_at);
