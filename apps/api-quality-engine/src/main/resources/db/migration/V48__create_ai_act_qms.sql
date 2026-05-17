-- AI Act (UE 2024/1689) — Quality Management System (Art. 17).
-- Documentation QMS pour les providers de systèmes IA HIGH-risk.

CREATE TABLE ai_act_qms (
    id                                       UUID         NOT NULL,
    tenant_id                                UUID         NOT NULL,
    reference                                VARCHAR(64)  NOT NULL,
    version                                  VARCHAR(32)  NOT NULL,
    name                                     VARCHAR(250) NOT NULL,
    description                              VARCHAR(4000),
    regulatory_compliance_strategy           VARCHAR(8000),
    design_control_description               VARCHAR(8000),
    quality_control_description              VARCHAR(8000),
    data_management_description              VARCHAR(8000),
    risk_management_description              VARCHAR(8000),
    pmm_description                          VARCHAR(8000),
    regulator_communication_description      VARCHAR(8000),
    resource_management_description          VARCHAR(8000),
    supplier_monitoring_description          VARCHAR(8000),
    covered_ai_system_ids                    VARCHAR(8000),
    status                                   VARCHAR(32)  NOT NULL,
    submitted_at                             TIMESTAMPTZ,
    submitted_by                             UUID,
    approved_at                              TIMESTAMPTZ,
    approved_by                              UUID,
    approval_notes                           VARCHAR(4000),
    effective_from                           TIMESTAMPTZ,
    effective_to                             TIMESTAMPTZ,
    superseded_by_qms_id                     UUID,
    archived_reason                          VARCHAR(2000),
    created_by                               UUID         NOT NULL,
    created_at                               TIMESTAMPTZ  NOT NULL,
    updated_at                               TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_ai_act_qms PRIMARY KEY (id),
    CONSTRAINT uq_aqms_tenant_ref_version UNIQUE (tenant_id, reference, version),
    CONSTRAINT chk_aqms_status CHECK
        (status IN ('DRAFT','APPROVED','IN_FORCE','SUPERSEDED','ARCHIVED')),
    CONSTRAINT chk_aqms_reference CHECK
        (reference ~ '^[A-Z][A-Z0-9_-]{1,63}$'),
    CONSTRAINT chk_aqms_version CHECK
        (version ~ '^\d+\.\d+(\.\d+)?$'),
    -- APPROVED/IN_FORCE/SUPERSEDED ⇒ descriptions obligatoires non blanches
    CONSTRAINT chk_aqms_approved_has_descriptions CHECK
        (status NOT IN ('APPROVED','IN_FORCE','SUPERSEDED')
         OR (regulatory_compliance_strategy IS NOT NULL
             AND length(trim(regulatory_compliance_strategy)) > 0
             AND design_control_description IS NOT NULL
             AND length(trim(design_control_description)) > 0
             AND quality_control_description IS NOT NULL
             AND length(trim(quality_control_description)) > 0
             AND data_management_description IS NOT NULL
             AND length(trim(data_management_description)) > 0
             AND risk_management_description IS NOT NULL
             AND length(trim(risk_management_description)) > 0
             AND pmm_description IS NOT NULL
             AND length(trim(pmm_description)) > 0
             AND regulator_communication_description IS NOT NULL
             AND length(trim(regulator_communication_description)) > 0)),
    -- APPROVED/IN_FORCE/SUPERSEDED ⇒ submitter + approver + dates
    CONSTRAINT chk_aqms_approval_has_actors CHECK
        (status NOT IN ('APPROVED','IN_FORCE','SUPERSEDED')
         OR (submitted_at IS NOT NULL AND submitted_by IS NOT NULL
             AND approved_at IS NOT NULL AND approved_by IS NOT NULL)),
    -- Segregation of duties : approver ≠ submitter
    CONSTRAINT chk_aqms_approver_differs_submitter CHECK
        (approved_by IS NULL OR submitted_by IS NULL OR approved_by <> submitted_by),
    -- IN_FORCE/SUPERSEDED ⇒ effective_from
    CONSTRAINT chk_aqms_in_force_has_from CHECK
        (status NOT IN ('IN_FORCE','SUPERSEDED') OR effective_from IS NOT NULL),
    -- SUPERSEDED ⇒ supersededBy + effective_to
    CONSTRAINT chk_aqms_superseded_has_successor_and_to CHECK
        (status <> 'SUPERSEDED'
         OR (superseded_by_qms_id IS NOT NULL AND effective_to IS NOT NULL)),
    -- ARCHIVED ⇒ effective_to + archive reason
    CONSTRAINT chk_aqms_archived_has_to_and_reason CHECK
        (status <> 'ARCHIVED'
         OR (effective_to IS NOT NULL AND archived_reason IS NOT NULL
             AND length(trim(archived_reason)) > 0))
);

CREATE INDEX idx_aqms_tenant        ON ai_act_qms (tenant_id);
CREATE INDEX idx_aqms_tenant_status ON ai_act_qms (tenant_id, status);

-- Index partiel : QMS en force — utilisation rapide pour les audits.
CREATE INDEX idx_aqms_in_force
    ON ai_act_qms (tenant_id, effective_from)
    WHERE status = 'IN_FORCE';

COMMENT ON TABLE  ai_act_qms IS
    'AI Act (UE 2024/1689 Art. 17) Quality Management Systems for HIGH-risk AI providers.';
COMMENT ON COLUMN ai_act_qms.covered_ai_system_ids IS
    'CSV of ai_act_systems.id UUIDs covered by this QMS version.';
