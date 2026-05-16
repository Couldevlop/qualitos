-- GDPR Art. 33/34 — Personal data breach incidents.
-- Art. 33§1 : notification autorité de contrôle sous 72h.
-- Art. 34   : notification aux personnes concernées si risque élevé.

CREATE TABLE gdpr_breach_incidents (
    id                              UUID         NOT NULL,
    tenant_id                       UUID         NOT NULL,
    internal_reference              VARCHAR(64)  NOT NULL,
    title                           VARCHAR(250) NOT NULL,
    description                     VARCHAR(4000),
    detected_at                     TIMESTAMPTZ  NOT NULL,
    occurred_at                     TIMESTAMPTZ,
    dpa_deadline_at                 TIMESTAMPTZ  NOT NULL,
    severity                        VARCHAR(32)  NOT NULL,
    status                          VARCHAR(32)  NOT NULL,
    affected_subjects_count         BIGINT       NOT NULL DEFAULT 0,
    affected_data_categories        VARCHAR(2000),
    risk_of_harm_description        VARCHAR(2000),
    containment_measures            VARCHAR(4000),
    dpa_notified_at                 TIMESTAMPTZ,
    dpa_reference                   VARCHAR(250),
    subjects_notified_at            TIMESTAMPTZ,
    subjects_notification_channel   VARCHAR(250),
    rejection_reason                VARCHAR(2000),
    closure_notes                   VARCHAR(4000),
    reported_by                     UUID         NOT NULL,
    handled_by                      UUID,
    closed_at                       TIMESTAMPTZ,
    updated_at                      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_gdpr_breach_incidents PRIMARY KEY (id),
    CONSTRAINT uq_breach_tenant_reference
        UNIQUE (tenant_id, internal_reference),
    CONSTRAINT chk_breach_status CHECK
        (status IN ('DETECTED','ASSESSING','CONTAINED','CLOSED','REJECTED')),
    CONSTRAINT chk_breach_severity CHECK
        (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT chk_breach_reference CHECK
        (internal_reference ~ '^[A-Z][A-Z0-9_-]{1,63}$'),
    CONSTRAINT chk_breach_count CHECK (affected_subjects_count >= 0),
    CONSTRAINT chk_breach_deadline_after_detected
        CHECK (dpa_deadline_at >= detected_at),
    CONSTRAINT chk_breach_occurred_before_detected
        CHECK (occurred_at IS NULL OR occurred_at <= detected_at),
    CONSTRAINT chk_breach_dpa_notif_after_detected
        CHECK (dpa_notified_at IS NULL OR dpa_notified_at >= detected_at),
    CONSTRAINT chk_breach_subj_notif_after_detected
        CHECK (subjects_notified_at IS NULL OR subjects_notified_at >= detected_at),
    CONSTRAINT chk_breach_closed_consistent
        CHECK ((status IN ('CLOSED','REJECTED') AND closed_at IS NOT NULL)
            OR (status NOT IN ('CLOSED','REJECTED'))),
    CONSTRAINT chk_breach_rejected_has_reason
        CHECK (status <> 'REJECTED' OR rejection_reason IS NOT NULL)
);

CREATE INDEX idx_breach_tenant         ON gdpr_breach_incidents (tenant_id);
CREATE INDEX idx_breach_tenant_status  ON gdpr_breach_incidents (tenant_id, status);
CREATE INDEX idx_breach_dpa_deadline   ON gdpr_breach_incidents (dpa_deadline_at);

-- Index partiel pour le scan DPA-overdue en O(log n).
CREATE INDEX idx_breach_dpa_overdue
    ON gdpr_breach_incidents (dpa_deadline_at)
    WHERE dpa_notified_at IS NULL AND status NOT IN ('CLOSED','REJECTED');

COMMENT ON TABLE  gdpr_breach_incidents IS
    'GDPR Art. 33/34 personal-data breach incidents. 72h DPA notification deadline.';
COMMENT ON COLUMN gdpr_breach_incidents.dpa_deadline_at IS
    'detectedAt + 72h (Art. 33§1) — internal reminder, not legally binding.';
COMMENT ON COLUMN gdpr_breach_incidents.affected_data_categories IS
    'CSV of category codes (regex-validated by domain — no free-form PII here).';
