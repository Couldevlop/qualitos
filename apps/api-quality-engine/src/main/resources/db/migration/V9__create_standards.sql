-- Standards Hub schema (CLAUDE.md §8).
-- Le catalogue (standards/sections/clauses/requirements) est platform-level
-- (pas de tenant_id). Les adoptions et evidences sont multi-tenant.

CREATE TABLE standards (
    id                            UUID         NOT NULL DEFAULT gen_random_uuid(),
    code                          VARCHAR(100) NOT NULL,
    full_name                     VARCHAR(500) NOT NULL,
    publisher                     VARCHAR(100),
    current_version               VARCHAR(30)  NOT NULL,
    publication_date              DATE,
    family                        VARCHAR(50),
    applicable_industries         TEXT,
    description                   TEXT,
    certification_body_required   BOOLEAN      NOT NULL DEFAULT FALSE,
    recertification_cycle_months  INTEGER,
    related_norm_codes            TEXT,
    status                        VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED',
    created_at                    TIMESTAMPTZ  NOT NULL,
    updated_at                    TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_standards PRIMARY KEY (id),
    CONSTRAINT uk_standards_code UNIQUE (code),
    CONSTRAINT chk_standards_status CHECK (status IN ('PUBLISHED','DEPRECATED','WITHDRAWN'))
);

CREATE INDEX idx_standards_family ON standards (family);
CREATE INDEX idx_standards_status ON standards (status);

CREATE TABLE standard_sections (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    standard_id UUID         NOT NULL,
    code        VARCHAR(20)  NOT NULL,
    title       VARCHAR(500) NOT NULL,
    description TEXT,
    order_index INTEGER      NOT NULL,

    CONSTRAINT pk_standard_sections PRIMARY KEY (id),
    CONSTRAINT fk_standard_sections_std FOREIGN KEY (standard_id)
        REFERENCES standards (id) ON DELETE CASCADE,
    CONSTRAINT uk_section_standard_code UNIQUE (standard_id, code)
);

CREATE INDEX idx_standard_sections_std ON standard_sections (standard_id);

CREATE TABLE standard_clauses (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    section_id  UUID         NOT NULL,
    code        VARCHAR(30)  NOT NULL,
    title       VARCHAR(500) NOT NULL,
    description TEXT,
    order_index INTEGER      NOT NULL,

    CONSTRAINT pk_standard_clauses PRIMARY KEY (id),
    CONSTRAINT fk_standard_clauses_section FOREIGN KEY (section_id)
        REFERENCES standard_sections (id) ON DELETE CASCADE,
    CONSTRAINT uk_clause_section_code UNIQUE (section_id, code)
);

CREATE INDEX idx_standard_clauses_section ON standard_clauses (section_id);

CREATE TABLE standard_requirements (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    clause_id           UUID         NOT NULL,
    code                VARCHAR(30)  NOT NULL,
    text                TEXT         NOT NULL,
    obligation          VARCHAR(10)  NOT NULL,
    evidence_types      TEXT,
    measurable_criteria TEXT,
    risk_if_missing     VARCHAR(20),
    order_index         INTEGER      NOT NULL,

    CONSTRAINT pk_standard_requirements PRIMARY KEY (id),
    CONSTRAINT fk_standard_requirements_clause FOREIGN KEY (clause_id)
        REFERENCES standard_clauses (id) ON DELETE CASCADE,
    CONSTRAINT uk_requirement_clause_code UNIQUE (clause_id, code),
    CONSTRAINT chk_requirement_obligation CHECK (obligation IN ('MUST','SHOULD','MAY')),
    CONSTRAINT chk_requirement_risk CHECK (
        risk_if_missing IS NULL OR risk_if_missing IN ('LOW','MEDIUM','HIGH','CRITICAL')
    )
);

CREATE INDEX idx_standard_requirements_clause ON standard_requirements (clause_id);

CREATE TABLE tenant_standards (
    id                         UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id                  UUID         NOT NULL,
    standard_id                UUID         NOT NULL,
    status                     VARCHAR(20)  NOT NULL DEFAULT 'PLANNING',
    scope_description          TEXT,
    target_certification_date  DATE,
    lead_auditor_id            UUID,
    certification_body         VARCHAR(255),
    certified_at               TIMESTAMPTZ,
    expires_at                 TIMESTAMPTZ,
    created_at                 TIMESTAMPTZ  NOT NULL,
    updated_at                 TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_tenant_standards PRIMARY KEY (id),
    CONSTRAINT fk_tenant_standards_std FOREIGN KEY (standard_id)
        REFERENCES standards (id) ON DELETE RESTRICT,
    CONSTRAINT uk_tenant_standard UNIQUE (tenant_id, standard_id),
    CONSTRAINT chk_tenant_standards_status CHECK (
        status IN ('PLANNING','IN_PROGRESS','CERTIFIED','SURVEILLANCE','EXPIRED','WITHDRAWN')
    )
);

CREATE INDEX idx_tenant_standards_tenant ON tenant_standards (tenant_id);
CREATE INDEX idx_tenant_standards_tenant_status ON tenant_standards (tenant_id, status);

CREATE TABLE requirement_evidences (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id           UUID         NOT NULL,
    tenant_standard_id  UUID         NOT NULL,
    requirement_id      UUID         NOT NULL,
    evidence_type       VARCHAR(30)  NOT NULL,
    evidence_ref_id     UUID,
    evidence_uri        VARCHAR(1024),
    note                TEXT,
    linked_by           UUID         NOT NULL,
    linked_at           TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_requirement_evidences PRIMARY KEY (id),
    CONSTRAINT fk_requirement_evidences_ts FOREIGN KEY (tenant_standard_id)
        REFERENCES tenant_standards (id) ON DELETE CASCADE,
    CONSTRAINT fk_requirement_evidences_req FOREIGN KEY (requirement_id)
        REFERENCES standard_requirements (id) ON DELETE CASCADE,
    CONSTRAINT uk_evidence_ts_req_ref UNIQUE (tenant_standard_id, requirement_id, evidence_ref_id),
    CONSTRAINT chk_evidence_type CHECK (
        evidence_type IN ('DOCUMENT','AUDIT','CAPA','PDCA_CYCLE','ISHIKAWA','FIVES_AUDIT',
                          'TRAINING_RECORD','KPI_RECORD','EXTERNAL_FILE','OTHER')
    )
);

CREATE INDEX idx_requirement_evidences_ts ON requirement_evidences (tenant_standard_id);
CREATE INDEX idx_requirement_evidences_req ON requirement_evidences (requirement_id);
CREATE INDEX idx_requirement_evidences_tenant ON requirement_evidences (tenant_id);
