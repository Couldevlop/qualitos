-- Le registre des risques et opportunités (ISO 9001 §6.1, ADR 0076).
--
-- Deux registres de même structure — identification, cotation, traitement,
-- exigences couvertes — mais pas de même cotation : gravité × probabilité,
-- brute puis résiduelle visée, pour un risque ; gain attendu × faisabilité,
-- avec une échéance visée, pour une opportunité. Deux tables plutôt qu'une
-- table à colonnes optionnelles : chacune garde ses NOT NULL et ses CHECK.
--
-- Aucune suppression prévue : un risque se clôt, une opportunité s'écarte. La
-- fiche compte comme preuve d'une exigence ; une preuve effacée ne se présente
-- pas à l'auditeur.

-- ---------------------------------------------------------------------------
-- 1. Les risques
-- ---------------------------------------------------------------------------

CREATE TABLE risk_register_risks (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID          NOT NULL,
    reference               VARCHAR(20)   NOT NULL,
    title                   VARCHAR(255)  NOT NULL,
    type                    VARCHAR(32)   NOT NULL,
    process                 VARCHAR(120)  NOT NULL,
    site                    VARCHAR(120),
    owner                   VARCHAR(150)  NOT NULL,
    cause                   VARCHAR(4000),
    effect                  VARCHAR(4000),
    origin                  VARCHAR(32)   NOT NULL DEFAULT 'DIRECT',
    origin_ref              VARCHAR(120),
    -- L'objet dont le risque est issu (ligne d'AMDEC, NC, constat d'audit,
    -- changement), relu par le serveur dans le client du jeton à la création.
    source_id               UUID,
    gross_severity          SMALLINT      NOT NULL,
    gross_probability       SMALLINT      NOT NULL,
    residual_severity       SMALLINT,
    residual_probability    SMALLINT,
    decision                VARCHAR(32)   NOT NULL DEFAULT 'UNDECIDED',
    status                  VARCHAR(32)   NOT NULL DEFAULT 'TO_TREAT',
    requirements            VARCHAR(255)  NOT NULL DEFAULT '',
    next_review_on          DATE,
    effectiveness_criterion VARCHAR(1000),
    created_by              UUID,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),

    -- La référence est attribuée par le serveur et reprise par les CAPA ouvertes
    -- depuis la fiche : unique dans le client, c'est le filet de deux créations
    -- simultanées (le service passe alors au rang suivant ; ici, 409).
    CONSTRAINT uq_risk_register_risks_reference UNIQUE (tenant_id, reference),
    CONSTRAINT ck_risk_register_risks_type
        CHECK (type IN ('QUALITY', 'ENVIRONMENT', 'HEALTH_SAFETY', 'INFORMATION_SECURITY', 'LEGAL')),
    CONSTRAINT ck_risk_register_risks_origin
        CHECK (origin IN ('DIRECT', 'MANAGEMENT_REVIEW', 'AUDIT', 'CUSTOMER_FEEDBACK', 'MONITORING',
                          'FMEA', 'NON_CONFORMITY', 'INCIDENT', 'CHANGE')),
    CONSTRAINT ck_risk_register_risks_decision
        CHECK (decision IN ('UNDECIDED', 'REDUCE', 'ACCEPT', 'AVOID', 'TRANSFER')),
    CONSTRAINT ck_risk_register_risks_status
        CHECK (status IN ('TO_TREAT', 'IN_TREATMENT', 'MONITORED', 'ACCEPTED', 'CLOSED')),
    CONSTRAINT ck_risk_register_risks_gross
        CHECK (gross_severity BETWEEN 1 AND 5 AND gross_probability BETWEEN 1 AND 5),
    -- Les deux notes résiduelles ensemble ou aucune, et jamais au-dessus de la
    -- brute : viser plus haut que l'existant n'est pas un traitement. Les
    -- IS NOT NULL sont indispensables : `NULL BETWEEN 1 AND 5` vaut NULL, et un
    -- CHECK laisse passer NULL — une note sur deux passerait sans eux.
    CONSTRAINT ck_risk_register_risks_residual CHECK (
        (residual_severity IS NULL AND residual_probability IS NULL)
        OR (residual_severity IS NOT NULL AND residual_probability IS NOT NULL
            AND residual_severity BETWEEN 1 AND 5 AND residual_probability BETWEEN 1 AND 5
            AND residual_severity * residual_probability <= gross_severity * gross_probability)),
    -- Une source n'a de sens que pour une origine qui désigne un objet.
    CONSTRAINT ck_risk_register_risks_source CHECK (
        source_id IS NULL OR origin IN ('FMEA', 'NON_CONFORMITY', 'AUDIT', 'CHANGE')),
    CONSTRAINT ck_risk_register_risks_title CHECK (length(btrim(title)) > 0),
    CONSTRAINT ck_risk_register_risks_process CHECK (length(btrim(process)) > 0),
    CONSTRAINT ck_risk_register_risks_owner CHECK (length(btrim(owner)) > 0)
);

