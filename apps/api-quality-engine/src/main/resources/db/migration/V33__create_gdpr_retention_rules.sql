-- GDPR Art. 5.1.e — "Limitation de la conservation".
-- Une règle s'applique à une catégorie de données (dataCategoryCode) pour
-- un tenant et définit une durée maximale de conservation.
-- Invariant clé : au plus une règle ACTIVE par (tenant, data_category_code)
-- à un instant donné (index partiel ci-dessous).

CREATE TABLE gdpr_retention_rules (
    id                          UUID         NOT NULL,
    tenant_id                   UUID         NOT NULL,
    data_category_code          VARCHAR(64)  NOT NULL,
    data_category_label         VARCHAR(250),
    retention_period_seconds    BIGINT       NOT NULL,
    legal_basis                 VARCHAR(2000) NOT NULL,
    lawful_basis_reference      VARCHAR(1024),
    status                      VARCHAR(32)  NOT NULL,
    effective_from              TIMESTAMPTZ,
    effective_to                TIMESTAMPTZ,
    created_by                  UUID         NOT NULL,
    created_at                  TIMESTAMPTZ  NOT NULL,
    updated_at                  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_gdpr_retention_rules PRIMARY KEY (id),
    CONSTRAINT chk_retention_status CHECK (status IN ('DRAFT','ACTIVE','ARCHIVED')),
    CONSTRAINT chk_retention_category CHECK
        (data_category_code ~ '^[a-z][a-z0-9._-]{1,63}$'),
    -- 1 jour minimum (86400 s), 100 années maximum (3 153 600 000 s).
    CONSTRAINT chk_retention_period_range CHECK
        (retention_period_seconds BETWEEN 86400 AND 3153600000),
    CONSTRAINT chk_retention_active_has_from CHECK
        ((status <> 'ACTIVE') OR (effective_from IS NOT NULL)),
    CONSTRAINT chk_retention_archived_has_to CHECK
        ((status <> 'ARCHIVED') OR (effective_to IS NOT NULL)),
    CONSTRAINT chk_retention_to_after_from CHECK
        (effective_to IS NULL OR effective_from IS NULL
         OR effective_to >= effective_from)
);

CREATE INDEX idx_retention_tenant          ON gdpr_retention_rules (tenant_id);
CREATE INDEX idx_retention_tenant_category ON gdpr_retention_rules
    (tenant_id, data_category_code);
CREATE INDEX idx_retention_tenant_status   ON gdpr_retention_rules
    (tenant_id, status);

-- Invariant fort : au plus une règle ACTIVE par (tenant, data_category_code).
CREATE UNIQUE INDEX uq_retention_active_per_category
    ON gdpr_retention_rules (tenant_id, data_category_code)
    WHERE status = 'ACTIVE';

COMMENT ON TABLE  gdpr_retention_rules IS
    'GDPR Art. 5.1.e retention rules. At most one ACTIVE per (tenant, category).';
COMMENT ON COLUMN gdpr_retention_rules.retention_period_seconds IS
    'Retention period in seconds (1 day to 100 years).';
COMMENT ON COLUMN gdpr_retention_rules.legal_basis IS
    'Mandatory — GDPR Art. 6 lawful basis (consent, contract, legal obligation, …).';
