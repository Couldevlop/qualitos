-- GDPR Art. 30 — Record of Processing Activities (RoPA).
-- Registre des traitements pour démontrer la conformité (Art. 5§2).
-- Une activité ACTIVE est immutable ; toute modification requiert l'archivage
-- de l'activité courante et la création d'une nouvelle DRAFT.

CREATE TABLE gdpr_processing_activities (
    id                                  UUID         NOT NULL,
    tenant_id                           UUID         NOT NULL,
    reference                           VARCHAR(64)  NOT NULL,
    name                                VARCHAR(250) NOT NULL,
    purposes                            VARCHAR(4000) NOT NULL,
    lawful_basis                        VARCHAR(32)  NOT NULL,
    lawful_basis_details                VARCHAR(4000),
    controller_name                     VARCHAR(250) NOT NULL,
    controller_contact                  VARCHAR(250) NOT NULL,
    dpo_contact                         VARCHAR(250),
    joint_controller_name               VARCHAR(250),
    joint_controller_contact            VARCHAR(250),
    data_subject_categories             VARCHAR(2000),
    data_categories                     VARCHAR(2000),
    special_categories_processed        BOOLEAN      NOT NULL DEFAULT FALSE,
    special_categories_justification    VARCHAR(4000),
    recipient_categories                VARCHAR(2000),
    third_country_transfers             VARCHAR(500),
    transfer_safeguards                 VARCHAR(4000),
    linked_retention_rule_ids           VARCHAR(4000),
    technical_measures                  VARCHAR(4000),
    organizational_measures             VARCHAR(4000),
    status                              VARCHAR(32)  NOT NULL,
    effective_from                      TIMESTAMPTZ,
    effective_to                        TIMESTAMPTZ,
    created_by                          UUID         NOT NULL,
    created_at                          TIMESTAMPTZ  NOT NULL,
    updated_at                          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_gdpr_processing_activities PRIMARY KEY (id),
    CONSTRAINT uq_ropa_tenant_reference UNIQUE (tenant_id, reference),
    CONSTRAINT chk_ropa_status CHECK
        (status IN ('DRAFT','ACTIVE','ARCHIVED')),
    CONSTRAINT chk_ropa_lawful_basis CHECK
        (lawful_basis IN ('CONSENT','CONTRACT','LEGAL_OBLIGATION',
                          'VITAL_INTERESTS','PUBLIC_TASK','LEGITIMATE_INTERESTS')),
    CONSTRAINT chk_ropa_reference CHECK
        (reference ~ '^[A-Z][A-Z0-9_-]{1,63}$'),
    CONSTRAINT chk_ropa_active_has_from CHECK
        ((status <> 'ACTIVE') OR (effective_from IS NOT NULL)),
    CONSTRAINT chk_ropa_archived_has_to CHECK
        ((status <> 'ARCHIVED') OR (effective_to IS NOT NULL)),
    CONSTRAINT chk_ropa_to_after_from CHECK
        (effective_to IS NULL OR effective_from IS NULL
         OR effective_to >= effective_from),
    CONSTRAINT chk_ropa_special_has_justification CHECK
        ((NOT special_categories_processed)
         OR (special_categories_justification IS NOT NULL
             AND length(trim(special_categories_justification)) > 0)),
    CONSTRAINT chk_ropa_transfer_has_safeguards CHECK
        (third_country_transfers IS NULL
         OR length(trim(third_country_transfers)) = 0
         OR (transfer_safeguards IS NOT NULL
             AND length(trim(transfer_safeguards)) > 0))
);

CREATE INDEX idx_ropa_tenant        ON gdpr_processing_activities (tenant_id);
CREATE INDEX idx_ropa_tenant_status ON gdpr_processing_activities (tenant_id, status);

COMMENT ON TABLE  gdpr_processing_activities IS
    'GDPR Art. 30 Record of Processing Activities. ACTIVE entries are immutable.';
COMMENT ON COLUMN gdpr_processing_activities.purposes IS
    'Free-text purposes (Art. 30§1.b). Validated for length; no PII content here.';
COMMENT ON COLUMN gdpr_processing_activities.lawful_basis IS
    'GDPR Art. 6§1 enum. LEGITIMATE_INTERESTS requires lawful_basis_details (LIA).';
COMMENT ON COLUMN gdpr_processing_activities.linked_retention_rule_ids IS
    'CSV of UUIDs linking to gdpr_retention_rules.id (Art. 30§1.f).';
