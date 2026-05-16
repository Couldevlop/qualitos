-- GDPR Art. 7 — Consent records.
-- OWASP A02 / Privacy by design : PII (email, identifier) jamais persistée en clair.
-- Le retrait est terminal et irréversible — un nouveau consentement crée une
-- nouvelle ligne pour préserver la traçabilité historique (charge de la preuve).

CREATE TABLE gdpr_consents (
    id                          UUID         NOT NULL,
    tenant_id                   UUID         NOT NULL,
    subject_identifier_hash     VARCHAR(64)  NOT NULL,
    subject_identifier_label    VARCHAR(250),
    purpose_code                VARCHAR(64)  NOT NULL,
    purpose_version             VARCHAR(32)  NOT NULL,
    source                      VARCHAR(32)  NOT NULL,
    evidence_url                VARCHAR(1024),
    ip_address                  VARCHAR(64),
    user_agent                  VARCHAR(500),
    granted_by                  UUID,
    granted_at                  TIMESTAMPTZ  NOT NULL,
    expires_at                  TIMESTAMPTZ,
    status                      VARCHAR(32)  NOT NULL,
    withdrawn_at                TIMESTAMPTZ,
    withdrawn_by                UUID,
    withdrawal_reason           VARCHAR(2000),
    updated_at                  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_gdpr_consents PRIMARY KEY (id),
    CONSTRAINT chk_consent_status CHECK (status IN ('GRANTED','WITHDRAWN','EXPIRED')),
    CONSTRAINT chk_consent_source CHECK (source IN
        ('WEB_FORM','MOBILE_APP','EMAIL','PAPER','PHONE','API','IMPORT','OTHER')),
    CONSTRAINT chk_consent_hash_hex CHECK (subject_identifier_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_consent_purpose_code CHECK (purpose_code ~ '^[a-z][a-z0-9._-]{1,63}$'),
    CONSTRAINT chk_consent_purpose_version CHECK (purpose_version ~ '^[A-Za-z0-9._:-]{1,32}$'),
    CONSTRAINT chk_consent_expires_after_granted
        CHECK (expires_at IS NULL OR expires_at > granted_at),
    CONSTRAINT chk_consent_withdrawn_after_granted
        CHECK (withdrawn_at IS NULL OR withdrawn_at >= granted_at),
    CONSTRAINT chk_consent_withdrawn_when_withdrawn
        CHECK ((status = 'WITHDRAWN' AND withdrawn_at IS NOT NULL)
            OR (status <> 'WITHDRAWN'))
);

CREATE INDEX idx_consent_tenant              ON gdpr_consents (tenant_id);
CREATE INDEX idx_consent_tenant_subj_purpose ON gdpr_consents
    (tenant_id, subject_identifier_hash, purpose_code);
CREATE INDEX idx_consent_tenant_purpose      ON gdpr_consents (tenant_id, purpose_code);
CREATE INDEX idx_consent_expires             ON gdpr_consents (expires_at);

-- Index partiel pour scan d'expiration efficace (uniquement les GRANTED avec expiresAt).
CREATE INDEX idx_consent_expirable
    ON gdpr_consents (expires_at)
    WHERE status = 'GRANTED' AND expires_at IS NOT NULL;

COMMENT ON TABLE  gdpr_consents IS
    'GDPR Art. 7 consent records. PII never stored in clear. Withdrawal is irreversible.';
COMMENT ON COLUMN gdpr_consents.subject_identifier_hash IS
    'SHA-256 hex of normalized (trim+lower) identifier — privacy by design.';
COMMENT ON COLUMN gdpr_consents.purpose_version IS
    'Version of the consent text shown to the subject — required for proof.';
COMMENT ON COLUMN gdpr_consents.ip_address IS
    'Optional — collected only when lawful basis exists (proof of consent).';
