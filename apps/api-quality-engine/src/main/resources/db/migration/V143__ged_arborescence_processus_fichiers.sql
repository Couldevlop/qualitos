-- GED : arborescence, processus, revue périodique et fichiers des versions (ADR 0094).
--
-- Les nouvelles tables portent une colonne tenant_id : la politique d'isolation
-- par client (ADR 0086) leur est posée automatiquement après la migration.

-- ---------------------------------------------------------------------------
-- 1. Le paramétrage GED d'un client
-- ---------------------------------------------------------------------------
--
-- Une ligne par client, écrite par l'administrateur du client. Sa présence dit
-- aussi que l'arborescence et les processus par défaut ont été semés : sans ce
-- témoin, un client qui supprimerait tous ses dossiers les verrait renaître.
CREATE TABLE document_settings (
    tenant_id                 UUID         NOT NULL,
    review_period_months      INTEGER      NOT NULL DEFAULT 24,
    review_warning_days       INTEGER      NOT NULL DEFAULT 30,
    max_file_mb               INTEGER      NOT NULL DEFAULT 25,
    -- NULL : pas de quota.
    quota_mb                  INTEGER,
    -- NULL : les fichiers des versions obsolètes sont gardés sans limite.
    obsolete_retention_years  INTEGER,
    updated_by                UUID,
    created_at                TIMESTAMPTZ  NOT NULL,
    updated_at                TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_document_settings PRIMARY KEY (tenant_id),
    CONSTRAINT chk_doc_settings_review CHECK (review_period_months BETWEEN 1 AND 120),
    CONSTRAINT chk_doc_settings_warning CHECK (review_warning_days BETWEEN 0 AND 365),
    CONSTRAINT chk_doc_settings_max_file CHECK (max_file_mb BETWEEN 1 AND 50),
    CONSTRAINT chk_doc_settings_quota CHECK (quota_mb IS NULL OR quota_mb >= 1),
    CONSTRAINT chk_doc_settings_retention CHECK (obsolete_retention_years IS NULL
        OR obsolete_retention_years BETWEEN 1 AND 100)
);

-- ---------------------------------------------------------------------------
-- 2. L'arborescence documentaire
-- ---------------------------------------------------------------------------
CREATE TABLE document_folders (
    id          UUID          NOT NULL DEFAULT gen_random_uuid(),
    tenant_id   UUID          NOT NULL,
    parent_id   UUID,
    name        VARCHAR(120)  NOT NULL,
    position    INTEGER       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,

    CONSTRAINT pk_document_folders PRIMARY KEY (id),
    -- Un dossier non vide ne se supprime pas (le service répond 409) ; la clé
    -- étrangère le garantit aussi si un appel passait outre.
    CONSTRAINT fk_document_folders_parent FOREIGN KEY (parent_id)
        REFERENCES document_folders (id) ON DELETE RESTRICT,
    -- Deux dossiers frères ne portent pas le même nom, à la racine comme ailleurs.
    CONSTRAINT uq_document_folders_name UNIQUE NULLS NOT DISTINCT (tenant_id, parent_id, name)
);

CREATE INDEX ix_document_folders_tenant ON document_folders (tenant_id, parent_id, position);

-- ---------------------------------------------------------------------------
-- 3. Les processus auxquels un document se rattache
-- ---------------------------------------------------------------------------
CREATE TABLE document_processes (
    id          UUID          NOT NULL DEFAULT gen_random_uuid(),
    tenant_id   UUID          NOT NULL,
    name        VARCHAR(120)  NOT NULL,
    position    INTEGER       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,

    CONSTRAINT pk_document_processes PRIMARY KEY (id),
    CONSTRAINT uq_document_processes_name UNIQUE (tenant_id, name)
);

CREATE INDEX ix_document_processes_tenant ON document_processes (tenant_id, position);

