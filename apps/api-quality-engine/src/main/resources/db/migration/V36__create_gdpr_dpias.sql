-- GDPR Art. 35 — Data Protection Impact Assessment (DPIA/AIPD).
-- Workflow DPO : DRAFT → IN_PROGRESS → DPO_REVIEW → APPROVED|REJECTED → ARCHIVED.
-- Niveau de risque résiduel HIGH/SEVERE ⇒ consultation Art. 36 requise.

CREATE TABLE gdpr_dpias (
    id                                  UUID         NOT NULL,
    tenant_id                           UUID         NOT NULL,
    reference                           VARCHAR(64)  NOT NULL,
    title                               VARCHAR(250) NOT NULL,
    description                         VARCHAR(4000),
    linked_processing_activity_ids      VARCHAR(4000),
    necessity_notes                     VARCHAR(8000),
    risks_to_rights                     VARCHAR(8000),
    mitigation_measures                 VARCHAR(8000),
    overall_risk_level                  VARCHAR(32)  NOT NULL,
    consultation_required               BOOLEAN      NOT NULL DEFAULT FALSE,
    consultation_notes                  VARCHAR(8000),
    status                              VARCHAR(32)  NOT NULL,
    dpo_user_id                         UUID,
    dpo_opinion                         VARCHAR(8000),
    dpo_opinion_at                      TIMESTAMPTZ,
    effective_from                      TIMESTAMPTZ,
    effective_to                        TIMESTAMPTZ,
    created_by                          UUID         NOT NULL,
    handled_by                          UUID,
    created_at                          TIMESTAMPTZ  NOT NULL,
    updated_at                          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_gdpr_dpias PRIMARY KEY (id),
    CONSTRAINT uq_dpia_tenant_reference UNIQUE (tenant_id, reference),
    CONSTRAINT chk_dpia_status CHECK
        (status IN ('DRAFT','IN_PROGRESS','DPO_REVIEW','APPROVED','REJECTED','ARCHIVED')),
    CONSTRAINT chk_dpia_risk_level CHECK
        (overall_risk_level IN ('LOW','MEDIUM','HIGH','SEVERE')),
    CONSTRAINT chk_dpia_reference CHECK
        (reference ~ '^[A-Z][A-Z0-9_-]{1,63}$'),
    -- APPROVED/REJECTED ⇒ opinion DPO renseignée.
    CONSTRAINT chk_dpia_opinion_when_decided CHECK
        (status NOT IN ('APPROVED','REJECTED')
         OR (dpo_user_id IS NOT NULL AND dpo_opinion IS NOT NULL
             AND length(trim(dpo_opinion)) > 0 AND dpo_opinion_at IS NOT NULL)),
    -- APPROVED ⇒ effective_from renseigné.
    CONSTRAINT chk_dpia_approved_has_from CHECK
        (status <> 'APPROVED' OR effective_from IS NOT NULL),
    CONSTRAINT chk_dpia_archived_has_to CHECK
        (status <> 'ARCHIVED' OR effective_to IS NOT NULL),
    CONSTRAINT chk_dpia_to_after_from CHECK
        (effective_to IS NULL OR effective_from IS NULL
         OR effective_to >= effective_from),
    -- Si consultation_required ⇒ consultation_notes non vides.
    CONSTRAINT chk_dpia_consultation_has_notes CHECK
        ((NOT consultation_required)
         OR (consultation_notes IS NOT NULL AND length(trim(consultation_notes)) > 0))
);

CREATE INDEX idx_dpia_tenant        ON gdpr_dpias (tenant_id);
CREATE INDEX idx_dpia_tenant_status ON gdpr_dpias (tenant_id, status);

-- Index partiel pour le pipeline "requiring consultation" : risque résiduel
-- élevé sur des DPIA non archivées et non rejetées.
CREATE INDEX idx_dpia_requiring_consultation
    ON gdpr_dpias (tenant_id, overall_risk_level, status)
    WHERE overall_risk_level IN ('HIGH','SEVERE')
      AND status NOT IN ('REJECTED','ARCHIVED');

COMMENT ON TABLE  gdpr_dpias IS
    'GDPR Art. 35 Data Protection Impact Assessments. DPO workflow.';
COMMENT ON COLUMN gdpr_dpias.overall_risk_level IS
    'Residual risk after mitigations. HIGH/SEVERE triggers Art. 36 prior consultation.';
COMMENT ON COLUMN gdpr_dpias.linked_processing_activity_ids IS
    'CSV of UUIDs linking to gdpr_processing_activities.id (Art. 35§7.a).';
