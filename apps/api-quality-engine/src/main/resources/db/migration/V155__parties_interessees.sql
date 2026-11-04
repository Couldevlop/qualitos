-- Registre des parties intéressées (ISO 9001 §4.2).
--
-- Pourquoi. La norme demande de déterminer les parties intéressées PERTINENTES
-- et leurs exigences. La plateforme savait tenir les fournisseurs, les clients
-- par leurs réclamations et les autorités par les normes visées — mais aucun
-- écran ne répondait à la question de l'auditeur : « montrez-moi votre registre
-- des parties intéressées, et dites-moi ce que chacune exige de vous ».
--
-- Deux cotations, pas une. L'impact sur la qualité et l'impact réglementaire se
-- notent séparément parce qu'ils ne se compensent pas : une autorité de tutelle
-- dont l'impact qualité est faible reste critique. La criticité globale n'est
-- donc PAS stockée — elle se calcule comme le plus élevé des deux, et un impact
-- modifié ne peut jamais laisser derrière lui une criticité périmée.
CREATE TABLE stakeholders (
    id                        UUID PRIMARY KEY,
    tenant_id                 UUID         NOT NULL,

    -- identification
    name                      VARCHAR(255) NOT NULL,
    category                  VARCHAR(40)  NOT NULL,
    side                      VARCHAR(20)  NOT NULL,
    description               TEXT,
    contact                   VARCHAR(500),
    sites                     VARCHAR(500),

    -- analyse
    needs                     TEXT,
    requirements_summary      TEXT,
    relevance                 VARCHAR(20)  NOT NULL DEFAULT 'TO_DETERMINE',
    relevance_justification   TEXT,
    risks_and_opportunities   TEXT,
    quality_impact            VARCHAR(10)  NOT NULL DEFAULT 'LOW',
    regulatory_impact         VARCHAR(10)  NOT NULL DEFAULT 'LOW',
    -- Le responsable interne de la relation : son compte, et son nom RECOPIÉ.
    -- Une copie et non une jointure : la fiche doit rester lisible des années
    -- plus tard, quand le compte a disparu de l'annuaire (ADR 0091).
    owner_account_id          UUID,
    owner_name                VARCHAR(255),

    -- pilotage
    controls                  TEXT,
    indicators                VARCHAR(500),
    documents                 VARCHAR(500),
    actions                   VARCHAR(500),
    next_review_on            DATE,
    last_reviewed_on          DATE,

    created_at                TIMESTAMPTZ  NOT NULL,
    created_by                UUID,
    updated_at                TIMESTAMPTZ  NOT NULL,
    updated_by                UUID,

    CONSTRAINT chk_stakeholders_side
        CHECK (side IN ('INTERNAL', 'EXTERNAL')),
    CONSTRAINT chk_stakeholders_relevance
        CHECK (relevance IN ('RELEVANT', 'NOT_RELEVANT', 'TO_DETERMINE')),
    CONSTRAINT chk_stakeholders_quality_impact
        CHECK (quality_impact IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_stakeholders_regulatory_impact
        CHECK (regulatory_impact IN ('LOW', 'MEDIUM', 'HIGH'))
);

-- Le registre se lit toujours client par client.
CREATE INDEX idx_stakeholders_tenant ON stakeholders (tenant_id);

-- Deux fiches de même nom dans la même famille sont un doublon, pas une nuance :
-- l'unicité est posée en base, insensible à la casse, et non seulement dans le
-- service — l'API est publique (§13.1).
CREATE UNIQUE INDEX uq_stakeholders_tenant_category_name
    ON stakeholders (tenant_id, category, LOWER(name));

-- Les revues en retard sont la première question d'une revue de direction.
CREATE INDEX idx_stakeholders_tenant_review
    ON stakeholders (tenant_id, next_review_on)
    WHERE next_review_on IS NOT NULL;

-- Les exigences portées par chaque partie intéressée.
--
-- Table fille et non colonne JSON : une exigence se compte, se filtre par
-- statut, et se rattache à un risque, une non-conformité, un audit, un document
-- ou une CAPA. C'est ce rattachement qui fait du registre autre chose qu'un
-- tableau — le référentiel transverse de §3.6. Un JSON interdirait de répondre
-- à « quelles exigences sont à réévaluer, toutes parties confondues ? » sans
-- tout relire.
CREATE TABLE stakeholder_requirements (
    id              UUID PRIMARY KEY,
    -- Dénormalisé depuis la fiche parente, et jamais pour la lecture : c'est
    -- cette colonne qui met la table sous la politique de cloisonnement, posée
    -- dynamiquement sur TOUTE table portant un tenant_id (ADR 0086). Sans elle,
    -- la table resterait hors de la seconde barrière : le filtre applicatif
    -- tiendrait seul, et le jour où un chemin chargerait une exigence par son
    -- seul identifiant, la fuite serait silencieuse.
    tenant_id       UUID         NOT NULL,
    stakeholder_id  UUID         NOT NULL,
    -- L'ordre d'inscription : celui dans lequel la fiche les affiche.
    position        INTEGER      NOT NULL,

    need            VARCHAR(500),
    requirement     VARCHAR(500) NOT NULL,
    origin          VARCHAR(500),
    nature          VARCHAR(20),
    risk            VARCHAR(500),
    risk_level      VARCHAR(10),
    control_measure VARCHAR(500),
    indicator       VARCHAR(500),
    responsible     VARCHAR(500),
    evidence        VARCHAR(500),
    status          VARCHAR(20)  NOT NULL DEFAULT 'TO_ANALYSE',

    -- Les liens vers les autres modules. Aucune clé étrangère : ces objets
    -- vivent leur vie, et un risque clos ne doit pas empêcher de lire
    -- l'exigence qu'il portait.
    risk_id         UUID,
    nc_id           UUID,
    audit_id        UUID,
    document_id     UUID,
    capa_id         UUID,

    CONSTRAINT fk_stakeholder_requirements_stakeholder
        FOREIGN KEY (stakeholder_id) REFERENCES stakeholders (id) ON DELETE CASCADE,
    CONSTRAINT chk_stakeholder_requirements_status
        CHECK (status IN ('TO_ANALYSE', 'APPLICABLE', 'CONTROLLED', 'TO_REASSESS')),
    CONSTRAINT chk_stakeholder_requirements_nature
        CHECK (nature IS NULL
               OR nature IN ('CONTRACTUAL', 'REGULATORY', 'NORMATIVE', 'VOLUNTARY', 'INTERNAL')),
    CONSTRAINT chk_stakeholder_requirements_risk_level
        CHECK (risk_level IS NULL OR risk_level IN ('LOW', 'MEDIUM', 'HIGH'))
);

CREATE INDEX idx_stakeholder_requirements_stakeholder
    ON stakeholder_requirements (stakeholder_id, position);

-- La politique de cloisonnement filtre sur tenant_id : sans index, chaque
-- lecture d'exigence la contrôlerait ligne à ligne.
CREATE INDEX idx_stakeholder_requirements_tenant
    ON stakeholder_requirements (tenant_id);

-- « Qu'est-ce qui est à réévaluer ? » est la question d'après un avenant ou une
-- révision de norme : elle doit se poser sans balayer la table.
CREATE INDEX idx_stakeholder_requirements_status
    ON stakeholder_requirements (status);
