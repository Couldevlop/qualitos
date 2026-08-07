-- Analyse des 5 Pourquoi rattachée à une non-conformité (§3.5).
--
-- La méthode existait dans la plateforme comme sous-causes d'un diagramme
-- Ishikawa. Imbriquée dans un arbre cause-effet, elle n'était ni identifiable ni
-- consultable pour elle-même, et l'on ne pouvait pas partir d'une non-conformité
-- pour la dérouler — c'est pourtant son point de départ naturel.
CREATE TABLE five_whys_analyses (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL,
    -- L'écart d'où part l'analyse : une analyse orpheline n'a pas de sujet.
    nc_id       UUID         NOT NULL,
    problem     VARCHAR(500) NOT NULL,
    -- Nulle tant que l'analyse n'aboutit pas : sans cela, une analyse en cours
    -- et une analyse sans conclusion ne se distingueraient plus.
    root_cause  TEXT,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_five_whys_analyses PRIMARY KEY (id),
    CONSTRAINT fk_five_whys_analyses_nc FOREIGN KEY (nc_id)
        REFERENCES non_conformities (id) ON DELETE CASCADE,
    CONSTRAINT chk_five_whys_problem CHECK (length(trim(problem)) > 0)
);

CREATE INDEX idx_five_whys_analyses_tenant ON five_whys_analyses (tenant_id, created_at DESC);
CREATE INDEX idx_five_whys_analyses_nc     ON five_whys_analyses (nc_id, tenant_id);

-- Une SUITE de pourquoi, et non cinq colonnes figées : cinq est un ordre de
-- grandeur, pas un dogme. Trois qui atteignent la cause racine valent mieux que
-- cinq qui la dépassent, et certaines défaillances en demandent sept.
CREATE TABLE five_whys_steps (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL,
    analysis_id UUID        NOT NULL,
    -- L'ordre EST le sens de la méthode.
    position    INTEGER     NOT NULL,
    answer      TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_five_whys_steps PRIMARY KEY (id),
    CONSTRAINT fk_five_whys_steps_analysis FOREIGN KEY (analysis_id)
        REFERENCES five_whys_analyses (id) ON DELETE CASCADE,
    -- Deux réponses au même rang rendraient la chaîne illisible.
    CONSTRAINT uq_five_whys_steps_position UNIQUE (analysis_id, position),
    CONSTRAINT chk_five_whys_steps_position CHECK (position BETWEEN 1 AND 7),
    CONSTRAINT chk_five_whys_steps_answer CHECK (length(trim(answer)) > 0)
);

CREATE INDEX idx_five_whys_steps_analysis ON five_whys_steps (analysis_id, tenant_id, position);
