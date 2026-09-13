-- Rapport 8D : le dossier de résolution d'une non-conformité, figé à la clôture.
--
-- Une ligne par non-conformité, et trois colonnes de texte seulement — l'équipe
-- (D1), l'endiguement (D3), la reconnaissance (D8). Les cinq autres disciplines
-- n'ont AUCUNE colonne ici : elles sont agrégées à la lecture depuis la NC, les
-- Ishikawa et les 5 pourquoi rattachés, la CAPA escaladée, le mode de défaillance
-- PFMEA et les plans de surveillance du produit. Les recopier aurait créé une
-- seconde vérité, dont la seule certitude est qu'elle divergerait.
--
-- `snapshot_json` est l'exception, et elle est délibérée : à l'ÉMISSION, le texte
-- des huit disciplines est figé dans cette colonne. Un rapport émis doit dire, des
-- années plus tard, ce qu'il disait le jour de la clôture ; une vue recalculée
-- changerait avec les objets qu'elle relit — une CAPA rouverte, un Ishikawa
-- complété — et un 8D qui change n'est plus un 8D.
--
-- TEXT et non jsonb : le contenu n'est jamais interrogé par le SGBD, seulement
-- relu en entier.

CREATE TABLE nc_eightd_reports (
    id                UUID PRIMARY KEY,
    tenant_id         UUID         NOT NULL,
    nc_id             UUID         NOT NULL,
    status            VARCHAR(20)  NOT NULL,

    team              VARCHAR(4000),
    containment       VARCHAR(4000),
    recognition       VARCHAR(4000),

    snapshot_json     TEXT,
    sha256_hex        VARCHAR(64),
    signature         TEXT,
    anchor_tx_ref     VARCHAR(200),
    verification_code VARCHAR(64),
    issued_at         TIMESTAMPTZ,
    issued_by         UUID,
    issued_by_name    VARCHAR(255),

    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,

    CONSTRAINT chk_nc_eightd_status CHECK (status IN ('DRAFT', 'ISSUED')),

    -- Un rapport par non-conformité. Sans cette contrainte, deux émissions
    -- concurrentes produiraient deux documents portant la même référence et deux
    -- empreintes différentes, dont aucun ne serait opposable.
    CONSTRAINT uq_nc_eightd_reports_nc UNIQUE (tenant_id, nc_id),

    -- Le code de vérification est l'autorité de la route publique : il doit être
    -- unique globalement, car cette route n'a par construction aucun tenant.
    CONSTRAINT uq_nc_eightd_reports_code UNIQUE (verification_code),

    -- Émis veut dire PROUVÉ : une empreinte sans signature, ou une signature sans
    -- ancrage, serait une demi-preuve — donc pas une preuve. La base le tient
    -- aussi, et pas seulement l'agrégat : une écriture qui contournerait le service
    -- laisserait sinon une ligne indéfendable.
    CONSTRAINT chk_nc_eightd_issued_is_sealed CHECK (
        status <> 'ISSUED' OR (
            snapshot_json IS NOT NULL
            AND sha256_hex IS NOT NULL
            AND signature IS NOT NULL
            AND anchor_tx_ref IS NOT NULL
            AND verification_code IS NOT NULL
            AND issued_at IS NOT NULL
            AND issued_by IS NOT NULL
        )
    ),

    CONSTRAINT fk_nc_eightd_reports_nc FOREIGN KEY (nc_id)
        REFERENCES non_conformities (id) ON DELETE CASCADE
);

-- L'écran du rapport part TOUJOURS de (tenant, NC) ; l'index unique ci-dessus le
-- sert déjà. Celui-ci sert la route publique, qui ne connaît que le code.
CREATE INDEX idx_nc_eightd_reports_code ON nc_eightd_reports (verification_code);

COMMENT ON TABLE nc_eightd_reports IS
    'Rapport 8D d''une non-conformité : trois disciplines saisies, cinq agrégées, contenu figé et scellé à l''émission.';
COMMENT ON COLUMN nc_eightd_reports.snapshot_json IS
    'Texte figé des huit disciplines à l''émission : le rendu PDF en est une fonction pure, pour que l''empreinte scellée reste vérifiable.';
