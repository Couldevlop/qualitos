-- NIS2 Art. 23 — Cybersecurity incidents register.
-- Trois délais légaux : 24h alerte préliminaire, 72h évaluation initiale,
-- 30 jours rapport final.

CREATE TABLE nis2_cyber_incidents (
    id                                  UUID         NOT NULL,
    tenant_id                           UUID         NOT NULL,
    reference                           VARCHAR(64)  NOT NULL,
    title                               VARCHAR(250) NOT NULL,
    description                         VARCHAR(4000),
    detected_at                         TIMESTAMPTZ  NOT NULL,
    occurred_at                         TIMESTAMPTZ,
    early_warning_deadline_at           TIMESTAMPTZ  NOT NULL,
    initial_assessment_deadline_at      TIMESTAMPTZ  NOT NULL,
    final_report_deadline_at            TIMESTAMPTZ  NOT NULL,
    incident_type                       VARCHAR(32)  NOT NULL,
    severity                            VARCHAR(32)  NOT NULL,
    status                              VARCHAR(32)  NOT NULL,
    estimated_affected_users            BIGINT       NOT NULL DEFAULT 0,
    affected_assets                     VARCHAR(2000),
    affected_services                   VARCHAR(2000),
    linked_breach_id                    UUID,
    containment_measures                VARCHAR(4000),
    impact_description                  VARCHAR(4000),
    early_warning_sent_at               TIMESTAMPTZ,
    early_warning_reference             VARCHAR(250),
    initial_assessment_sent_at          TIMESTAMPTZ,
    initial_assessment_reference        VARCHAR(250),
    final_report_sent_at                TIMESTAMPTZ,
    final_report_reference              VARCHAR(250),
    closure_notes                       VARCHAR(4000),
    rejection_reason                    VARCHAR(2000),
    reported_by                         UUID         NOT NULL,
    handled_by                          UUID,
    closed_at                           TIMESTAMPTZ,
    updated_at                          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_nis2_cyber_incidents PRIMARY KEY (id),
    CONSTRAINT uq_cyb_tenant_reference UNIQUE (tenant_id, reference),
    CONSTRAINT chk_cyb_status CHECK
        (status IN ('DETECTED','ASSESSING','MITIGATED','CLOSED','REJECTED')),
    CONSTRAINT chk_cyb_severity CHECK
        (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT chk_cyb_type CHECK
        (incident_type IN ('RANSOMWARE','DATA_BREACH','DDOS','INSIDER_THREAT',
                           'MALWARE','PHISHING','SYSTEM_OUTAGE','SUPPLY_CHAIN',
                           'UNAUTHORIZED_ACCESS','OTHER')),
    CONSTRAINT chk_cyb_reference CHECK
        (reference ~ '^[A-Z][A-Z0-9_-]{1,63}$'),
    CONSTRAINT chk_cyb_count CHECK (estimated_affected_users >= 0),
    CONSTRAINT chk_cyb_occurred_before_detected CHECK
        (occurred_at IS NULL OR occurred_at <= detected_at),
    -- Deadlines doivent venir après detectedAt
    CONSTRAINT chk_cyb_deadlines_after_detected CHECK
        (early_warning_deadline_at >= detected_at
         AND initial_assessment_deadline_at >= detected_at
         AND final_report_deadline_at >= detected_at),
    -- Notifications doivent être ≥ detected_at
    CONSTRAINT chk_cyb_early_after_detected CHECK
        (early_warning_sent_at IS NULL OR early_warning_sent_at >= detected_at),
    CONSTRAINT chk_cyb_initial_after_detected CHECK
        (initial_assessment_sent_at IS NULL OR initial_assessment_sent_at >= detected_at),
    CONSTRAINT chk_cyb_final_after_detected CHECK
        (final_report_sent_at IS NULL OR final_report_sent_at >= detected_at),
    -- Cohérence terminale
    CONSTRAINT chk_cyb_closed_consistent CHECK
        ((status IN ('CLOSED','REJECTED') AND closed_at IS NOT NULL)
         OR status NOT IN ('CLOSED','REJECTED')),
    CONSTRAINT chk_cyb_rejected_has_reason CHECK
        (status <> 'REJECTED' OR rejection_reason IS NOT NULL)
);

CREATE INDEX idx_cyb_tenant                       ON nis2_cyber_incidents (tenant_id);
CREATE INDEX idx_cyb_tenant_status                ON nis2_cyber_incidents (tenant_id, status);
CREATE INDEX idx_cyb_early_warning_deadline       ON nis2_cyber_incidents (early_warning_deadline_at);
CREATE INDEX idx_cyb_initial_assessment_deadline  ON nis2_cyber_incidents (initial_assessment_deadline_at);
CREATE INDEX idx_cyb_final_report_deadline        ON nis2_cyber_incidents (final_report_deadline_at);

-- Index partiels pour les pipelines "X overdue" en O(log n).
CREATE INDEX idx_cyb_early_warning_overdue
    ON nis2_cyber_incidents (early_warning_deadline_at)
    WHERE early_warning_sent_at IS NULL AND status NOT IN ('CLOSED','REJECTED');

CREATE INDEX idx_cyb_initial_assessment_overdue
    ON nis2_cyber_incidents (initial_assessment_deadline_at)
    WHERE initial_assessment_sent_at IS NULL AND status NOT IN ('CLOSED','REJECTED');

CREATE INDEX idx_cyb_final_report_overdue
    ON nis2_cyber_incidents (final_report_deadline_at)
    WHERE final_report_sent_at IS NULL AND status NOT IN ('CLOSED','REJECTED');

COMMENT ON TABLE  nis2_cyber_incidents IS
    'NIS2 Art. 23 cybersecurity incidents register. 24h/72h/30d deadlines.';
COMMENT ON COLUMN nis2_cyber_incidents.linked_breach_id IS
    'Optional link to gdpr_breach_incidents.id when PII is involved.';
