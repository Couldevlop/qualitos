-- Standards Hub §8.4 onglet 7 — audit blanc IA avancé : exécutions figées
-- (questions ciblées + gap analysis + plan de remédiation), générées par l'IA
-- à partir des preuves réelles du tenant. Multi-tenant : tenant_id obligatoire
-- (issu du JWT côté applicatif). Les listes (questions/écarts/plan) sont
-- stockées en JSON (colonne TEXT) — pas de @Lob.

CREATE TABLE standard_mock_audits (
    id                 UUID PRIMARY KEY,
    tenant_id          UUID NOT NULL,
    adoption_id        UUID NOT NULL,
    standard_id        UUID NOT NULL,
    standard_code      VARCHAR(100) NOT NULL,
    standard_name      VARCHAR(500) NOT NULL,
    readiness          DOUBLE PRECISION NOT NULL,
    major_count        INTEGER NOT NULL,
    minor_count        INTEGER NOT NULL,
    observation_count  INTEGER NOT NULL,
    question_count     INTEGER NOT NULL,
    questions_json     TEXT NOT NULL,
    gaps_json          TEXT NOT NULL,
    remediation_json   TEXT NOT NULL,
    ai_provider        VARCHAR(100),
    created_by         UUID NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_sma_readiness CHECK (readiness >= 0 AND readiness <= 100),
    CONSTRAINT chk_sma_counts CHECK (
        major_count >= 0 AND minor_count >= 0
        AND observation_count >= 0 AND question_count >= 0
    )
);

CREATE INDEX idx_sma_tenant ON standard_mock_audits (tenant_id);
CREATE INDEX idx_sma_tenant_adoption ON standard_mock_audits (tenant_id, adoption_id);

COMMENT ON TABLE standard_mock_audits IS
    'Exécutions d''audit blanc IA avancé (Standards Hub §8.4 onglet 7) : questions ciblées, gap analysis et plan de remédiation.';
