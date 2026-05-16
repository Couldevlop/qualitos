-- NIS2 Art. 21.2 — Mandatory cybersecurity risk management measures.
-- 10 catégories couvertes par l'enum category.

CREATE TABLE nis2_risk_measures (
    id                                  UUID         NOT NULL,
    tenant_id                           UUID         NOT NULL,
    reference                           VARCHAR(64)  NOT NULL,
    category                            VARCHAR(32)  NOT NULL,
    title                               VARCHAR(250) NOT NULL,
    description                         VARCHAR(4000),
    status                              VARCHAR(32)  NOT NULL,
    owner_user_id                       UUID,
    maturity_level                      INTEGER      NOT NULL,
    residual_risk_rating                VARCHAR(32)  NOT NULL,
    critical_risk_justification         VARCHAR(4000),
    review_interval_days                INTEGER      NOT NULL,
    effective_from                      TIMESTAMPTZ,
    effective_to                        TIMESTAMPTZ,
    last_reviewed_at                    TIMESTAMPTZ,
    reviewed_by                         UUID,
    next_review_due_at                  TIMESTAMPTZ,
    evidence_urls                       VARCHAR(4000),
    linked_processing_activity_ids      VARCHAR(4000),
    linked_processor_agreement_ids      VARCHAR(4000),
    notes                               VARCHAR(4000),
    created_by                          UUID         NOT NULL,
    created_at                          TIMESTAMPTZ  NOT NULL,
    updated_at                          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_nis2_risk_measures PRIMARY KEY (id),
    CONSTRAINT uq_nis2m_tenant_reference UNIQUE (tenant_id, reference),
    CONSTRAINT chk_nis2m_status CHECK
        (status IN ('PLANNED','IN_PROGRESS','IMPLEMENTED','VERIFIED','DEPRECATED')),
    CONSTRAINT chk_nis2m_category CHECK
        (category IN ('RISK_ANALYSIS','INCIDENT_HANDLING','BUSINESS_CONTINUITY',
                      'SUPPLY_CHAIN_SECURITY','SECURE_DEVELOPMENT',
                      'EFFECTIVENESS_ASSESSMENT','CYBER_HYGIENE_TRAINING',
                      'CRYPTOGRAPHY','HR_AND_ACCESS_CONTROL',
                      'MFA_AND_COMMUNICATIONS')),
    CONSTRAINT chk_nis2m_risk CHECK
        (residual_risk_rating IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT chk_nis2m_reference CHECK
        (reference ~ '^[A-Z][A-Z0-9_-]{1,63}$'),
    CONSTRAINT chk_nis2m_maturity_range CHECK
        (maturity_level BETWEEN 1 AND 5),
    CONSTRAINT chk_nis2m_review_interval_range CHECK
        (review_interval_days BETWEEN 30 AND 1095),
    -- CRITICAL residual ⇒ justification non vide
    CONSTRAINT chk_nis2m_critical_has_justification CHECK
        (residual_risk_rating <> 'CRITICAL'
         OR (critical_risk_justification IS NOT NULL
             AND length(trim(critical_risk_justification)) > 0)),
    -- VERIFIED ⇒ last_reviewed_at + reviewed_by + next_review_due_at
    CONSTRAINT chk_nis2m_verified_has_review CHECK
        (status <> 'VERIFIED'
         OR (last_reviewed_at IS NOT NULL
             AND reviewed_by IS NOT NULL
             AND next_review_due_at IS NOT NULL)),
    -- DEPRECATED ⇒ effective_to
    CONSTRAINT chk_nis2m_deprecated_has_to CHECK
        (status <> 'DEPRECATED' OR effective_to IS NOT NULL),
    CONSTRAINT chk_nis2m_to_after_from CHECK
        (effective_to IS NULL OR effective_from IS NULL
         OR effective_to >= effective_from)
);

CREATE INDEX idx_nis2m_tenant          ON nis2_risk_measures (tenant_id);
CREATE INDEX idx_nis2m_tenant_status   ON nis2_risk_measures (tenant_id, status);
CREATE INDEX idx_nis2m_tenant_category ON nis2_risk_measures (tenant_id, category);
CREATE INDEX idx_nis2m_next_review     ON nis2_risk_measures (next_review_due_at);

-- Index partiel pour le scan "review overdue" (non DEPRECATED, deadline passée).
CREATE INDEX idx_nis2m_review_overdue
    ON nis2_risk_measures (next_review_due_at)
    WHERE next_review_due_at IS NOT NULL AND status <> 'DEPRECATED';

-- Index pour le pipeline "CRITICAL residual non DEPRECATED" — escalade direction.
CREATE INDEX idx_nis2m_critical_active
    ON nis2_risk_measures (tenant_id, status)
    WHERE residual_risk_rating = 'CRITICAL' AND status <> 'DEPRECATED';

COMMENT ON TABLE  nis2_risk_measures IS
    'NIS2 Art. 21.2 mandatory cybersecurity measures register.';
COMMENT ON COLUMN nis2_risk_measures.maturity_level IS
    'CMMI-like 1..5 maturity scale.';
COMMENT ON COLUMN nis2_risk_measures.review_interval_days IS
    'Periodic review cycle (30..1095 days = 1 month .. 3 years).';