CREATE INDEX idx_risk_register_risks_tenant ON risk_register_risks (tenant_id);

CREATE INDEX idx_risk_register_risks_source
    ON risk_register_risks (tenant_id, origin, source_id) WHERE source_id IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 2. Les opportunités
-- ---------------------------------------------------------------------------

CREATE TABLE risk_register_opportunities (
    id                UUID PRIMARY KEY,
    tenant_id         UUID          NOT NULL,
    reference         VARCHAR(20)   NOT NULL,
    title             VARCHAR(255)  NOT NULL,
    type              VARCHAR(32)   NOT NULL,
    process           VARCHAR(120)  NOT NULL,
    site              VARCHAR(120),
    owner             VARCHAR(150)  NOT NULL,
    target_date       DATE,
    context           VARCHAR(4000),
    benefit           VARCHAR(4000),
    origin            VARCHAR(32)   NOT NULL DEFAULT 'DIRECT',
    origin_ref        VARCHAR(120),
    gain              SMALLINT      NOT NULL,
    feasibility       SMALLINT      NOT NULL,
    decision          VARCHAR(32)   NOT NULL DEFAULT 'UNDECIDED',
    status            VARCHAR(32)   NOT NULL DEFAULT 'UNDER_STUDY',
    requirements      VARCHAR(255)  NOT NULL DEFAULT '',
    benefit_criterion VARCHAR(1000),
    created_by        UUID,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_risk_register_opportunities_reference UNIQUE (tenant_id, reference),
    CONSTRAINT ck_risk_register_opportunities_type
        CHECK (type IN ('QUALITY', 'ENVIRONMENT', 'HEALTH_SAFETY', 'INFORMATION_SECURITY', 'LEGAL')),
    -- Une opportunité ne naît ni d'une AMDEC, ni d'une non-conformité, ni d'un
    -- incident, ni d'un changement : ces sources analysent ce qui peut mal tourner.
    CONSTRAINT ck_risk_register_opportunities_origin
        CHECK (origin IN ('DIRECT', 'MANAGEMENT_REVIEW', 'AUDIT', 'CUSTOMER_FEEDBACK', 'MONITORING')),
    CONSTRAINT ck_risk_register_opportunities_decision
        CHECK (decision IN ('UNDECIDED', 'PLAN', 'STUDY', 'POSTPONE', 'DISCARD')),
    CONSTRAINT ck_risk_register_opportunities_status
        CHECK (status IN ('UNDER_STUDY', 'PLANNED', 'IN_PROGRESS', 'DONE', 'DISCARDED')),
    CONSTRAINT ck_risk_register_opportunities_rating
        CHECK (gain BETWEEN 1 AND 5 AND feasibility BETWEEN 1 AND 5),
    CONSTRAINT ck_risk_register_opportunities_title CHECK (length(btrim(title)) > 0),
    CONSTRAINT ck_risk_register_opportunities_process CHECK (length(btrim(process)) > 0),
    CONSTRAINT ck_risk_register_opportunities_owner CHECK (length(btrim(owner)) > 0)
);

CREATE INDEX idx_risk_register_opportunities_tenant ON risk_register_opportunities (tenant_id);

-- ---------------------------------------------------------------------------
-- 3. Les actions d'une opportunité (ACT-n)
-- ---------------------------------------------------------------------------

