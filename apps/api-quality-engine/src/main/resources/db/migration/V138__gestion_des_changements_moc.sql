-- La gestion des changements (MOC) — ADR 0087.
--
-- Le module « changements » devient le MOC : demande, analyse d'impact sur huit
-- domaines, validation par rôles, historique. La table change_requests garde son
-- nom et ses identifiants : le registre des risques cite des changements comme
-- origine, et le tableau de bord SMI en relit les échéances.
--
-- Chaque nouvelle table porte tenant_id : la politique d'isolation RLS lui est
-- posée automatiquement après la migration (ADR 0086).

-- ---------------------------------------------------------------------------
-- 1. La demande : nouvelles colonnes
-- ---------------------------------------------------------------------------

ALTER TABLE change_requests DROP CONSTRAINT chk_change_type;
ALTER TABLE change_requests DROP CONSTRAINT chk_change_priority;
ALTER TABLE change_requests DROP CONSTRAINT chk_change_status;

ALTER TABLE change_requests ADD COLUMN site VARCHAR(200);
ALTER TABLE change_requests ADD COLUMN justification VARCHAR(4000);
ALTER TABLE change_requests ADD COLUMN requester_name VARCHAR(200);
ALTER TABLE change_requests ADD COLUMN impact_score INTEGER;
ALTER TABLE change_requests ADD COLUMN classification VARCHAR(20) NOT NULL DEFAULT 'NOT_ASSESSED';
ALTER TABLE change_requests ADD COLUMN submitted_at TIMESTAMPTZ;
ALTER TABLE change_requests ADD COLUMN closed_at TIMESTAMPTZ;
-- Deux validateurs qui statuent au même instant ne concluent pas la demande deux fois.
ALTER TABLE change_requests ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- ---------------------------------------------------------------------------
-- 2. L'historique (créé d'abord : la reprise y consigne l'ancien état)
-- ---------------------------------------------------------------------------

CREATE TABLE change_history (
    id           UUID          PRIMARY KEY,
    tenant_id    UUID          NOT NULL,
    change_id    UUID          NOT NULL REFERENCES change_requests (id) ON DELETE CASCADE,
    occurred_at  TIMESTAMPTZ   NOT NULL,
    event        VARCHAR(40)   NOT NULL,
    actor_id     UUID,
    actor_name   VARCHAR(200),
    detail       VARCHAR(1000),
    CONSTRAINT chk_change_history_event CHECK (event IN (
        'CREATED', 'UPDATED', 'SENT_TO_ANALYSIS', 'ANALYSIS_COMPLETED', 'SUBMITTED',
        'VALIDATION_APPROVED', 'VALIDATION_REJECTED', 'APPROVED', 'REJECTED', 'REOPENED',
        'REMINDER_SENT', 'LEGACY_STATUS', 'LEGACY_DECISION', 'LEGACY_IMPACT'))
);

CREATE INDEX ix_change_history_change ON change_history (change_id, occurred_at);

-- ---------------------------------------------------------------------------
-- 3. Reprise des demandes antérieures au MOC
-- ---------------------------------------------------------------------------
--
-- Rien ne se perd : ce que le nouveau modèle ne porte plus (ancien statut,
-- décisions par personne, objets liés) est écrit à l'historique de la demande.

INSERT INTO change_history (id, tenant_id, change_id, occurred_at, event, actor_id, detail)
SELECT gen_random_uuid(), tenant_id, id, created_at, 'CREATED', requester_user_id, NULL
FROM change_requests;

INSERT INTO change_history (id, tenant_id, change_id, occurred_at, event, actor_id, detail)
SELECT gen_random_uuid(), a.tenant_id, a.change_id, COALESCE(a.decided_at, a.created_at), 'LEGACY_DECISION',
       a.approver_user_id, LEFT(a.decision || COALESCE(' — ' || a.comment, ''), 1000)
FROM change_approvals a
WHERE a.decision <> 'PENDING';

INSERT INTO change_history (id, tenant_id, change_id, occurred_at, event, actor_id, detail)
SELECT gen_random_uuid(), i.tenant_id, i.change_id, i.created_at, 'LEGACY_IMPACT', NULL,
       LEFT(i.target_type || ' ' || i.target_id::text || COALESCE(' — ' || i.notes, ''), 1000)
FROM change_impacts i;

INSERT INTO change_history (id, tenant_id, change_id, occurred_at, event, actor_id, detail)
SELECT gen_random_uuid(), tenant_id, id, updated_at, 'LEGACY_STATUS', NULL,
       LEFT(status
            || COALESCE(' — ' || implemented_at::text, '')
            || COALESCE(' — ' || rejection_reason, ''), 1000)
FROM change_requests
WHERE status IN ('SUBMITTED', 'UNDER_REVIEW', 'IMPLEMENTED', 'CANCELLED');

