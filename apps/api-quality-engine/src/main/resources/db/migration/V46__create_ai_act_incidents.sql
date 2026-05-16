-- AI Act (UE 2024/1689) — Serious Incident Reporting (Art. 73).
-- Registre des incidents IA graves avec délais réglementaires.

CREATE TABLE ai_act_incidents (
    id                              UUID         NOT NULL,
    tenant_id                       UUID         NOT NULL,
    reference                       VARCHAR(64)  NOT NULL,
    ai_system_id                    UUID         NOT NULL,
    severity                        VARCHAR(64)  NOT NULL,
    description                     VARCHAR(4000) NOT NULL,
    affected_persons_description    VARCHAR(4000),
    immediate_actions_taken         VARCHAR(4000),
    occurred_at                     TIMESTAMPTZ  NOT NULL,
    detected_at                     TIMESTAMPTZ  NOT NULL,
    status                          VARCHAR(32)  NOT NULL,
    investigation_started_at        TIMESTAMPTZ,
    investigation_lead_user_id      UUID,
    root_cause_analysis             VARCHAR(4000),
    corrective_actions              VARCHAR(4000),
    notified_regulator_at           TIMESTAMPTZ,
    regulator_reference             VARCHAR(250),
    closed_at                       TIMESTAMPTZ,
    dismissed_at                    TIMESTAMPTZ,
    dismissal_reason                VARCHAR(2000),
    created_by                      UUID         NOT NULL,
    created_at                      TIMESTAMPTZ  NOT NULL,
    updated_at                      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_ai_act_incidents PRIMARY KEY (id),
    CONSTRAINT uq_aii_tenant_reference UNIQUE (tenant_id, reference),
    CONSTRAINT chk_aii_status CHECK
        (status IN ('DETECTED','INVESTIGATING','NOTIFIED_REGULATOR','CLOSED','DISMISSED')),
    CONSTRAINT chk_aii_severity CHECK
        (severity IN ('DEATH_OR_SERIOUS_HARM_TO_HEALTH',
                      'SERIOUS_INFRINGEMENT_FUNDAMENTAL_RIGHTS',
                      'CRITICAL_INFRASTRUCTURE_DISRUPTION',
                      'SERIOUS_PROPERTY_OR_ENVIRONMENTAL_DAMAGE')),
    CONSTRAINT chk_aii_reference CHECK
        (reference ~ '^[A-Z][A-Z0-9_-]{1,63}$'),
    CONSTRAINT chk_aii_occurred_before_detected CHECK (occurred_at <= detected_at),
    -- INVESTIGATING/NOTIFIED/CLOSED ⇒ investigation lead + started_at
    CONSTRAINT chk_aii_investigation_has_actor CHECK
        (status IN ('DETECTED','DISMISSED')
         OR (investigation_started_at IS NOT NULL
             AND investigation_lead_user_id IS NOT NULL)),
    -- NOTIFIED/CLOSED ⇒ regulator ref + RCA + notified_at
    CONSTRAINT chk_aii_notified_has_evidence CHECK
        (status NOT IN ('NOTIFIED_REGULATOR','CLOSED')
         OR (notified_regulator_at IS NOT NULL
             AND regulator_reference IS NOT NULL
             AND length(trim(regulator_reference)) > 0
             AND root_cause_analysis IS NOT NULL
             AND length(trim(root_cause_analysis)) > 0)),
    -- CLOSED ⇒ corrective actions + closed_at
    CONSTRAINT chk_aii_closed_has_actions CHECK
        (status <> 'CLOSED'
         OR (closed_at IS NOT NULL
             AND corrective_actions IS NOT NULL
             AND length(trim(corrective_actions)) > 0)),
    -- DISMISSED ⇒ reason + dismissed_at
    CONSTRAINT chk_aii_dismissed_has_reason CHECK
        (status <> 'DISMISSED'
         OR (dismissed_at IS NOT NULL
             AND dismissal_reason IS NOT NULL
             AND length(trim(dismissal_reason)) > 0))
);

CREATE INDEX idx_aii_tenant          ON ai_act_incidents (tenant_id);
CREATE INDEX idx_aii_tenant_status   ON ai_act_incidents (tenant_id, status);
CREATE INDEX idx_aii_tenant_severity ON ai_act_incidents (tenant_id, severity);
CREATE INDEX idx_aii_tenant_system   ON ai_act_incidents (tenant_id, ai_system_id);

-- Index partiel : incidents en attente de notification — pour scans O(log n).
CREATE INDEX idx_aii_pending_notification
    ON ai_act_incidents (tenant_id, detected_at)
    WHERE status IN ('DETECTED','INVESTIGATING');

COMMENT ON TABLE  ai_act_incidents IS
    'AI Act (UE 2024/1689 Art. 73) Serious AI incident reports.';
COMMENT ON COLUMN ai_act_incidents.severity IS
    'Severity drives regulator notification deadline: 2/10/15 days from detected_at.';
