-- AI Act (UE 2024/1689) — Fundamental Rights Impact Assessment (FRIA), Art. 27.
-- Évaluation d'impact sur les droits fondamentaux pour systèmes IA HIGH risk.

CREATE TABLE ai_act_fria (
    id                              UUID         NOT NULL,
    tenant_id                       UUID         NOT NULL,
    reference                       VARCHAR(64)  NOT NULL,
    ai_system_id                    UUID         NOT NULL,
    process_description             VARCHAR(4000) NOT NULL,
    deployment_duration_description VARCHAR(4000),
    affected_persons_categories     VARCHAR(4000) NOT NULL,
    specific_risks                  VARCHAR(4000) NOT NULL,
    mitigation_measures             VARCHAR(4000),
    human_oversight_measures        VARCHAR(4000),
    complaint_mechanism_description VARCHAR(4000),
    status                          VARCHAR(32)  NOT NULL,
    submitted_at                    TIMESTAMPTZ,
    submitted_by                    UUID,
    approved_at                     TIMESTAMPTZ,
    approved_by                     UUID,
    approval_notes                  VARCHAR(4000),
    effective_to                    TIMESTAMPTZ,
    archived_reason                 VARCHAR(2000),
    created_by                      UUID         NOT NULL,
    created_at                      TIMESTAMPTZ  NOT NULL,
    updated_at                      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_ai_act_fria PRIMARY KEY (id),
    CONSTRAINT uq_fria_tenant_reference UNIQUE (tenant_id, reference),
    CONSTRAINT chk_fria_status CHECK
        (status IN ('DRAFT','SUBMITTED','APPROVED','ARCHIVED')),
    CONSTRAINT chk_fria_reference CHECK
        (reference ~ '^[A-Z][A-Z0-9_-]{1,63}$'),
    -- SUBMITTED/APPROVED/ARCHIVED ⇒ mitigation + oversight requis
    CONSTRAINT chk_fria_advanced_has_measures CHECK
        (status = 'DRAFT'
         OR (mitigation_measures IS NOT NULL
             AND length(trim(mitigation_measures)) > 0
             AND human_oversight_measures IS NOT NULL
             AND length(trim(human_oversight_measures)) > 0)),
    -- SUBMITTED/APPROVED/ARCHIVED ⇒ submitter + submitted_at
    CONSTRAINT chk_fria_submitted_has_actor CHECK
        (status = 'DRAFT'
         OR (submitted_at IS NOT NULL AND submitted_by IS NOT NULL)),
    -- APPROVED/ARCHIVED ⇒ approver + approved_at
    CONSTRAINT chk_fria_approved_has_actor CHECK
        (status NOT IN ('APPROVED','ARCHIVED')
         OR (approved_at IS NOT NULL AND approved_by IS NOT NULL)),
    -- Segregation of duties : approver ≠ submitter
    CONSTRAINT chk_fria_approver_differs_submitter CHECK
        (approved_by IS NULL
         OR submitted_by IS NULL
         OR approved_by <> submitted_by),
    -- ARCHIVED ⇒ effective_to + reason
    CONSTRAINT chk_fria_archived_has_to_and_reason CHECK
        (status <> 'ARCHIVED'
         OR (effective_to IS NOT NULL AND archived_reason IS NOT NULL))
);

CREATE INDEX idx_fria_tenant        ON ai_act_fria (tenant_id);
CREATE INDEX idx_fria_tenant_status ON ai_act_fria (tenant_id, status);
CREATE INDEX idx_fria_tenant_system ON ai_act_fria (tenant_id, ai_system_id);

-- Index partiel : FRIA en cours (DRAFT/SUBMITTED) — pipeline de conformité.
CREATE INDEX idx_fria_pending
    ON ai_act_fria (tenant_id)
    WHERE status IN ('DRAFT','SUBMITTED');

COMMENT ON TABLE  ai_act_fria IS
    'AI Act (UE 2024/1689 Art. 27) Fundamental Rights Impact Assessments.';
COMMENT ON COLUMN ai_act_fria.ai_system_id IS
    'Lien vers ai_act_systems.id — typiquement systèmes HIGH risk déployés.';