-- L'ancienne « analyse de risque » est ce qui se rapproche le plus de la justification.
UPDATE change_requests SET justification = risk_assessment WHERE risk_assessment IS NOT NULL;

-- Soumises ou en revue : sans analyse par domaine, elles ne peuvent pas être
-- devant des validateurs par rôle. Elles repartent en analyse.
-- Mises en œuvre : elles avaient été approuvées. Annulées : closes sans suite.
UPDATE change_requests SET status = CASE status
    WHEN 'SUBMITTED'    THEN 'IN_ANALYSIS'
    WHEN 'UNDER_REVIEW' THEN 'IN_ANALYSIS'
    WHEN 'IMPLEMENTED'  THEN 'APPROVED'
    WHEN 'CANCELLED'    THEN 'REJECTED'
    ELSE status END;

UPDATE change_requests SET closed_at = updated_at WHERE status IN ('APPROVED', 'REJECTED');

-- L'ancienne nature (document, procédé…) n'a pas d'équivalent : tout changement
-- repris est réputé permanent.
UPDATE change_requests SET type = 'PERMANENT';

ALTER TABLE change_requests ADD CONSTRAINT chk_change_type CHECK (type IN ('PERMANENT', 'TEMPORARY'));
ALTER TABLE change_requests ADD CONSTRAINT chk_change_status CHECK (status IN
    ('DRAFT', 'IN_ANALYSIS', 'IN_VALIDATION', 'APPROVED', 'REJECTED'));
ALTER TABLE change_requests ADD CONSTRAINT chk_change_classification CHECK (classification IN
    ('NOT_ASSESSED', 'MINOR', 'MAJOR', 'CRITICAL'));
ALTER TABLE change_requests ADD CONSTRAINT chk_change_score CHECK (impact_score IS NULL OR impact_score BETWEEN 0 AND 100);

ALTER TABLE change_requests DROP COLUMN priority;
ALTER TABLE change_requests DROP COLUMN owner_user_id;
ALTER TABLE change_requests DROP COLUMN implemented_at;
ALTER TABLE change_requests DROP COLUMN risk_assessment;
ALTER TABLE change_requests DROP COLUMN rejection_reason;

DROP TABLE change_approvals;
DROP TABLE change_impacts;

COMMENT ON COLUMN change_requests.status IS
    'DRAFT → IN_ANALYSIS → IN_VALIDATION → APPROVED | REJECTED ; REJECTED → IN_ANALYSIS (réouverture).';
COMMENT ON COLUMN change_requests.impact_summary IS
    'Synthèse des natures d''impact (domaines touchés) : l''effet d''un risque créé depuis la demande.';

-- ---------------------------------------------------------------------------
-- 4. Les codes MOC-AAAA-NNN, par client et par année
-- ---------------------------------------------------------------------------

CREATE TABLE change_code_counters (
    tenant_id   UUID     NOT NULL,
    counter_year  INTEGER  NOT NULL,
    last_value    INTEGER  NOT NULL,
    PRIMARY KEY (tenant_id, counter_year),
    CONSTRAINT chk_change_code_counters_value CHECK (last_value >= 0)
);

-- Un code déjà au format MOC ne doit pas être réattribué.
INSERT INTO change_code_counters (tenant_id, counter_year, last_value)
SELECT tenant_id, CAST(substring(code FROM 5 FOR 4) AS INTEGER), MAX(CAST(substring(code FROM 10) AS INTEGER))
FROM change_requests
WHERE code ~ '^MOC-[0-9]{4}-[0-9]{1,9}$'
GROUP BY tenant_id, substring(code FROM 5 FOR 4);

-- ---------------------------------------------------------------------------
-- 5. L'analyse d'impact : un niveau par domaine
-- ---------------------------------------------------------------------------

CREATE TABLE change_impact_assessments (
    id           UUID           PRIMARY KEY,
    tenant_id    UUID           NOT NULL,
    change_id    UUID           NOT NULL REFERENCES change_requests (id) ON DELETE CASCADE,
    domain       VARCHAR(32)    NOT NULL,
    level        VARCHAR(16)    NOT NULL,
    notes        VARCHAR(2000),
    assessed_by  UUID           NOT NULL,
    assessed_at  TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uq_change_impact_domain UNIQUE (change_id, domain),
    CONSTRAINT chk_change_impact_domain CHECK (domain IN ('SAFETY', 'HEALTH_ENVIRONMENT', 'QUALITY',
        'PRODUCTION', 'MAINTENANCE', 'COSTS', 'REGULATORY', 'TRAINING')),
    CONSTRAINT chk_change_impact_level CHECK (level IN ('NONE', 'LOW', 'MEDIUM', 'HIGH'))
);

