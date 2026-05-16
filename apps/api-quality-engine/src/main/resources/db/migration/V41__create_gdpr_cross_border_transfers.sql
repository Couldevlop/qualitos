-- GDPR Chapitre V (Art. 44-49) — Cross-border transfers register.
-- Recense les transferts internationaux avec leur mécanisme de garantie.

CREATE TABLE gdpr_cross_border_transfers (
    id                                  UUID         NOT NULL,
    tenant_id                           UUID         NOT NULL,
    reference                           VARCHAR(64)  NOT NULL,
    recipient_name                      VARCHAR(250) NOT NULL,
    recipient_legal_entity              VARCHAR(250),
    recipient_contact                   VARCHAR(250),
    destination_countries               VARCHAR(500),
    mechanism                           VARCHAR(64)  NOT NULL,
    safeguards_description              VARCHAR(4000),
    safeguards_document_url             VARCHAR(1024),
    derogation_justification            VARCHAR(4000),
    data_categories                     VARCHAR(2000),
    linked_processing_activity_ids      VARCHAR(4000),
    linked_processor_agreement_ids      VARCHAR(4000),
    status                              VARCHAR(32)  NOT NULL,
    effective_from                      TIMESTAMPTZ,
    effective_to                        TIMESTAMPTZ,
    suspended_at                        TIMESTAMPTZ,
    suspension_reason                   VARCHAR(2000),
    termination_reason                  VARCHAR(2000),
    created_by                          UUID         NOT NULL,
    created_at                          TIMESTAMPTZ  NOT NULL,
    updated_at                          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_gdpr_cross_border_transfers PRIMARY KEY (id),
    CONSTRAINT uq_cbt_tenant_reference UNIQUE (tenant_id, reference),
    CONSTRAINT chk_cbt_status CHECK
        (status IN ('DRAFT','ACTIVE','SUSPENDED','TERMINATED')),
    CONSTRAINT chk_cbt_mechanism CHECK
        (mechanism IN ('ADEQUACY_DECISION','STANDARD_CONTRACTUAL_CLAUSES',
                       'BINDING_CORPORATE_RULES','CODE_OF_CONDUCT',
                       'CERTIFICATION','DEROGATION_ART49')),
    CONSTRAINT chk_cbt_reference CHECK
        (reference ~ '^[A-Z][A-Z0-9_-]{1,63}$'),
    -- ACTIVE ⇒ effective_from, destinationCountries non vide, safeguards non vides
    CONSTRAINT chk_cbt_active_has_metadata CHECK
        (status <> 'ACTIVE'
         OR (effective_from IS NOT NULL
             AND destination_countries IS NOT NULL
             AND length(trim(destination_countries)) > 0
             AND safeguards_description IS NOT NULL
             AND length(trim(safeguards_description)) > 0)),
    -- DEROGATION_ART49 en ACTIVE/SUSPENDED ⇒ derogation_justification non vide
    CONSTRAINT chk_cbt_derogation_justified CHECK
        (mechanism <> 'DEROGATION_ART49'
         OR status NOT IN ('ACTIVE','SUSPENDED')
         OR (derogation_justification IS NOT NULL
             AND length(trim(derogation_justification)) > 0)),
    -- SUSPENDED ⇒ suspended_at + reason
    CONSTRAINT chk_cbt_suspended_has_reason CHECK
        (status <> 'SUSPENDED'
         OR (suspended_at IS NOT NULL
             AND suspension_reason IS NOT NULL
             AND length(trim(suspension_reason)) > 0)),
    -- TERMINATED ⇒ effective_to + reason
    CONSTRAINT chk_cbt_terminated_has_reason CHECK
        (status <> 'TERMINATED'
         OR (effective_to IS NOT NULL
             AND termination_reason IS NOT NULL
             AND length(trim(termination_reason)) > 0)),
    CONSTRAINT chk_cbt_to_after_from CHECK
        (effective_to IS NULL OR effective_from IS NULL
         OR effective_to >= effective_from)
);

CREATE INDEX idx_cbt_tenant        ON gdpr_cross_border_transfers (tenant_id);
CREATE INDEX idx_cbt_tenant_status ON gdpr_cross_border_transfers (tenant_id, status);

-- Index partiel pour le pipeline "DEROGATION_ART49 actives à surveiller".
CREATE INDEX idx_cbt_derogations_active
    ON gdpr_cross_border_transfers (tenant_id)
    WHERE mechanism = 'DEROGATION_ART49' AND status IN ('ACTIVE','SUSPENDED');

COMMENT ON TABLE  gdpr_cross_border_transfers IS
    'GDPR Chap. V cross-border transfers register (Art. 44-49 mechanisms).';
COMMENT ON COLUMN gdpr_cross_border_transfers.mechanism IS
    'Adequacy / SCC / BCR / Code of conduct / Certification / Art. 49 derogation.';
COMMENT ON COLUMN gdpr_cross_border_transfers.derogation_justification IS
    'Required when mechanism=DEROGATION_ART49 (Art. 49 = exception, must be motivated).';
