-- GDPR — Articles 15/16/17/18/20/21 — Data subject requests
-- OWASP A02 / Privacy by design : la PII (email, identifiant) n'est jamais persistée
-- en clair. Seul un hash SHA-256 déterministe est stocké pour permettre la recherche
-- "toutes les demandes de cette personne" sans détention de la donnée brute.

CREATE TABLE gdpr_subject_requests (
    id                          UUID         NOT NULL,
    tenant_id                   UUID         NOT NULL,
    type                        VARCHAR(32)  NOT NULL,
    subject_identifier_hash     VARCHAR(64)  NOT NULL,
    subject_identifier_label    VARCHAR(250),
    status                      VARCHAR(32)  NOT NULL,
    received_at                 TIMESTAMPTZ  NOT NULL,
    deadline_at                 TIMESTAMPTZ  NOT NULL,
    extended                    BOOLEAN      NOT NULL DEFAULT FALSE,
    in_progress_at              TIMESTAMPTZ,
    completed_at                TIMESTAMPTZ,
    rejection_reason            VARCHAR(2000),
    resolution_notes            VARCHAR(4000),
    evidence_url                VARCHAR(1024),
    requested_by                UUID         NOT NULL,
    handled_by                  UUID,
    updated_at                  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_gdpr_subject_requests PRIMARY KEY (id),
    CONSTRAINT chk_dsr_type     CHECK (type IN ('ACCESS','ERASURE','PORTABILITY',
                                                 'RECTIFICATION','RESTRICTION','OBJECTION')),
    CONSTRAINT chk_dsr_status   CHECK (status IN ('RECEIVED','IN_PROGRESS','COMPLETED','REJECTED')),
    CONSTRAINT chk_dsr_hash_hex CHECK (subject_identifier_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_dsr_deadline_after_received CHECK (deadline_at >= received_at)
);

CREATE INDEX idx_dsr_tenant         ON gdpr_subject_requests (tenant_id);
CREATE INDEX idx_dsr_tenant_subj    ON gdpr_subject_requests (tenant_id, subject_identifier_hash);
CREATE INDEX idx_dsr_tenant_status  ON gdpr_subject_requests (tenant_id, status);
CREATE INDEX idx_dsr_deadline       ON gdpr_subject_requests (deadline_at);

-- Partial index pour le scan des overdue (status non terminaux).
CREATE INDEX idx_dsr_overdue_open
    ON gdpr_subject_requests (deadline_at)
    WHERE status IN ('RECEIVED','IN_PROGRESS');

COMMENT ON TABLE  gdpr_subject_requests IS
    'GDPR data subject requests register (Art. 15/16/17/18/20/21). PII never stored in clear.';
COMMENT ON COLUMN gdpr_subject_requests.subject_identifier_hash IS
    'SHA-256 hex of normalized (trim+lower) identifier — privacy by design.';
COMMENT ON COLUMN gdpr_subject_requests.subject_identifier_label IS
    'Optional masked label for display (e.g., "j***@gmail.com"). Never contains full PII.';
COMMENT ON COLUMN gdpr_subject_requests.deadline_at IS
    'GDPR Art. 12.3 — must be answered within 1 month, extendable once up to +2 months.';
