-- Le coût de la qualité, selon le modèle PAF (ADR 0075).
--
-- Quatre familles : PRÉVENTION et APPRÉCIATION sont des dépenses de conformité,
-- DÉFAILLANCES INTERNES et EXTERNES des pertes de non-conformité. Une ligne est
-- UNE imputation — un montant, un responsable, une date — et c'est sa date
-- d'imputation qui la range dans un mois : aucun champ « période » à côté, qui
-- pourrait la contredire.

-- ---------------------------------------------------------------------------
-- 1. Les libellés : ce que la liste déroulante propose
-- ---------------------------------------------------------------------------

-- `tenant_id` NUL : libellé du catalogue livré, commun à tous les clients, avec
-- un `code` stable que l'écran traduit. Renseigné : libellé tapé en texte libre
-- par ce client, sans code, qui rejoint sa liste pour les mois suivants.
--
-- `part_control` rend obligatoires la référence, le nombre de pièces, le lot et
-- la date de réception ou de fabrication sur toute ligne de ce libellé.
CREATE TABLE coq_labels (
    id           UUID PRIMARY KEY,
    tenant_id    UUID,
    category     VARCHAR(32)  NOT NULL,
    code         VARCHAR(64),
    name         VARCHAR(150) NOT NULL,
    part_control BOOLEAN      NOT NULL DEFAULT FALSE,
    position     INTEGER      NOT NULL DEFAULT 1000,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_coq_labels_category
        CHECK (category IN ('PREVENTION', 'APPRAISAL', 'INTERNAL_FAILURE', 'EXTERNAL_FAILURE')),
    -- Un libellé livré a un code ; un libellé saisi n'en a pas. Les deux
    -- ensemble ou aucun des deux ne voudraient rien dire.
    CONSTRAINT ck_coq_labels_code_iff_builtin
        CHECK ((tenant_id IS NULL) = (code IS NOT NULL))
);

-- Deux « Rebuts » dans la même famille couperaient le cumul annuel en deux
-- lignes qu'on croirait différentes. Le service rend l'existant ; cet index
-- est le filet pour deux saisies simultanées.
CREATE UNIQUE INDEX ux_coq_labels_tenant_category_name
    ON coq_labels (tenant_id, category, lower(name))
    WHERE tenant_id IS NOT NULL;

CREATE UNIQUE INDEX ux_coq_labels_code ON coq_labels (code) WHERE code IS NOT NULL;

CREATE INDEX idx_coq_labels_tenant ON coq_labels (tenant_id);

-- Le catalogue livré, dans l'ordre de la maquette. Identifiants fixes : rejouer
-- la migration sur une autre base rend les mêmes, et une ligne saisie sur l'une
-- se relit sur l'autre sous le même libellé.
INSERT INTO coq_labels (id, tenant_id, category, code, name, part_control, position) VALUES
    ('c0a10000-0000-4000-8000-000000000101', NULL, 'PREVENTION', 'PREVENTION_TRAINING',          'Formation qualité du personnel',          FALSE, 10),
    ('c0a10000-0000-4000-8000-000000000102', NULL, 'PREVENTION', 'PREVENTION_PLANNING',          'Planification et système qualité',        FALSE, 20),
    ('c0a10000-0000-4000-8000-000000000103', NULL, 'PREVENTION', 'PREVENTION_INTERNAL_AUDITS',   'Audits qualité internes',                 FALSE, 30),
    ('c0a10000-0000-4000-8000-000000000104', NULL, 'PREVENTION', 'PREVENTION_DESIGN_REVIEW',     'Revue de conception / AMDEC',             FALSE, 40),

    ('c0a10000-0000-4000-8000-000000000201', NULL, 'APPRAISAL', 'APPRAISAL_INCOMING_INSPECTION', 'Contrôle réception matières',             TRUE,  10),
    ('c0a10000-0000-4000-8000-000000000202', NULL, 'APPRAISAL', 'APPRAISAL_IN_PROCESS',          'Inspections en cours de production',      TRUE,  20),
    ('c0a10000-0000-4000-8000-000000000203', NULL, 'APPRAISAL', 'APPRAISAL_TESTING_CALIBRATION', 'Essais et étalonnage',                    FALSE, 30),
    ('c0a10000-0000-4000-8000-000000000204', NULL, 'APPRAISAL', 'APPRAISAL_PRODUCT_AUDIT',       'Audit produit / process',                 TRUE,  40),

    ('c0a10000-0000-4000-8000-000000000301', NULL, 'INTERNAL_FAILURE', 'INTERNAL_SCRAP',         'Rebuts et mise au rebut',                 TRUE,  10),
    ('c0a10000-0000-4000-8000-000000000302', NULL, 'INTERNAL_FAILURE', 'INTERNAL_REWORK',        'Retouches et réparations',                TRUE,  20),
    ('c0a10000-0000-4000-8000-000000000303', NULL, 'INTERNAL_FAILURE', 'INTERNAL_REINSPECTION',  'Re-contrôles après retouche',             TRUE,  30),
    ('c0a10000-0000-4000-8000-000000000304', NULL, 'INTERNAL_FAILURE', 'INTERNAL_DOWNTIME',      'Arrêts de production liés aux défauts',   FALSE, 40),

    ('c0a10000-0000-4000-8000-000000000401', NULL, 'EXTERNAL_FAILURE', 'EXTERNAL_COMPLAINTS',    'Réclamations et traitement litiges',      FALSE, 10),
    ('c0a10000-0000-4000-8000-000000000402', NULL, 'EXTERNAL_FAILURE', 'EXTERNAL_RETURNS',       'Retours produits et remplacements',       TRUE,  20),
    ('c0a10000-0000-4000-8000-000000000403', NULL, 'EXTERNAL_FAILURE', 'EXTERNAL_WARRANTY',      'Garanties et interventions terrain',      FALSE, 30),
    ('c0a10000-0000-4000-8000-000000000404', NULL, 'EXTERNAL_FAILURE', 'EXTERNAL_PENALTIES',     'Pénalités contractuelles',                FALSE, 40);

