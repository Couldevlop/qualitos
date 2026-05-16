-- GDPR Art. 13/14 — Privacy notices register.
-- Mentions d'information versionnées et multilingues présentées aux personnes
-- concernées au moment de la collecte (Art. 13) ou plus tard (Art. 14).

CREATE TABLE gdpr_privacy_notices (
    id                              UUID         NOT NULL,
    tenant_id                       UUID         NOT NULL,
    reference                       VARCHAR(64)  NOT NULL,
    version                         VARCHAR(32)  NOT NULL,
    language                        VARCHAR(2)   NOT NULL,
    title                           VARCHAR(250) NOT NULL,
    summary                         VARCHAR(2000),
    content_markdown                TEXT,
    linked_processing_activity_ids  VARCHAR(4000),
    publish_url                     VARCHAR(1024),
    contact_name                    VARCHAR(250),
    contact_email                   VARCHAR(250),
    status                          VARCHAR(32)  NOT NULL,
    effective_from                  TIMESTAMPTZ,
    effective_to                    TIMESTAMPTZ,
    published_at                    TIMESTAMPTZ,
    published_by                    UUID,
    created_by                      UUID         NOT NULL,
    created_at                      TIMESTAMPTZ  NOT NULL,
    updated_at                      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_gdpr_privacy_notices PRIMARY KEY (id),
    CONSTRAINT uq_pn_tenant_ref_version_lang
        UNIQUE (tenant_id, reference, version, language),
    CONSTRAINT chk_pn_status CHECK
        (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    CONSTRAINT chk_pn_reference CHECK
        (reference ~ '^[A-Z][A-Z0-9_-]{1,63}$'),
    CONSTRAINT chk_pn_version CHECK
        (version ~ '^[A-Za-z0-9._:-]{1,32}$'),
    CONSTRAINT chk_pn_language CHECK
        (language ~ '^[a-z]{2}$'),
    CONSTRAINT chk_pn_published_has_metadata CHECK
        (status <> 'PUBLISHED' OR
         (published_at IS NOT NULL AND published_by IS NOT NULL
          AND effective_from IS NOT NULL
          AND summary IS NOT NULL AND length(trim(summary)) > 0
          AND content_markdown IS NOT NULL AND length(trim(content_markdown)) > 0)),
    CONSTRAINT chk_pn_archived_has_to CHECK
        (status <> 'ARCHIVED' OR effective_to IS NOT NULL),
    CONSTRAINT chk_pn_to_after_from CHECK
        (effective_to IS NULL OR effective_from IS NULL
         OR effective_to >= effective_from)
);

CREATE INDEX idx_pn_tenant        ON gdpr_privacy_notices (tenant_id);
CREATE INDEX idx_pn_tenant_status ON gdpr_privacy_notices (tenant_id, status);
CREATE INDEX idx_pn_tenant_ref_lang
    ON gdpr_privacy_notices (tenant_id, reference, language);

-- Invariant fort : au plus une PUBLISHED par (tenant, reference, language).
CREATE UNIQUE INDEX uq_pn_published_per_ref_lang
    ON gdpr_privacy_notices (tenant_id, reference, language)
    WHERE status = 'PUBLISHED';

COMMENT ON TABLE  gdpr_privacy_notices IS
    'GDPR Art. 13/14 privacy notices. Versioned & multilingual. At most one PUBLISHED per (reference, language).';
COMMENT ON COLUMN gdpr_privacy_notices.language IS
    'ISO 639-1 alpha-2 lowercase (en, fr, es, ...).';
COMMENT ON COLUMN gdpr_privacy_notices.linked_processing_activity_ids IS
    'CSV of UUIDs linking to gdpr_processing_activities.id.';
