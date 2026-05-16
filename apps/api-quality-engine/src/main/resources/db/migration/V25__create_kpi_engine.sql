-- V25: KPI engine (CLAUDE.md §6)

CREATE TABLE kpi_definitions (
    id                         UUID PRIMARY KEY,
    tenant_id                  UUID NOT NULL,
    code                       VARCHAR(100) NOT NULL,
    name                       VARCHAR(250) NOT NULL,
    description                VARCHAR(2000),
    category                   VARCHAR(64),
    unit                       VARCHAR(32),
    direction                  VARCHAR(32) NOT NULL,
    frequency                  VARCHAR(32) NOT NULL,
    target_value               NUMERIC(24, 6),
    threshold_warning          NUMERIC(24, 6),
    threshold_critical         NUMERIC(24, 6),
    status                     VARCHAR(32) NOT NULL,
    applicable_industries_csv  VARCHAR(1000),
    owner_user_id              UUID,
    created_by                 UUID NOT NULL,
    created_at                 TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                 TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_kpi_def_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT chk_kpi_def_direction CHECK (direction IN ('HIGHER_IS_BETTER','LOWER_IS_BETTER')),
    CONSTRAINT chk_kpi_def_frequency CHECK (frequency IN
        ('REALTIME','DAILY','WEEKLY','MONTHLY','QUARTERLY','YEARLY','ON_DEMAND')),
    CONSTRAINT chk_kpi_def_status CHECK (status IN ('DRAFT','ACTIVE','ARCHIVED')),
    CONSTRAINT chk_kpi_def_code CHECK (code ~ '^[a-z0-9][a-z0-9_-]{1,99}$')
);

CREATE INDEX idx_kpi_def_tenant          ON kpi_definitions(tenant_id);
CREATE INDEX idx_kpi_def_tenant_status   ON kpi_definitions(tenant_id, status);
CREATE INDEX idx_kpi_def_tenant_category ON kpi_definitions(tenant_id, category);

CREATE TABLE kpi_measurements (
    id                   UUID PRIMARY KEY,
    tenant_id            UUID NOT NULL,
    kpi_id               UUID NOT NULL,
    period_start         TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end           TIMESTAMP WITH TIME ZONE NOT NULL,
    value                NUMERIC(24, 6) NOT NULL,
    unit                 VARCHAR(32),
    source               VARCHAR(32) NOT NULL,
    recorded_by_user_id  UUID,
    notes                VARCHAR(1000),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_kpi_measure_kpi FOREIGN KEY (kpi_id)
        REFERENCES kpi_definitions(id) ON DELETE CASCADE,
    CONSTRAINT uk_kpi_measure_period UNIQUE (kpi_id, period_start),
    CONSTRAINT chk_kpi_measure_source CHECK (source IN
        ('MANUAL','COMPUTED','IMPORT','IOT_AGGREGATED')),
    CONSTRAINT chk_kpi_measure_period CHECK (period_end > period_start)
);

CREATE INDEX idx_kpi_measure_tenant_kpi_period ON kpi_measurements(tenant_id, kpi_id, period_start);
CREATE INDEX idx_kpi_measure_tenant_period     ON kpi_measurements(tenant_id, period_start);

COMMENT ON TABLE kpi_definitions IS 'Définitions KPI par tenant (§6.2).';
COMMENT ON COLUMN kpi_definitions.direction IS
    'HIGHER_IS_BETTER : on monte (OEE, FPY). LOWER_IS_BETTER : on baisse (DPMO, MTTR).';
COMMENT ON TABLE kpi_measurements IS 'Time-series par KPI ; UNIQUE (kpi_id, period_start).';