-- ---------------------------------------------------------------------------
-- 4. Les fichiers, stockés une fois par contenu
-- ---------------------------------------------------------------------------
--
-- Le binaire vit dans le stockage objet sous une clé dérivée de son empreinte :
-- deux versions au même fichier partagent un seul objet. L'empreinte et la
-- taille d'origine décrivent le fichier tel que déposé ; stored_size et
-- content_encoding disent ce qui est réellement conservé (compressé ou non).
CREATE TABLE document_blobs (
    id                UUID          NOT NULL DEFAULT gen_random_uuid(),
    tenant_id         UUID          NOT NULL,
    sha256            VARCHAR(64)   NOT NULL,
    object_key        VARCHAR(512)  NOT NULL,
    content_type      VARCHAR(150)  NOT NULL,
    original_size     BIGINT        NOT NULL,
    stored_size       BIGINT        NOT NULL,
    content_encoding  VARCHAR(10),
    created_at        TIMESTAMPTZ   NOT NULL,

    CONSTRAINT pk_document_blobs PRIMARY KEY (id),
    CONSTRAINT uq_document_blobs_sha UNIQUE (tenant_id, sha256),
    CONSTRAINT uq_document_blobs_object_key UNIQUE (object_key),
    CONSTRAINT chk_document_blobs_sha CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_document_blobs_size CHECK (original_size > 0 AND stored_size > 0),
    CONSTRAINT chk_document_blobs_encoding CHECK (content_encoding IS NULL OR content_encoding = 'gzip')
);

-- ---------------------------------------------------------------------------
-- 5. Les documents : dossier, processus, cycle de vie et revue périodique
-- ---------------------------------------------------------------------------
ALTER TABLE documents
    ADD COLUMN folder_id         UUID,
    ADD COLUMN process_id        UUID,
    -- DRAFT : jamais publié ; IN_FORCE : une version publiée, rien en cours ;
    -- IN_REVISION : une version publiée et une suivante en préparation.
    ADD COLUMN lifecycle         VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN next_review_date  DATE,
    ADD CONSTRAINT fk_documents_folder FOREIGN KEY (folder_id)
        REFERENCES document_folders (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_documents_process FOREIGN KEY (process_id)
        REFERENCES document_processes (id) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_documents_lifecycle CHECK (lifecycle IN ('DRAFT', 'IN_FORCE', 'IN_REVISION'));

ALTER TABLE documents DROP CONSTRAINT chk_documents_type;
ALTER TABLE documents ADD CONSTRAINT chk_documents_type CHECK (
    type IN ('POLICY','PROCEDURE','WORK_INSTRUCTION','RECORD','FORM','MANUAL','EXTERNAL','OTHER'));

-- Reprise : le cycle de vie se déduit des versions existantes.
UPDATE documents d SET lifecycle = CASE
    WHEN d.current_version_id IS NULL THEN 'DRAFT'
    WHEN EXISTS (SELECT 1 FROM document_versions v
                 WHERE v.document_id = d.id AND v.status IN ('DRAFT', 'IN_REVIEW', 'APPROVED')) THEN 'IN_REVISION'
    ELSE 'IN_FORCE'
END;

-- Reprise : la prochaine revue des documents publiés, au délai par défaut.
UPDATE documents d SET next_review_date = (v.published_at + INTERVAL '24 months')::date
FROM document_versions v
WHERE v.id = d.current_version_id AND v.published_at IS NOT NULL;

CREATE INDEX ix_documents_tenant_folder ON documents (tenant_id, folder_id);
CREATE INDEX ix_documents_tenant_review ON documents (tenant_id, next_review_date)
    WHERE status = 'ACTIVE' AND lifecycle <> 'DRAFT';

-- ---------------------------------------------------------------------------
-- 6. Les versions : leur fichier, et la date où elles sont devenues obsolètes
-- ---------------------------------------------------------------------------
ALTER TABLE document_versions
    ADD COLUMN blob_id          UUID,
    ADD COLUMN file_name        VARCHAR(255),
    ADD COLUMN obsoleted_at     TIMESTAMPTZ,
    -- Le fichier d'une version obsolète a été retiré au terme de sa conservation ;
    -- l'empreinte (content_hash) et l'ancrage restent, comme preuve.
    ADD COLUMN file_purged_at   TIMESTAMPTZ,
    ADD CONSTRAINT fk_document_versions_blob FOREIGN KEY (blob_id)
        REFERENCES document_blobs (id) ON DELETE RESTRICT;

CREATE INDEX ix_document_versions_blob ON document_versions (blob_id) WHERE blob_id IS NOT NULL;
CREATE INDEX ix_document_versions_obsolete ON document_versions (obsoleted_at)
    WHERE status = 'OBSOLETE' AND blob_id IS NOT NULL;

-- Reprise : une version déjà obsolète l'est depuis, au plus tard, maintenant.
UPDATE document_versions SET obsoleted_at = updated_at WHERE status = 'OBSOLETE';
