-- DMAIC + Poka-Yoke (CLAUDE.md §3.4).
-- Catalogue Poka-Yoke partage entre tenants (pas de tenant_id).
-- Projets DMAIC, mesures, assignments : tenant-scoped.

CREATE TABLE dmaic_projects (
    id                       UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id                UUID         NOT NULL,
    title                    VARCHAR(255) NOT NULL,
    problem_statement        TEXT,
    goal_statement           TEXT,
    phase                    VARCHAR(20)  NOT NULL DEFAULT 'DEFINE',
    status                   VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    champion_id              UUID,
    black_belt_id            UUID         NOT NULL,
    target_completion_date   DATE,
    spec_lower_limit         DOUBLE PRECISION,
    spec_upper_limit         DOUBLE PRECISION,
    spec_target              DOUBLE PRECISION,
    spec_unit                VARCHAR(50),
    estimated_savings_eur    DOUBLE PRECISION,
    started_at               TIMESTAMPTZ,
    completed_at             TIMESTAMPTZ,
    created_at               TIMESTAMPTZ  NOT NULL,
    updated_at               TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_dmaic_projects PRIMARY KEY (id),
    CONSTRAINT chk_dmaic_phase CHECK (phase IN ('DEFINE','MEASURE','ANALYZE','IMPROVE','CONTROL')),
    CONSTRAINT chk_dmaic_status CHECK (status IN ('ACTIVE','ON_HOLD','COMPLETED','CANCELLED'))
);

CREATE INDEX idx_dmaic_projects_tenant ON dmaic_projects (tenant_id);
CREATE INDEX idx_dmaic_projects_tenant_status ON dmaic_projects (tenant_id, status);
CREATE INDEX idx_dmaic_projects_tenant_phase ON dmaic_projects (tenant_id, phase);

CREATE TABLE dmaic_process_measures (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    project_id   UUID         NOT NULL,
    value        DOUBLE PRECISION NOT NULL,
    subgroup_id  VARCHAR(100),
    source_ref   VARCHAR(255),
    recorded_at  TIMESTAMPTZ  NOT NULL,
    operator_id  UUID,
    note         TEXT,
    created_at   TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_dmaic_measures PRIMARY KEY (id),
    CONSTRAINT fk_dmaic_measures_project FOREIGN KEY (project_id)
        REFERENCES dmaic_projects (id) ON DELETE CASCADE
);

CREATE INDEX idx_dmaic_measures_project ON dmaic_process_measures (project_id);
CREATE INDEX idx_dmaic_measures_project_time ON dmaic_process_measures (project_id, recorded_at);

CREATE TABLE pokayoke_devices (
    id                     UUID         NOT NULL DEFAULT gen_random_uuid(),
    code                   VARCHAR(100) NOT NULL,
    name                   VARCHAR(255) NOT NULL,
    description            TEXT,
    type                   VARCHAR(20)  NOT NULL,
    mechanism              VARCHAR(30)  NOT NULL,
    applicable_industries  TEXT,
    examples               TEXT,
    implementation_cost    VARCHAR(20),
    created_at             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_pokayoke_devices PRIMARY KEY (id),
    CONSTRAINT uk_pokayoke_devices_code UNIQUE (code),
    CONSTRAINT chk_pokayoke_type CHECK (type IN ('PREVENTION','DETECTION')),
    CONSTRAINT chk_pokayoke_mechanism CHECK (
        mechanism IN ('PHYSICAL_SHAPE','INTERLOCK','LIMIT_SWITCH','SENSOR','VISION',
                      'CHECKLIST','COLOR_CODING','POSITION_REFERENCE','COUNTER',
                      'SOFTWARE_VALIDATION','OTHER')
    )
);

CREATE INDEX idx_pokayoke_devices_type ON pokayoke_devices (type);
CREATE INDEX idx_pokayoke_devices_mechanism ON pokayoke_devices (mechanism);

CREATE TABLE pokayoke_assignments (
    id                    UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id             UUID         NOT NULL,
    project_id            UUID         NOT NULL,
    device_id             UUID         NOT NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'PROPOSED',
    note                  TEXT,
    implemented_at        TIMESTAMPTZ,
    verified_at           TIMESTAMPTZ,
    defect_reduction_pct  DOUBLE PRECISION,
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_pokayoke_assignments PRIMARY KEY (id),
    CONSTRAINT fk_pokayoke_assignments_project FOREIGN KEY (project_id)
        REFERENCES dmaic_projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_pokayoke_assignments_device FOREIGN KEY (device_id)
        REFERENCES pokayoke_devices (id) ON DELETE RESTRICT,
    CONSTRAINT chk_pokayoke_assignment_status CHECK (
        status IN ('PROPOSED','IN_DESIGN','IMPLEMENTED','VERIFIED','ABANDONED')
    )
);

CREATE INDEX idx_pokayoke_assignments_project ON pokayoke_assignments (project_id);
CREATE INDEX idx_pokayoke_assignments_tenant ON pokayoke_assignments (tenant_id);
