-- Les circuits de validation paramétrables (ADR 0080).
--
-- Un client règle, pour un type d'objet (une version de document…), une suite
-- d'étapes : chacune nomme le rôle qui approuve et combien de ses porteurs.
-- Sans ligne ici, l'objet garde l'approbation simple d'avant.

-- ---------------------------------------------------------------------------
-- 1. Le circuit réglé par le client
-- ---------------------------------------------------------------------------

CREATE TABLE circuit_steps (
    tenant_id      UUID          NOT NULL,
    subject        VARCHAR(40)   NOT NULL,
    ordinal        INTEGER       NOT NULL,
    name           VARCHAR(120)  NOT NULL,
    role_code      VARCHAR(64)   NOT NULL,
    min_approvals  INTEGER       NOT NULL,
    updated_by     UUID          NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL,
    PRIMARY KEY (tenant_id, subject, ordinal),
    CONSTRAINT chk_circuit_steps_ordinal CHECK (ordinal BETWEEN 0 AND 9),
    CONSTRAINT chk_circuit_steps_role CHECK (role_code ~ '^[A-Z][A-Z0-9_]{1,63}$' AND role_code <> 'SUPER_ADMIN'),
    CONSTRAINT chk_circuit_steps_min CHECK (min_approvals BETWEEN 1 AND 10)
);

-- ---------------------------------------------------------------------------
-- 2. Les passages : les étapes y sont COPIÉES au départ
-- ---------------------------------------------------------------------------
--
-- Changer le circuit du client ne change pas un passage commencé : sinon un
-- réglage ferait approuver un objet déjà soumis par moins de monde que prévu.

CREATE TABLE circuit_runs (
    id            UUID          PRIMARY KEY,
    tenant_id     UUID          NOT NULL,
    subject       VARCHAR(40)   NOT NULL,
    subject_id    UUID          NOT NULL,
    author_id     UUID,
    status        VARCHAR(20)   NOT NULL,
    current_step  INTEGER       NOT NULL,
    started_at    TIMESTAMPTZ   NOT NULL,
    ended_at      TIMESTAMPTZ,
    -- Deux approbateurs au même instant ne franchissent pas l'étape deux fois.
    version       BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT chk_circuit_runs_status CHECK (status IN ('IN_PROGRESS', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_circuit_runs_ended CHECK ((status = 'IN_PROGRESS') = (ended_at IS NULL))
);

CREATE INDEX ix_circuit_runs_subject ON circuit_runs (tenant_id, subject, subject_id, started_at DESC);

-- Un objet n'a qu'un passage ouvert à la fois, même sous deux soumissions concurrentes.
CREATE UNIQUE INDEX uq_circuit_runs_open ON circuit_runs (tenant_id, subject, subject_id)
    WHERE status = 'IN_PROGRESS';

CREATE TABLE circuit_run_steps (
    run_id         UUID          NOT NULL REFERENCES circuit_runs (id) ON DELETE CASCADE,
    ordinal        INTEGER       NOT NULL,
    name           VARCHAR(120)  NOT NULL,
    role_code      VARCHAR(64)   NOT NULL,
    min_approvals  INTEGER       NOT NULL,
    PRIMARY KEY (run_id, ordinal),
    CONSTRAINT chk_circuit_run_steps_min CHECK (min_approvals BETWEEN 1 AND 10)
);

CREATE TABLE circuit_decisions (
    run_id      UUID           NOT NULL REFERENCES circuit_runs (id) ON DELETE CASCADE,
    ordinal     INTEGER        NOT NULL,
    step_index  INTEGER        NOT NULL,
    actor_id    UUID           NOT NULL,
    approved    BOOLEAN        NOT NULL,
    comment     VARCHAR(1000),
    decided_at  TIMESTAMPTZ    NOT NULL,
    PRIMARY KEY (run_id, ordinal),
    -- Quatre yeux : une personne décide une fois par passage, à n'importe quelle étape.
    CONSTRAINT uq_circuit_decisions_actor UNIQUE (run_id, actor_id),
    -- Un refus se motive.
    CONSTRAINT chk_circuit_decisions_reason CHECK (approved OR comment IS NOT NULL)
);

-- ---------------------------------------------------------------------------
-- 3. Le refus d'une version de document, et sa raison
-- ---------------------------------------------------------------------------
--
-- Une version refusée revient en brouillon : l'auteur doit lire pourquoi.

ALTER TABLE document_versions ADD COLUMN rejected_by UUID;
ALTER TABLE document_versions ADD COLUMN rejected_at TIMESTAMPTZ;
ALTER TABLE document_versions ADD COLUMN rejection_reason VARCHAR(1000);
