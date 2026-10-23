-- SPC : registre des études Gage R&R, répétabilité et reproductibilité des
-- moyens de mesure (ADR 0097).
--
-- La table porte une colonne tenant_id : la politique d'isolation par client
-- (ADR 0086) lui est posée automatiquement après la migration.
--
-- Les opérateurs (compte de l'annuaire et nom recopié) et la grille
-- [opérateur][pièce][essai] sont rangés en JSON dans des colonnes TEXT : ils se
-- lisent et s'écrivent d'un bloc, le SGBD ne les interroge jamais. Le %GRR, le
-- ndc et le verdict sont calculés par le serveur à chaque écriture ; le plan
-- reste dans le domaine de la table des constantes AIAG.
CREATE TABLE spc_gage_rr_studies (
    id                     UUID              NOT NULL DEFAULT gen_random_uuid(),
    tenant_id              UUID              NOT NULL,
    part_reference         VARCHAR(120)      NOT NULL,
    characteristic         VARCHAR(150),
    instrument             VARCHAR(150),
    lsl                    DOUBLE PRECISION  NOT NULL,
    usl                    DOUBLE PRECISION  NOT NULL,
    operator_count         INTEGER           NOT NULL,
    part_count             INTEGER           NOT NULL,
    trial_count            INTEGER           NOT NULL,
    sigma_multiplier       DOUBLE PRECISION  NOT NULL,
    study_date             DATE              NOT NULL,
    responsible_user_id    UUID,
    responsible_name       VARCHAR(150),
    operators              TEXT              NOT NULL,
    measurements           TEXT              NOT NULL,
    grr_percent            DOUBLE PRECISION  NOT NULL,
    grr_tolerance_percent  DOUBLE PRECISION  NOT NULL,
    ndc                    INTEGER           NOT NULL,
    decision               VARCHAR(20)       NOT NULL,
    anova_grr_percent      DOUBLE PRECISION  NOT NULL,
    anova_ndc              INTEGER           NOT NULL,
    created_by             UUID,
    created_at             TIMESTAMPTZ       NOT NULL,
    updated_at             TIMESTAMPTZ       NOT NULL,

    CONSTRAINT pk_spc_gage_rr_studies PRIMARY KEY (id),
    CONSTRAINT chk_spc_gage_rr_limits CHECK (usl > lsl),
    CONSTRAINT chk_spc_gage_rr_operators CHECK (operator_count BETWEEN 2 AND 3),
    CONSTRAINT chk_spc_gage_rr_parts CHECK (part_count BETWEEN 2 AND 10),
    CONSTRAINT chk_spc_gage_rr_trials CHECK (trial_count BETWEEN 2 AND 3),
    CONSTRAINT chk_spc_gage_rr_multiplier CHECK (sigma_multiplier IN (5.15, 6)),
    CONSTRAINT chk_spc_gage_rr_ndc CHECK (ndc >= 0 AND anova_ndc >= 0),
    CONSTRAINT chk_spc_gage_rr_decision CHECK (decision IN ('ACCEPTABLE', 'CONDITIONAL', 'NOT_ACCEPTABLE'))
);

CREATE INDEX ix_spc_gage_rr_studies_tenant ON spc_gage_rr_studies (tenant_id, study_date DESC);
