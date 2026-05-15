-- V20: Supplier Quality Management (CLAUDE.md §4.6)

CREATE TABLE suppliers (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    code            VARCHAR(120) NOT NULL,
    name            VARCHAR(250) NOT NULL,
    country_code    CHAR(2),
    contact_email   VARCHAR(320),
    supplier_type   VARCHAR(32) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    score           INT NOT NULL DEFAULT 100,
    last_audit_at   DATE,
    approved_at     TIMESTAMP WITH TIME ZONE,
    approved_by     UUID,
    created_by      UUID NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_supplier_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT chk_supplier_status CHECK (status IN
        ('PROSPECT','APPROVED','CONDITIONAL','SUSPENDED','BLACKLISTED')),
    CONSTRAINT chk_supplier_type CHECK (supplier_type IN
        ('RAW_MATERIAL','COMPONENT','SERVICE','CONTRACT_MANUFACTURER',
         'SOFTWARE','LOGISTICS','OTHER')),
    CONSTRAINT chk_supplier_score CHECK (score BETWEEN 0 AND 100),
    CONSTRAINT chk_supplier_country CHECK (country_code IS NULL OR country_code ~ '^[A-Z]{2}$')
);

CREATE INDEX idx_supplier_tenant        ON suppliers(tenant_id);
CREATE INDEX idx_supplier_tenant_status ON suppliers(tenant_id, status);
CREATE INDEX idx_supplier_tenant_score  ON suppliers(tenant_id, score);

CREATE TABLE supplier_audit_records (
    id                       UUID PRIMARY KEY,
    tenant_id                UUID NOT NULL,
    supplier_id              UUID NOT NULL,
    audited_on               DATE NOT NULL,
    score                    INT NOT NULL,
    auditor_user_id          UUID,
    findings_summary         VARCHAR(2000),
    critical_findings_count  INT NOT NULL DEFAULT 0,
    major_findings_count     INT NOT NULL DEFAULT 0,
    minor_findings_count     INT NOT NULL DEFAULT 0,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_supplier_audit_supplier FOREIGN KEY (supplier_id)
        REFERENCES suppliers(id) ON DELETE CASCADE,
    CONSTRAINT chk_supplier_audit_score CHECK (score BETWEEN 0 AND 100),
    CONSTRAINT chk_supplier_audit_critical CHECK (critical_findings_count >= 0),
    CONSTRAINT chk_supplier_audit_major    CHECK (major_findings_count    >= 0),
    CONSTRAINT chk_supplier_audit_minor    CHECK (minor_findings_count    >= 0)
);

CREATE INDEX idx_supplier_audit_supplier ON supplier_audit_records(supplier_id, audited_on);
CREATE INDEX idx_supplier_audit_tenant   ON supplier_audit_records(tenant_id, audited_on);

CREATE TABLE supplier_non_conformities (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL,
    supplier_id   UUID NOT NULL,
    lot_reference VARCHAR(100),
    description   VARCHAR(1000),
    severity      VARCHAR(32) NOT NULL,
    status        VARCHAR(32) NOT NULL,
    detected_on   DATE NOT NULL,
    resolved_on   DATE,
    resolution    VARCHAR(1000),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_supplier_nc_supplier FOREIGN KEY (supplier_id)
        REFERENCES suppliers(id) ON DELETE CASCADE,
    CONSTRAINT chk_supplier_nc_severity CHECK (severity IN ('MINOR','MAJOR','CRITICAL')),
    CONSTRAINT chk_supplier_nc_status   CHECK (status IN ('OPEN','IN_REVIEW','RESOLVED','REJECTED'))
);

CREATE INDEX idx_supplier_nc_supplier      ON supplier_non_conformities(supplier_id, detected_on);
CREATE INDEX idx_supplier_nc_tenant_status ON supplier_non_conformities(tenant_id, status);

CREATE TABLE supplier_certificates (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL,
    supplier_id   UUID NOT NULL,
    standard_code VARCHAR(64) NOT NULL,
    reference     VARCHAR(200),
    issued_on     DATE NOT NULL,
    expires_on    DATE NOT NULL,
    document_url  VARCHAR(1024),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_supplier_cert_supplier FOREIGN KEY (supplier_id)
        REFERENCES suppliers(id) ON DELETE CASCADE,
    CONSTRAINT chk_supplier_cert_dates CHECK (expires_on > issued_on),
    CONSTRAINT chk_supplier_cert_code CHECK (standard_code ~ '^[a-z0-9][a-z0-9_-]{1,62}$')
);

CREATE INDEX idx_supplier_cert_supplier ON supplier_certificates(supplier_id, expires_on);
CREATE INDEX idx_supplier_cert_tenant   ON supplier_certificates(tenant_id, expires_on);

COMMENT ON TABLE suppliers IS 'Référentiel fournisseurs avec score recalculé par SupplierScoringService.';
COMMENT ON COLUMN suppliers.score IS 'Score 0..100 — ne JAMAIS écrire directement (formule §4.6).';
