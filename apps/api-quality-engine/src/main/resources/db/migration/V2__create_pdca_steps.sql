CREATE TABLE pdca_steps (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    cycle_id    UUID        NOT NULL,
    phase       VARCHAR(10) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    assignee_id UUID,
    due_date    DATE,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_pdca_steps PRIMARY KEY (id),
    CONSTRAINT fk_pdca_steps_cycle FOREIGN KEY (cycle_id)
        REFERENCES pdca_cycles (id) ON DELETE CASCADE,
    CONSTRAINT chk_pdca_steps_phase CHECK (
        phase IN ('PLAN', 'DO', 'CHECK', 'ACT')
    ),
    CONSTRAINT chk_pdca_steps_status CHECK (
        status IN ('PENDING', 'IN_PROGRESS', 'DONE')
    )
);

CREATE INDEX idx_pdca_steps_cycle_id ON pdca_steps (cycle_id);
CREATE INDEX idx_pdca_steps_cycle_phase ON pdca_steps (cycle_id, phase);
