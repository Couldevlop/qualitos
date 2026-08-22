-- Le PFMEA et le control plan sont des documents approuvés : ils ne se mettent pas
-- à jour tout seuls après une NC. Ils reçoivent une PROPOSITION, qu'un humain
-- accepte ou refuse — et le refus est tracé, parce que ne pas bouger est aussi une
-- décision qualité que l'auditeur voudra lire.
CREATE TABLE quality_revision_requests (
    id                UUID PRIMARY KEY,
    tenant_id         UUID         NOT NULL,
    product_id        UUID         NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    target_type       VARCHAR(32)  NOT NULL,
    target_id         UUID,
    trigger_type      VARCHAR(20)  NOT NULL,
    trigger_ref_id    UUID         NOT NULL,
    trigger_ref_label VARCHAR(120) NOT NULL,
    rationale         VARCHAR(1000) NOT NULL,
    -- TEXT et non JSONB : les tests tournent sur H2, qui ne connaît pas JSONB.
    -- La sérialisation est explicite côté service, jamais déléguée à Hibernate.
    proposed_change   TEXT         NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    decided_by        UUID,
    decided_at        TIMESTAMPTZ,
    decision_note     VARCHAR(1000),
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL
);

-- L'idempotence est portée par la base, pas par un contrôle en Java qui perdrait
-- la course entre deux NC saisies au même instant : une seule demande en attente
-- par cible.
CREATE UNIQUE INDEX uk_revision_request_pending
    ON quality_revision_requests (tenant_id, target_type, target_id)
    WHERE status = 'PENDING' AND target_id IS NOT NULL;

CREATE INDEX idx_revision_requests_product
    ON quality_revision_requests (tenant_id, product_id, status);
CREATE INDEX idx_revision_requests_trigger
    ON quality_revision_requests (tenant_id, trigger_ref_id);
