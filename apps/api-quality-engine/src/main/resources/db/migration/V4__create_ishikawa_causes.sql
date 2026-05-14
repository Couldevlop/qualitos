CREATE TABLE ishikawa_causes (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    diagram_id        UUID         NOT NULL,
    parent_id         UUID,
    category          VARCHAR(20)  NOT NULL,
    label             VARCHAR(500) NOT NULL,
    description       TEXT,
    root_cause_score  DOUBLE PRECISION,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_ishikawa_causes PRIMARY KEY (id),
    CONSTRAINT fk_ishikawa_causes_diagram FOREIGN KEY (diagram_id)
        REFERENCES ishikawa_diagrams (id) ON DELETE CASCADE,
    CONSTRAINT fk_ishikawa_causes_parent FOREIGN KEY (parent_id)
        REFERENCES ishikawa_causes (id) ON DELETE CASCADE,
    CONSTRAINT chk_ishikawa_causes_category CHECK (
        category IN (
            'METHODS', 'MANPOWER', 'MACHINES', 'MATERIALS',
            'MEASUREMENTS', 'ENVIRONMENT', 'MANAGEMENT', 'MONEY'
        )
    ),
    CONSTRAINT chk_ishikawa_causes_score CHECK (
        root_cause_score IS NULL OR (root_cause_score >= 0.0 AND root_cause_score <= 1.0)
    )
);

CREATE INDEX idx_ishikawa_causes_diagram_id ON ishikawa_causes (diagram_id);
CREATE INDEX idx_ishikawa_causes_diagram_category ON ishikawa_causes (diagram_id, category);
CREATE INDEX idx_ishikawa_causes_parent_id ON ishikawa_causes (parent_id);
