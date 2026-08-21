-- Le control plan traduit le PFMEA en contrôles réellement exécutés en production.
-- Sans lui, l'analyse de risque reste un document d'étude que rien ne relie au poste.
CREATE TABLE control_plans (
    id             UUID PRIMARY KEY,
    tenant_id      UUID        NOT NULL,
    product_id     UUID        NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    code           VARCHAR(64) NOT NULL,
    phase          VARCHAR(20) NOT NULL,
    revision       INTEGER     NOT NULL,
    status         VARCHAR(20) NOT NULL,
    owner_user_id  UUID,
    approved_by    UUID,
    approved_at    TIMESTAMPTZ,
    created_by     UUID        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

-- Un plan en vigueur par produit ET PAR PHASE : un produit en pré-série et en
-- série en a légitimement deux. Partiel, pour ne heurter ni les révisions
-- archivées ni le brouillon en préparation.
CREATE UNIQUE INDEX uk_control_plan_active_per_phase
    ON control_plans (tenant_id, product_id, phase)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_control_plans_product ON control_plans (tenant_id, product_id);

CREATE TABLE control_plan_lines (
    id                    UUID PRIMARY KEY,
    tenant_id             UUID         NOT NULL,
    plan_id               UUID         NOT NULL REFERENCES control_plans (id) ON DELETE CASCADE,
    sequence_no           INTEGER      NOT NULL,
    operation_id          UUID         REFERENCES product_operations (id) ON DELETE SET NULL,
    machine               VARCHAR(250),
    characteristic_no     VARCHAR(32),
    characteristic_label  VARCHAR(500) NOT NULL,
    characteristic_type   VARCHAR(20)  NOT NULL,
    special_class         VARCHAR(20),
    specification         VARCHAR(500),
    tolerance_lower       NUMERIC(18, 6),
    tolerance_upper       NUMERIC(18, 6),
    unit                  VARCHAR(24),
    measurement_technique VARCHAR(250),
    sample_size           INTEGER,
    sample_frequency      VARCHAR(120),
    control_method        VARCHAR(500),
    reaction_plan         VARCHAR(1000),
    -- Le lien qui dit POURQUOI ce contrôle existe. SET NULL et non CASCADE :
    -- supprimer une ligne de PFMEA ne doit pas supprimer un contrôle exécuté au poste.
    fmea_item_id          UUID         REFERENCES fmea_items (id) ON DELETE SET NULL
);

CREATE INDEX idx_control_plan_lines_plan ON control_plan_lines (plan_id, sequence_no);
CREATE INDEX idx_control_plan_lines_fmea ON control_plan_lines (fmea_item_id);