-- Pas des CAPA : une CAPA traite un écart, et ses indicateurs (délai de
-- clôture, récidive) n'ont pas de sens pour un projet d'amélioration.
CREATE TABLE risk_register_actions (
    id             UUID PRIMARY KEY,
    tenant_id      UUID         NOT NULL,
    opportunity_id UUID         NOT NULL,
    number         INTEGER      NOT NULL,
    title          VARCHAR(255) NOT NULL,
    due_date       DATE,
    status         VARCHAR(32)  NOT NULL DEFAULT 'TO_START',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_risk_register_actions_opportunity
        FOREIGN KEY (opportunity_id) REFERENCES risk_register_opportunities (id),
    -- « ACT-3 » désigne une seule action dans le client, où qu'on la cite.
    CONSTRAINT uq_risk_register_actions_number UNIQUE (tenant_id, number),
    CONSTRAINT ck_risk_register_actions_number CHECK (number >= 1),
    CONSTRAINT ck_risk_register_actions_status
        CHECK (status IN ('TO_START', 'IN_PROGRESS', 'DONE', 'CANCELLED')),
    CONSTRAINT ck_risk_register_actions_title CHECK (length(btrim(title)) > 0)
);

CREATE INDEX idx_risk_register_actions_opportunity
    ON risk_register_actions (tenant_id, opportunity_id);

-- Une action appartient au client de son opportunité. Un CHECK ne peut pas
-- interroger une autre table : d'où un déclencheur, filet derrière le service
-- qui ne charge jamais une opportunité que dans le tenant du jeton.
CREATE OR REPLACE FUNCTION risk_register_actions_same_tenant()
RETURNS TRIGGER AS $$
DECLARE
    opportunity_tenant UUID;
BEGIN
    SELECT tenant_id INTO opportunity_tenant
      FROM risk_register_opportunities WHERE id = NEW.opportunity_id;
    IF opportunity_tenant IS DISTINCT FROM NEW.tenant_id THEN
        RAISE EXCEPTION
            'risk_register_actions.tenant_id (%) differs from the tenant (%) of opportunity %',
            NEW.tenant_id, opportunity_tenant, NEW.opportunity_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_risk_register_actions_same_tenant
    BEFORE INSERT OR UPDATE OF opportunity_id, tenant_id ON risk_register_actions
    FOR EACH ROW EXECUTE FUNCTION risk_register_actions_same_tenant();

-- ---------------------------------------------------------------------------
-- 4. Le suivi d'une fiche
-- ---------------------------------------------------------------------------

-- Des codes, pas des phrases (« 3x3 », « IN_TREATMENT ») : l'écran compose la
-- phrase dans la langue de l'utilisateur. Le journal d'audit chaîné reste la
-- trace opposable ; celui-ci est la chronologie lisible de la fiche.
CREATE TABLE risk_register_events (
    id          UUID PRIMARY KEY,
    tenant_id   UUID         NOT NULL,
    item_kind   VARCHAR(16)  NOT NULL,
    item_id     UUID         NOT NULL,
    event_type  VARCHAR(32)  NOT NULL,
    from_value  VARCHAR(64),
    to_value    VARCHAR(64),
    detail      VARCHAR(255),
    actor_id    UUID,
    occurred_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_risk_register_events_kind CHECK (item_kind IN ('RISK', 'OPPORTUNITY')),
    CONSTRAINT ck_risk_register_events_type
        CHECK (event_type IN ('CREATED', 'RATING_CHANGED', 'RESIDUAL_CHANGED', 'STATUS_CHANGED',
                              'DECISION_CHANGED', 'ACTION_OPENED'))
);

CREATE INDEX idx_risk_register_events_item
    ON risk_register_events (tenant_id, item_kind, item_id, occurred_at DESC);

-- ---------------------------------------------------------------------------
-- 5. La CAPA ouverte depuis un risque
-- ---------------------------------------------------------------------------

-- Le dossier CAPA porte son origine : source RISK, référence du risque. La
-- contrainte d'origine datait de la V6 et ignorait déjà SPC_ALERT et ANOMALY,
-- que l'énumération Java proposait : elle est réécrite sur l'énumération
-- complète, pour que la base et le code disent enfin la même chose.
ALTER TABLE capa_cases DROP CONSTRAINT chk_capa_cases_source;

ALTER TABLE capa_cases ADD CONSTRAINT chk_capa_cases_source CHECK (
    source_type IN ('NON_CONFORMITY', 'AUDIT', 'COMPLAINT', 'INTERNAL', 'IOT_ALERT',
                    'SPC_ALERT', 'ANOMALY', 'RISK', 'OTHER'));

CREATE INDEX idx_capa_cases_tenant_source
    ON capa_cases (tenant_id, source_type, source_ref);