-- ---------------------------------------------------------------------------
-- 6. Les cartes de validation, une par rôle requis, figées à la soumission
-- ---------------------------------------------------------------------------

CREATE TABLE change_validations (
    id                UUID           PRIMARY KEY,
    tenant_id         UUID           NOT NULL,
    change_id         UUID           NOT NULL REFERENCES change_requests (id) ON DELETE CASCADE,
    ordinal           INTEGER        NOT NULL,
    -- Le rôle du paramétrage, pour savoir qui peut statuer AU MOMENT de la décision ;
    -- son libellé est recopié : un renommage ne réécrit pas une demande en cours.
    role_id           UUID           NOT NULL,
    role_label        VARCHAR(100)   NOT NULL,
    decision          VARCHAR(16)    NOT NULL,
    decided_by        UUID,
    decided_by_name   VARCHAR(200),
    comment           VARCHAR(1000),
    decided_at        TIMESTAMPTZ,
    last_reminded_at  TIMESTAMPTZ,
    created_at        TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uq_change_validation_role UNIQUE (change_id, role_id),
    CONSTRAINT chk_change_validation_decision CHECK (decision IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_change_validation_decided CHECK ((decision = 'PENDING') = (decided_at IS NULL)),
    -- Un refus se motive.
    CONSTRAINT chk_change_validation_reason CHECK (decision <> 'REJECTED'
        OR (comment IS NOT NULL AND length(trim(comment)) > 0))
);

CREATE INDEX ix_change_validations_change ON change_validations (change_id, ordinal);
CREATE INDEX ix_change_validations_role ON change_validations (tenant_id, role_id, decision);

-- ---------------------------------------------------------------------------
-- 7. Le paramétrage du client (« Paramètres MOC », administrateur seulement)
-- ---------------------------------------------------------------------------
--
-- Sans ligne, le client reçoit les valeurs livrées : seuils 25 / 50, poids 1,
-- rappel à 3 jours, et les quatre rôles de la maquette sans titulaire.

CREATE TABLE moc_settings (
    tenant_id           UUID         PRIMARY KEY,
    major_threshold     INTEGER      NOT NULL,
    critical_threshold  INTEGER      NOT NULL,
    reminder_days       INTEGER      NOT NULL,
    updated_by          UUID         NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_moc_settings_thresholds CHECK (major_threshold > 0
        AND major_threshold < critical_threshold AND critical_threshold <= 100),
    CONSTRAINT chk_moc_settings_reminder CHECK (reminder_days BETWEEN 1 AND 30)
);

CREATE TABLE moc_domain_weights (
    id         UUID         PRIMARY KEY,
    tenant_id  UUID         NOT NULL,
    domain     VARCHAR(32)  NOT NULL,
    weight     INTEGER      NOT NULL,
    CONSTRAINT uq_moc_domain_weights UNIQUE (tenant_id, domain),
    CONSTRAINT chk_moc_domain_weights_domain CHECK (domain IN ('SAFETY', 'HEALTH_ENVIRONMENT', 'QUALITY',
        'PRODUCTION', 'MAINTENANCE', 'COSTS', 'REGULATORY', 'TRAINING')),
    CONSTRAINT chk_moc_domain_weights_weight CHECK (weight BETWEEN 0 AND 10)
);

-- L'unicité des libellés (sans casse) est tenue par le service : un renommage
-- croisé de deux rôles dans un même enregistrement heurterait un index unique.
CREATE TABLE moc_validation_roles (
    id                 UUID          PRIMARY KEY,
    tenant_id          UUID          NOT NULL,
    ordinal            INTEGER       NOT NULL,
    label              VARCHAR(100)  NOT NULL,
    required_minor     BOOLEAN       NOT NULL,
    required_major     BOOLEAN       NOT NULL,
    required_critical  BOOLEAN       NOT NULL,
    CONSTRAINT chk_moc_validation_roles_label CHECK (length(trim(label)) > 0)
);

CREATE INDEX ix_moc_validation_roles_tenant ON moc_validation_roles (tenant_id, ordinal);

-- Les titulaires : des membres de l'annuaire du client (identifiant de connexion),
-- leur adresse recopiée pour l'affichage. Principal en premier (ordinal 0).
CREATE TABLE moc_role_holders (
    id          UUID          PRIMARY KEY,
    tenant_id   UUID          NOT NULL,
    role_id     UUID          NOT NULL REFERENCES moc_validation_roles (id) ON DELETE CASCADE,
    user_id     UUID          NOT NULL,
    user_label  VARCHAR(200)  NOT NULL,
    ordinal     INTEGER       NOT NULL,
    CONSTRAINT uq_moc_role_holders UNIQUE (role_id, user_id)
);

CREATE INDEX ix_moc_role_holders_tenant ON moc_role_holders (tenant_id, user_id);
