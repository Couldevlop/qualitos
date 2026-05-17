-- AI Act (UE 2024/1689) — Conformity Assessments (Art. 43).
-- Registre des évaluations de conformité par systèmes HIGH-risk.

CREATE TABLE ai_act_conformity_assessments (
    id                              UUID         NOT NULL,
    tenant_id                       UUID         NOT NULL,
    reference                       VARCHAR(64)  NOT NULL,
    ai_system_id                    UUID         NOT NULL,
    qms_id                          UUID,
    procedure                       VARCHAR(32)  NOT NULL,
    notified_body_id                VARCHAR(8),
    notified_body_name              VARCHAR(250),
    scope                           VARCHAR(4000) NOT NULL,
    status                          VARCHAR(32)  NOT NULL,
    planned_at                      TIMESTAMPTZ,
    started_at                      TIMESTAMPTZ,
    certified_at                    TIMESTAMPTZ,
    certificate_number              VARCHAR(250),
    valid_until                     TIMESTAMPTZ,
    eu_declaration_reference        VARCHAR(250),
    expired_at                      TIMESTAMPTZ,
    revoked_at                      TIMESTAMPTZ,
    revocation_reason               VARCHAR(2000),
    failed_at                       TIMESTAMPTZ,
    failure_reason                  VARCHAR(2000),
    created_by                      UUID         NOT NULL,
    created_at                      TIMESTAMPTZ  NOT NULL,
    updated_at                      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_ai_act_conformity_assessments PRIMARY KEY (id),
    CONSTRAINT uq_aica_tenant_reference UNIQUE (tenant_id, reference),
    CONSTRAINT chk_aica_status CHECK
        (status IN ('PLANNED','IN_PROGRESS','CERTIFIED','EXPIRED','REVOKED','FAILED')),
    CONSTRAINT chk_aica_procedure CHECK
        (procedure IN ('INTERNAL_CONTROL','NOTIFIED_BODY')),
    CONSTRAINT chk_aica_reference CHECK
        (reference ~ '^[A-Z][A-Z0-9_-]{1,63}$'),
    CONSTRAINT chk_aica_notified_body_id CHECK
        (notified_body_id IS NULL OR notified_body_id ~ '^[0-9]{4}$'),
    -- NOTIFIED_BODY ⇒ notifiedBodyId + notifiedBodyName
    CONSTRAINT chk_aica_notified_body_required CHECK
        (procedure <> 'NOTIFIED_BODY'
         OR (notified_body_id IS NOT NULL AND notified_body_name IS NOT NULL
             AND length(trim(notified_body_name)) > 0)),
    -- IN_PROGRESS et au-delà ⇒ startedAt
    CONSTRAINT chk_aica_started_has_timestamp CHECK
        (status IN ('PLANNED','FAILED')
         OR started_at IS NOT NULL),
    -- CERTIFIED/EXPIRED/REVOKED (depuis CERTIFIED) ⇒ certificate + validUntil + EU decl
    CONSTRAINT chk_aica_certified_has_certificate CHECK
        (status NOT IN ('CERTIFIED','EXPIRED')
         OR (certified_at IS NOT NULL
             AND certificate_number IS NOT NULL
             AND length(trim(certificate_number)) > 0
             AND valid_until IS NOT NULL
             AND valid_until > certified_at
             AND eu_declaration_reference IS NOT NULL
             AND length(trim(eu_declaration_reference)) > 0)),
    -- EXPIRED ⇒ expired_at ≥ valid_until
    CONSTRAINT chk_aica_expired_after_valid_until CHECK
        (status <> 'EXPIRED'
         OR (expired_at IS NOT NULL AND valid_until IS NOT NULL
             AND expired_at >= valid_until)),
    -- REVOKED ⇒ reason + revoked_at
    CONSTRAINT chk_aica_revoked_has_reason CHECK
        (status <> 'REVOKED'
         OR (revoked_at IS NOT NULL
             AND revocation_reason IS NOT NULL
             AND length(trim(revocation_reason)) > 0)),
    -- FAILED ⇒ reason + failed_at
    CONSTRAINT chk_aica_failed_has_reason CHECK
        (status <> 'FAILED'
         OR (failed_at IS NOT NULL
             AND failure_reason IS NOT NULL
             AND length(trim(failure_reason)) > 0))
);

CREATE INDEX idx_aica_tenant        ON ai_act_conformity_assessments (tenant_id);
CREATE INDEX idx_aica_tenant_status ON ai_act_conformity_assessments (tenant_id, status);
CREATE INDEX idx_aica_tenant_system ON ai_act_conformity_assessments (tenant_id, ai_system_id);

-- Index partiel : certifications actives — scan rapide des échéances.
CREATE INDEX idx_aica_certified_valid_until
    ON ai_act_conformity_assessments (tenant_id, valid_until)
    WHERE status = 'CERTIFIED';

COMMENT ON TABLE  ai_act_conformity_assessments IS
    'AI Act (UE 2024/1689 Art. 43) Conformity assessments for HIGH-risk AI systems.';
COMMENT ON COLUMN ai_act_conformity_assessments.notified_body_id IS
    '4-digit identifier assigned by the EU Commission to the notified body.';
COMMENT ON COLUMN ai_act_conformity_assessments.eu_declaration_reference IS
    'Reference of the EU Declaration of Conformity (Annex V).';
