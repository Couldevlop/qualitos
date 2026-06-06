CREATE TABLE non_conformities (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id       UUID         NOT NULL,
    reference       VARCHAR(40)  NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    category        VARCHAR(30)  NOT NULL,
    severity        VARCHAR(20)  NOT NULL,
    status          VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    detected_at     TIMESTAMPTZ  NOT NULL,
    zone            VARCHAR(255),
    geo_lat         DOUBLE PRECISION,
    geo_lng         DOUBLE PRECISION,
    photo_urls      TEXT,
    reporter_id     UUID,
    capa_case_id    UUID,
    root_cause      TEXT,
    resolution_note TEXT,
    resolved_at     TIMESTAMPTZ,
    closed_at       TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_non_conformities PRIMARY KEY (id),
    CONSTRAINT uq_non_conformities_reference UNIQUE (tenant_id, reference),
    CONSTRAINT fk_non_conformities_capa FOREIGN KEY (capa_case_id)
        REFERENCES capa_cases (id) ON DELETE SET NULL,
    CONSTRAINT chk_non_conformities_category CHECK (
        category IN ('PRODUCT', 'PROCESS', 'DOCUMENTATION', 'SUPPLIER', 'SAFETY', 'ENVIRONMENT', 'OTHER')
    ),
    CONSTRAINT chk_non_conformities_severity CHECK (
        severity IN ('MINOR', 'MAJOR', 'CRITICAL')
    ),
    CONSTRAINT chk_non_conformities_status CHECK (
        status IN ('OPEN', 'UNDER_ANALYSIS', 'ACTION_DEFINED', 'RESOLVED', 'CLOSED', 'CANCELLED')
    )
);

CREATE INDEX idx_non_conformities_tenant_status ON non_conformities (tenant_id, status);
CREATE INDEX idx_non_conformities_tenant_severity ON non_conformities (tenant_id, severity);
CREATE INDEX idx_non_conformities_tenant_detected_at ON non_conformities (tenant_id, detected_at DESC);
