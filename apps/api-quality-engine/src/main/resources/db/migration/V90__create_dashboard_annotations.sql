-- N1 1.3 — Collaborative dashboard annotations (CLAUDE.md §7.3, ADR 0034).
-- A comment posted on a dashboard chart. tenant_id + author_id always come from
-- the validated JWT (§18.2 #2); body is plain text (Angular escapes at render).
-- Annotations are immutable except for deletion (author or tenant admin).

CREATE TABLE dashboard_annotations (
    id            UUID         NOT NULL,
    tenant_id     UUID         NOT NULL,
    author_id     UUID         NOT NULL,
    chart_key     VARCHAR(64)  NOT NULL,
    anchor_label  VARCHAR(120),
    body          VARCHAR(2000) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_dashboard_annotations PRIMARY KEY (id),
    CONSTRAINT chk_da_body       CHECK (length(trim(body)) >= 1),
    CONSTRAINT chk_da_chart_key  CHECK (chart_key ~ '^[a-z0-9]+([._-][a-z0-9]+){0,5}$')
);

CREATE INDEX idx_da_tenant_chart  ON dashboard_annotations (tenant_id, chart_key, created_at DESC);
CREATE INDEX idx_da_tenant_author ON dashboard_annotations (tenant_id, author_id);

COMMENT ON TABLE  dashboard_annotations IS
    'Collaborative comments on dashboard charts (CLAUDE.md §7.3).';
COMMENT ON COLUMN dashboard_annotations.chart_key IS
    'Stable opaque chart identifier within a dashboard (e.g. exec.trend).';
COMMENT ON COLUMN dashboard_annotations.author_id IS
    'JWT sub of the author — never taken from the request body (§18.2 #2).';
