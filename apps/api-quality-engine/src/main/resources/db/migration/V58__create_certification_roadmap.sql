-- Roadmap de certification (CLAUDE.md §8.5) : les étapes chronologiques d'un
-- projet de certification, instanciées par adoption (tenant_standard) à partir
-- d'une trame générique de 19 étapes. Chaque étape porte durée, responsable,
-- livrables, modules QualitOS impliqués, et un suivi (statut, dates, assignation).
-- Multi-tenant : tenant_id présent + FK vers tenant_standards (CASCADE).

CREATE TABLE certification_roadmap_stages (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id           UUID         NOT NULL,
    tenant_standard_id  UUID         NOT NULL,
    step_number         INTEGER      NOT NULL,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    typical_duration    VARCHAR(60),
    deliverables        TEXT,
    responsible_role    VARCHAR(120),
    involved_modules    VARCHAR(255),
    status              VARCHAR(20)  NOT NULL DEFAULT 'NOT_STARTED',
    assignee_id         UUID,
    planned_start_date  DATE,
    planned_end_date    DATE,
    actual_start_date   DATE,
    actual_end_date     DATE,
    notes               TEXT,
    order_index         INTEGER      NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_certification_roadmap_stages PRIMARY KEY (id),
    CONSTRAINT fk_roadmap_stage_ts FOREIGN KEY (tenant_standard_id)
        REFERENCES tenant_standards (id) ON DELETE CASCADE,
    CONSTRAINT uk_roadmap_stage_ts_step UNIQUE (tenant_standard_id, step_number),
    CONSTRAINT chk_roadmap_stage_status CHECK (
        status IN ('NOT_STARTED','IN_PROGRESS','DONE','SKIPPED')
    )
);

CREATE INDEX idx_roadmap_stages_ts ON certification_roadmap_stages (tenant_standard_id);
CREATE INDEX idx_roadmap_stages_tenant ON certification_roadmap_stages (tenant_id);
