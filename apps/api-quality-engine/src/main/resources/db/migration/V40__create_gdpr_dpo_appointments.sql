-- GDPR Art. 37-39 — DPO appointments registry.
-- Registre des désignations de Délégué à la Protection des Données.

CREATE TABLE gdpr_dpo_appointments (
    id                                  UUID         NOT NULL,
    tenant_id                           UUID         NOT NULL,
    reference                           VARCHAR(64)  NOT NULL,
    dpo_full_name                       VARCHAR(250) NOT NULL,
    dpo_email                           VARCHAR(320) NOT NULL,
    dpo_phone                           VARCHAR(64),
    dpo_type                            VARCHAR(32)  NOT NULL,
    external_company_name               VARCHAR(250),
    qualifications                      VARCHAR(4000),
    scope                               VARCHAR(64)  NOT NULL,
    effective_from                      TIMESTAMPTZ,
    effective_to                        TIMESTAMPTZ,
    regulator_notified_at               TIMESTAMPTZ,
    regulator_notification_reference    VARCHAR(250),
    linked_processing_activity_ids      VARCHAR(4000),
    status                              VARCHAR(32)  NOT NULL,
    end_reason                          VARCHAR(2000),
    created_by                          UUID         NOT NULL,
    created_at                          TIMESTAMPTZ  NOT NULL,
    updated_at                          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_gdpr_dpo_appointments PRIMARY KEY (id),
    CONSTRAINT uq_dpo_tenant_reference UNIQUE (tenant_id, reference),
    CONSTRAINT chk_dpo_status CHECK
        (status IN ('PROPOSED','ACTIVE','ENDED','CANCELLED')),
    CONSTRAINT chk_dpo_type CHECK
        (dpo_type IN ('INTERNAL','EXTERNAL')),
    CONSTRAINT chk_dpo_reference CHECK
        (reference ~ '^[A-Z][A-Z0-9_-]{1,63}$'),
    CONSTRAINT chk_dpo_scope CHECK
        (scope ~ '^[A-Z][A-Z0-9_-]{0,63}$'),
    CONSTRAINT chk_dpo_email CHECK
        (dpo_email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    -- EXTERNAL ⇒ externalCompanyName non vide (Art. 37.6)
    CONSTRAINT chk_dpo_external_has_company CHECK
        (dpo_type <> 'EXTERNAL'
         OR (external_company_name IS NOT NULL
             AND length(trim(external_company_name)) > 0)),
    -- ACTIVE ⇒ effective_from + regulator_notified_at + reference renseignés
    CONSTRAINT chk_dpo_active_has_metadata CHECK
        (status <> 'ACTIVE'
         OR (effective_from IS NOT NULL
             AND regulator_notified_at IS NOT NULL
             AND regulator_notification_reference IS NOT NULL
             AND length(trim(regulator_notification_reference)) > 0)),
    -- ENDED ⇒ effective_to + reason
    CONSTRAINT chk_dpo_ended_has_to_and_reason CHECK
        (status <> 'ENDED'
         OR (effective_to IS NOT NULL
             AND end_reason IS NOT NULL
             AND length(trim(end_reason)) > 0)),
    -- CANCELLED ⇒ reason
    CONSTRAINT chk_dpo_cancelled_has_reason CHECK
        (status <> 'CANCELLED'
         OR (end_reason IS NOT NULL AND length(trim(end_reason)) > 0)),
    CONSTRAINT chk_dpo_to_after_from CHECK
        (effective_to IS NULL OR effective_from IS NULL
         OR effective_to >= effective_from)
);

CREATE INDEX idx_dpo_tenant        ON gdpr_dpo_appointments (tenant_id);
CREATE INDEX idx_dpo_tenant_status ON gdpr_dpo_appointments (tenant_id, status);
CREATE INDEX idx_dpo_tenant_scope  ON gdpr_dpo_appointments (tenant_id, scope);

-- Invariant fort : au plus une ACTIVE par (tenant, scope).
CREATE UNIQUE INDEX uq_dpo_active_per_scope
    ON gdpr_dpo_appointments (tenant_id, scope)
    WHERE status = 'ACTIVE';

COMMENT ON TABLE  gdpr_dpo_appointments IS
    'GDPR Art. 37-39 DPO appointments register. At most one ACTIVE per (tenant, scope).';
COMMENT ON COLUMN gdpr_dpo_appointments.regulator_notified_at IS
    'Required for ACTIVE — Art. 37.7 — notification to supervisory authority.';
COMMENT ON COLUMN gdpr_dpo_appointments.scope IS
    'Functional scope (e.g., GROUP, EU-OPS) — invariant 1-ACTIVE-per-scope.';
