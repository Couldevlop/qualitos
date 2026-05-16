-- GDPR Art. 22 — Automated Decision-Making register.
-- Décisions automatisées et profilages : référentiel des traitements
-- algorithmiques affectant les personnes concernées.

CREATE TABLE gdpr_automated_decisions (
    id                              UUID         NOT NULL,
    tenant_id                       UUID         NOT NULL,
    reference                       VARCHAR(64)  NOT NULL,
    name                            VARCHAR(250) NOT NULL,
    description                     VARCHAR(4000),
    decision_type                   VARCHAR(64)  NOT NULL,
    art22_lawful_basis              VARCHAR(32),
    lawful_basis_details            VARCHAR(4000),
    input_data_categories           VARCHAR(2000),
    linked_processing_activity_ids  VARCHAR(4000),
    linked_dpia_id                  UUID,
    algorithm_description           VARCHAR(8000),
    significance_for_subject        VARCHAR(4000),
    human_review_mechanism          VARCHAR(4000),
    objection_mechanism             VARCHAR(4000),
    status                          VARCHAR(32)  NOT NULL,
    effective_from                  TIMESTAMPTZ,
    effective_to                    TIMESTAMPTZ,
    created_by                      UUID         NOT NULL,
    created_at                      TIMESTAMPTZ  NOT NULL,
    updated_at                      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_gdpr_automated_decisions PRIMARY KEY (id),
    CONSTRAINT uq_adm_tenant_reference UNIQUE (tenant_id, reference),
    CONSTRAINT chk_adm_status CHECK
        (status IN ('DRAFT','ACTIVE','DEPRECATED','ARCHIVED')),
    CONSTRAINT chk_adm_decision_type CHECK
        (decision_type IN ('PROFILING_ONLY','AUTOMATED_DECISION',
                           'AUTOMATED_DECISION_WITH_LEGAL_EFFECT')),
    CONSTRAINT chk_adm_lawful_basis CHECK
        (art22_lawful_basis IS NULL
         OR art22_lawful_basis IN ('EXPLICIT_CONSENT','CONTRACTUAL_NECESSITY','AUTHORIZED_BY_LAW')),
    CONSTRAINT chk_adm_reference CHECK
        (reference ~ '^[A-Z][A-Z0-9_-]{1,63}$'),
    -- Garde-fou Art. 22.2-22.3 : si décision automatisée à effet juridique
    -- en production (ACTIVE ou DEPRECATED), base légale + révision humaine
    -- + DPIA obligatoires.
    CONSTRAINT chk_adm_legal_effect_active CHECK
        (decision_type <> 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT'
         OR status NOT IN ('ACTIVE','DEPRECATED')
         OR (art22_lawful_basis IS NOT NULL
             AND human_review_mechanism IS NOT NULL
             AND length(trim(human_review_mechanism)) > 0
             AND linked_dpia_id IS NOT NULL)),
    -- ACTIVE / DEPRECATED ⇒ effective_from renseigné
    CONSTRAINT chk_adm_active_has_from CHECK
        (status NOT IN ('ACTIVE','DEPRECATED') OR effective_from IS NOT NULL),
    -- ARCHIVED ⇒ effective_to renseigné
    CONSTRAINT chk_adm_archived_has_to CHECK
        (status <> 'ARCHIVED' OR effective_to IS NOT NULL),
    CONSTRAINT chk_adm_to_after_from CHECK
        (effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from)
);

CREATE INDEX idx_adm_tenant        ON gdpr_automated_decisions (tenant_id);
CREATE INDEX idx_adm_tenant_status ON gdpr_automated_decisions (tenant_id, status);

-- Index partiel pour le pipeline "décisions à effet juridique en production".
CREATE INDEX idx_adm_legal_effect_in_production
    ON gdpr_automated_decisions (tenant_id)
    WHERE decision_type = 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT'
      AND status IN ('ACTIVE','DEPRECATED');

COMMENT ON TABLE  gdpr_automated_decisions IS
    'GDPR Art. 22 register of automated decisions and profiling.';
COMMENT ON COLUMN gdpr_automated_decisions.linked_dpia_id IS
    'Required for AUTOMATED_DECISION_WITH_LEGAL_EFFECT (Art. 35.3.a — DPIA mandatory).';
COMMENT ON COLUMN gdpr_automated_decisions.human_review_mechanism IS
    'Required for AUTOMATED_DECISION_WITH_LEGAL_EFFECT (Art. 22.3).';
