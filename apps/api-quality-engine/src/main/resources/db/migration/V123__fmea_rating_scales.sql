-- Le référentiel de cotation FMEA, propre à chaque tenant.
--
-- Coter Sévérité, Occurrence et Détection de 1 à 10 ne veut rien dire sans
-- l'échelle qui donne le sens des chiffres. Cette échelle n'est pas universelle :
-- « perturbation majeure du service » ne recouvre pas la même réalité dans un
-- atelier de sertissage, un laboratoire d'analyses et un centre d'appels. Un
-- barème imposé se traduit par des cotations que personne ne croit.
--
-- MODÈLE PAR DÉFAUT ET NON PAR COPIE : un tenant qui n'a rien redéfini n'a
-- AUCUNE ligne ici, et l'application sert le barème de référence. Recopier les
-- trente lignes à la création de chaque tenant aurait fige le référentiel au
-- jour de son inscription, et rendu impossible de distinguer « pas touché » de
-- « redéfini à l'identique ».
CREATE TABLE fmea_rating_scale_rows (
    id           UUID PRIMARY KEY,
    tenant_id    UUID         NOT NULL,
    -- SEVERITY | OCCURRENCE | DETECTION
    kind         VARCHAR(20)  NOT NULL,
    -- 10 (le plus grave, le plus fréquent, le moins détectable) à 1.
    score        SMALLINT     NOT NULL,
    label        VARCHAR(120) NOT NULL,
    description  VARCHAR(500),
    -- Propres à l'occurrence : période et taux de défaillance. Nuls ailleurs,
    -- plutôt que trois tables qui ne différeraient que par deux colonnes.
    time_period  VARCHAR(120),
    failure_rate VARCHAR(120),
    updated_by   UUID         NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,

    CONSTRAINT chk_fmea_scale_kind CHECK (kind IN ('SEVERITY', 'OCCURRENCE', 'DETECTION')),
    -- Une échelle va de 1 à 10 : c'est ce que le RPN multiplie. Une valeur hors
    -- bornes ferait sortir le produit de la plage attendue sans qu'aucun écran
    -- ne le signale.
    CONSTRAINT chk_fmea_scale_score CHECK (score BETWEEN 1 AND 10),
    -- Un seul barème par échelle et par tenant : deux lignes pour le même score
    -- rendraient l'affichage dépendant de l'ordre de lecture de la base.
    CONSTRAINT uk_fmea_scale_row UNIQUE (tenant_id, kind, score)
);

CREATE INDEX idx_fmea_scale_tenant_kind ON fmea_rating_scale_rows (tenant_id, kind, score DESC);