-- ---------------------------------------------------------------------------
-- 2. Les imputations
-- ---------------------------------------------------------------------------

-- `category` et `part_control` sont recopiés du libellé à l'écriture : un
-- libellé ne change jamais de famille, et les totaux se calculent alors sans
-- jointure.
CREATE TABLE coq_entries (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID           NOT NULL,
    label_id            UUID           NOT NULL,
    category            VARCHAR(32)    NOT NULL,
    amount              NUMERIC(14, 2) NOT NULL,
    responsible         VARCHAR(150)   NOT NULL,
    imputation_date     DATE           NOT NULL,
    comment             VARCHAR(2000),

    part_control        BOOLEAN        NOT NULL DEFAULT FALSE,
    part_reference      VARCHAR(120),
    part_quantity       INTEGER,
    lot                 VARCHAR(120),
    received_or_made_on DATE,

    created_by          UUID,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT fk_coq_entries_label FOREIGN KEY (label_id) REFERENCES coq_labels (id),
    CONSTRAINT ck_coq_entries_category
        CHECK (category IN ('PREVENTION', 'APPRAISAL', 'INTERNAL_FAILURE', 'EXTERNAL_FAILURE')),
    CONSTRAINT ck_coq_entries_amount CHECK (amount >= 0),
    CONSTRAINT ck_coq_entries_responsible CHECK (length(btrim(responsible)) > 0),
    -- Le domaine l'exige déjà ; la base le garantit même pour une écriture qui
    -- ne passerait pas par lui. Une ligne de pièces sans lot ne remonte à rien.
    CONSTRAINT ck_coq_entries_part_fields CHECK (
        NOT part_control OR (
            part_reference IS NOT NULL AND length(btrim(part_reference)) > 0
            AND part_quantity IS NOT NULL AND part_quantity >= 1
            AND lot IS NOT NULL AND length(btrim(lot)) > 0
            AND received_or_made_on IS NOT NULL))
);

-- L'écran lit toujours « les lignes de CE client entre deux dates ».
CREATE INDEX idx_coq_entries_tenant_date ON coq_entries (tenant_id, imputation_date);

-- Une ligne rattachée à un libellé SAISI appartient au tenant de ce libellé.
-- Un CHECK ne peut pas interroger une autre table : d'où un déclencheur, filet
-- derrière le service qui ne propose jamais que les libellés visibles.
CREATE OR REPLACE FUNCTION coq_entries_label_visible()
RETURNS TRIGGER AS $$
DECLARE
    label_tenant UUID;
BEGIN
    SELECT tenant_id INTO label_tenant FROM coq_labels WHERE id = NEW.label_id;
    IF label_tenant IS NOT NULL AND label_tenant IS DISTINCT FROM NEW.tenant_id THEN
        RAISE EXCEPTION
            'coq_entries.tenant_id (%) cannot use label % owned by tenant %',
            NEW.tenant_id, NEW.label_id, label_tenant;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_coq_entries_label_visible
    BEFORE INSERT OR UPDATE OF label_id, tenant_id ON coq_entries
    FOR EACH ROW EXECUTE FUNCTION coq_entries_label_visible();

-- ---------------------------------------------------------------------------
-- 3. Le réglage du client : sa devise d'affichage
-- ---------------------------------------------------------------------------

-- Aucune conversion : les montants restent tels que saisis. Additionner des
-- euros et des dollars donnerait un total faux, d'où une devise par client,
-- pas par ligne.
CREATE TABLE coq_settings (
    tenant_id  UUID PRIMARY KEY,
    currency   CHAR(3)     NOT NULL DEFAULT 'EUR',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_coq_settings_currency CHECK (currency ~ '^[A-Z]{3}$')
);
