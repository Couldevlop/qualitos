-- AI Act (UE 2024/1689) — AI systems register.
-- Recense les systèmes d'IA avec leur classification de risque.

CREATE TABLE ai_act_systems (
    id                                  UUID         NOT NULL,
    tenant_id                           UUID         NOT NULL,
    reference                           VARCHAR(64)  NOT NULL,
    name                                VARCHAR(250) NOT NULL,
    description                         VARCHAR(4000),
    provider_name                       VARCHAR(250),
    intended_purpose                    VARCHAR(4000) NOT NULL,
    risk_classification                 VARCHAR(32)  NOT NULL,
    role                                VARCHAR(32)  NOT NULL,
    general_purpose                     BOOLEAN      NOT NULL DEFAULT FALSE,
    status                              VARCHAR(32)  NOT NULL,
    conformity_assessment_evidence_url  VARCHAR(1024),
    ce_marking_number                   VARCHAR(250),
    human_oversight_description         VARCHAR(4000),
    transparency_measures               VARCHAR(4000),
    data_governance_notes               VARCHAR(4000),
    linked_dpia_id                      UUID,
    linked_processing_activity_ids      VARCHAR(4000),
    linked_automated_decision_ids       VARCHAR(4000),
    effective_from                      TIMESTAMPTZ,
    effective_to                        TIMESTAMPTZ,
    withdrawal_reason                   VARCHAR(2000),
    created_by                          UUID         NOT NULL,
    created_at                          TIMESTAMPTZ  NOT NULL,
    updated_at                          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_ai_act_systems PRIMARY KEY (id),
    CONSTRAINT uq_ais_tenant_reference UNIQUE (tenant_id, reference),
    CONSTRAINT chk_ais_status CHECK
        (status IN ('DRAFT','REGISTERED','IN_USE','DECOMMISSIONED','WITHDRAWN')),
    CONSTRAINT chk_ais_risk CHECK
        (risk_classification IN ('UNACCEPTABLE','HIGH','LIMITED','MINIMAL_OR_NO')),
    CONSTRAINT chk_ais_role CHECK
        (role IN ('PROVIDER','DEPLOYER','IMPORTER','DISTRIBUTOR')),
    CONSTRAINT chk_ais_reference CHECK
        (reference ~ '^[A-Z][A-Z0-9_-]{1,63}$'),
    -- UNACCEPTABLE ne peut pas atteindre REGISTERED/IN_USE (Art. 5)
    CONSTRAINT chk_ais_unacceptable_not_in_production CHECK
        (risk_classification <> 'UNACCEPTABLE'
         OR status IN ('DRAFT','WITHDRAWN')),
    -- HIGH en IN_USE ⇒ conformity assessment + human oversight requis (Art. 9-14)
    CONSTRAINT chk_ais_high_in_use_has_evidence CHECK
        (risk_classification <> 'HIGH'
         OR status <> 'IN_USE'
         OR (conformity_assessment_evidence_url IS NOT NULL
             AND length(trim(conformity_assessment_evidence_url)) > 0
             AND human_oversight_description IS NOT NULL
             AND length(trim(human_oversight_description)) > 0)),
    -- HIGH ou LIMITED en IN_USE ⇒ transparency_measures requis (Art. 13 / 50)
    CONSTRAINT chk_ais_transparency_when_required CHECK
        (status <> 'IN_USE'
         OR risk_classification NOT IN ('HIGH','LIMITED')
         OR (transparency_measures IS NOT NULL
             AND length(trim(transparency_measures)) > 0)),
    -- IN_USE ⇒ effective_from
    CONSTRAINT chk_ais_in_use_has_from CHECK
        (status <> 'IN_USE' OR effective_from IS NOT NULL),
    -- DECOMMISSIONED/WITHDRAWN ⇒ effective_to
    CONSTRAINT chk_ais_terminal_has_to CHECK
        (status NOT IN ('DECOMMISSIONED','WITHDRAWN') OR effective_to IS NOT NULL),
    CONSTRAINT chk_ais_withdrawn_has_reason CHECK
        (status <> 'WITHDRAWN' OR withdrawal_reason IS NOT NULL),
    CONSTRAINT chk_ais_to_after_from CHECK
        (effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from)
);

CREATE INDEX idx_ais_tenant        ON ai_act_systems (tenant_id);
CREATE INDEX idx_ais_tenant_status ON ai_act_systems (tenant_id, status);
CREATE INDEX idx_ais_tenant_risk   ON ai_act_systems (tenant_id, risk_classification);

-- Index partiel : systèmes HIGH en production — surveillance prioritaire.
CREATE INDEX idx_ais_high_in_use
    ON ai_act_systems (tenant_id)
    WHERE risk_classification = 'HIGH' AND status = 'IN_USE';

-- Index partiel : systèmes UNACCEPTABLE en DRAFT — alerte gouvernance.
CREATE INDEX idx_ais_unacceptable_draft
    ON ai_act_systems (tenant_id)
    WHERE risk_classification = 'UNACCEPTABLE' AND status = 'DRAFT';

COMMENT ON TABLE  ai_act_systems IS
    'AI Act (UE 2024/1689) AI systems register with risk classification.';
COMMENT ON COLUMN ai_act_systems.risk_classification IS
    'UNACCEPTABLE (prohibited Art. 5) / HIGH (Annex III) / LIMITED (Art. 50) / MINIMAL_OR_NO.';
COMMENT ON COLUMN ai_act_systems.linked_dpia_id IS
    'Optional link to gdpr_dpias.id when DPIA was conducted (often required for HIGH).';
COMMENT ON COLUMN ai_act_systems.linked_automated_decision_ids IS
    'CSV of UUIDs linking to gdpr_automated_decisions.id (GDPR Art. 22 cross-ref).';
