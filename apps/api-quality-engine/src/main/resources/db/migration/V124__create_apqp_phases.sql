-- APQP : les cinq phases deviennent des données du client, pas une constante.
--
-- Elles vivaient en dur dans le code du front, identiques pour tout le monde.
-- Or l'APQP se personnalise : un équipementier automobile n'attend pas les
-- mêmes livrables qu'un fabricant de dispositifs médicaux, et le manuel AIAG
-- est un point de départ, pas un carcan.
--
-- Le référentiel standard reste néanmoins la valeur d'amorçage : à sa première
-- ouverture, un client reçoit les cinq phases et leurs livrables, puis adapte.
-- Partir d'un écran vide obligerait chacun à ressaisir un contenu normatif.

CREATE TABLE apqp_phases (
    id          UUID PRIMARY KEY,
    tenant_id   UUID         NOT NULL,

    -- Rang dans le cycle, à partir de 1. C'est lui qui dessine le V : la phase
    -- du milieu en est le point bas. Un entier plutôt qu'un ordre implicite,
    -- parce qu'insérer une phase entre deux autres ne doit pas dépendre de
    -- l'ordre d'insertion en base.
    position    INT          NOT NULL,

    title       VARCHAR(255) NOT NULL,
    -- Ce que la phase établit, en une phrase.
    purpose     VARCHAR(500),
    -- La question à laquelle ses livrables répondent.
    question    VARCHAR(500),

    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE apqp_deliverables (
    id          UUID PRIMARY KEY,
    tenant_id   UUID         NOT NULL,

    -- La suppression d'une phase emporte ses livrables : un livrable sans phase
    -- n'a pas d'existence propre, il n'est pas rattaché ailleurs.
    phase_id    UUID         NOT NULL REFERENCES apqp_phases (id) ON DELETE CASCADE,

    position    INT          NOT NULL,
    label       VARCHAR(500) NOT NULL,

    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- L'écran lit toujours « les phases de CE client, dans l'ordre ». Sans cet
-- index, chaque ouverture balaie la table entière.
CREATE INDEX idx_apqp_phases_tenant_position
    ON apqp_phases (tenant_id, position);

CREATE INDEX idx_apqp_deliverables_phase_position
    ON apqp_deliverables (phase_id, position);

-- Le rang est unique DANS un client : deux phases au même rang rendraient le V
-- indéterminé. `DEFERRABLE` parce qu'une réorganisation passe par des états
-- intermédiaires où deux phases se croisent le temps d'une transaction.
ALTER TABLE apqp_phases
    ADD CONSTRAINT uq_apqp_phases_tenant_position
    UNIQUE (tenant_id, position) DEFERRABLE INITIALLY DEFERRED;
