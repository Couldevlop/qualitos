-- GDPR Art. 28 — Data Processor Agreements (DPA).
-- Registre des contrats de sous-traitance avec leurs obligations contractuelles
-- (sécurité, sous-traitants ultérieurs, transferts hors UE, droit d'audit,
-- restitution/destruction des données, délai de notification de violation).

CREATE TABLE gdpr_processor_agreements (
    id                                  UUID         NOT NULL,
    tenant_id                           UUID         NOT NULL,
    reference                           VARCHAR(64)  NOT NULL,
    processor_name                      VARCHAR(250) NOT NULL,
    processor_legal_entity              VARCHAR(250),
    processor_contact                   VARCHAR(250),
    processor_dpo_contact               VARCHAR(250),
    processor_country                   VARCHAR(2),
    services_description                VARCHAR(4000) NOT NULL,
    sub_processor_categories            VARCHAR(2000),
    linked_processing_activity_ids      VARCHAR(4000),
    third_country_transfers             VARCHAR(500),
    transfer_safeguards                 VARCHAR(4000),
    contract_document_url               VARCHAR(1024),
    signed_at                           TIMESTAMPTZ,
    effective_from                      TIMESTAMPTZ,
    expiration_date                     TIMESTAMPTZ,
    security_measures                   VARCHAR(4000),
    breach_notification_hours           INTEGER      NOT NULL,
    audit_rights                        BOOLEAN      NOT NULL DEFAULT FALSE,
    audit_rights_notes                  VARCHAR(4000),
    data_return_or_deletion_terms       VARCHAR(4000),
    status                              VARCHAR(32)  NOT NULL,
    terminated_at                       TIMESTAMPTZ,
    termination_reason                  VARCHAR(2000),
    created_by                          UUID         NOT NULL,
    created_at                          TIMESTAMPTZ  NOT NULL,
    updated_at                          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_gdpr_processor_agreements PRIMARY KEY (id),
    CONSTRAINT uq_dpa_tenant_reference UNIQUE (tenant_id, reference),
    CONSTRAINT chk_dpa_status CHECK
        (status IN ('DRAFT','ACTIVE','TERMINATED','EXPIRED')),
    CONSTRAINT chk_dpa_reference CHECK
        (reference ~ '^[A-Z][A-Z0-9_-]{1,63}$'),
    CONSTRAINT chk_dpa_country CHECK
        (processor_country IS NULL OR processor_country ~ '^[A-Z]{2}$'),
    CONSTRAINT chk_dpa_breach_hours_range CHECK
        (breach_notification_hours BETWEEN 1 AND 720),
    CONSTRAINT chk_dpa_active_signed CHECK
        (status <> 'ACTIVE'
         OR (signed_at IS NOT NULL AND effective_from IS NOT NULL
             AND processor_contact IS NOT NULL
             AND length(trim(processor_contact)) > 0)),
    CONSTRAINT chk_dpa_terminated_has_reason CHECK
        (status <> 'TERMINATED' OR
         (terminated_at IS NOT NULL
          AND termination_reason IS NOT NULL
          AND length(trim(termination_reason)) > 0)),
    CONSTRAINT chk_dpa_expired_has_date CHECK
        (status <> 'EXPIRED' OR terminated_at IS NOT NULL),
    CONSTRAINT chk_dpa_transfer_has_safeguards CHECK
        (third_country_transfers IS NULL
         OR length(trim(third_country_transfers)) = 0
         OR (transfer_safeguards IS NOT NULL
             AND length(trim(transfer_safeguards)) > 0)),
    CONSTRAINT chk_dpa_expiration_after_effective CHECK
        (expiration_date IS NULL OR effective_from IS NULL
         OR expiration_date > effective_from)
);

CREATE INDEX idx_dpa_tenant        ON gdpr_processor_agreements (tenant_id);
CREATE INDEX idx_dpa_tenant_status ON gdpr_processor_agreements (tenant_id, status);
CREATE INDEX idx_dpa_expiration    ON gdpr_processor_agreements (expiration_date);

-- Index partiel pour le scan d'expiration (ACTIVE avec expiration définie).
CREATE INDEX idx_dpa_expirable
    ON gdpr_processor_agreements (expiration_date)
    WHERE status = 'ACTIVE' AND expiration_date IS NOT NULL;

COMMENT ON TABLE  gdpr_processor_agreements IS
    'GDPR Art. 28 Data Processor Agreements register.';
COMMENT ON COLUMN gdpr_processor_agreements.breach_notification_hours IS
    'Processor commitment to notify controller of a breach within X hours (max 720).';
COMMENT ON COLUMN gdpr_processor_agreements.linked_processing_activity_ids IS
    'CSV of UUIDs linking to gdpr_processing_activities.id (Art. 28§3 context).';
