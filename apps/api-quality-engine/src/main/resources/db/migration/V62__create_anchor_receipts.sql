-- Ancrage blockchain — Phase A (ADR 0012).
-- Reçu d'ancrage signé et CHAÎNÉ : chaque reçu commit un Merkle root (racine
-- des integrity_hash d'un lot d'audit_events) avec une signature hybride
-- (Ed25519 + ML-DSA-65, ADR 0011) et référence le hash du reçu précédent du
-- tenant → chaîne inviolable, vérifiable sans réseau Fabric. RGPD : aucun
-- donnée personnelle on-receipt, uniquement des hashes (§11.3).

CREATE TABLE anchor_receipts (
    id                  UUID         NOT NULL,
    tenant_id           UUID         NOT NULL,
    seq_no              BIGINT       NOT NULL,
    merkle_root         VARCHAR(64)  NOT NULL,
    prev_receipt_hash   VARCHAR(64)  NOT NULL,
    receipt_hash        VARCHAR(64)  NOT NULL,
    signature           TEXT         NOT NULL,
    signed_at           TIMESTAMPTZ  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_anchor_receipts PRIMARY KEY (id),
    CONSTRAINT uq_anchor_receipts_tenant_seq  UNIQUE (tenant_id, seq_no),
    CONSTRAINT uq_anchor_receipts_tenant_hash UNIQUE (tenant_id, receipt_hash),
    CONSTRAINT chk_anchor_merkle_root  CHECK (merkle_root ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_anchor_receipt_hash CHECK (receipt_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_anchor_prev_hash    CHECK (prev_receipt_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_anchor_seq_positive CHECK (seq_no >= 1)
);

CREATE INDEX idx_anchor_receipts_tenant_seq ON anchor_receipts (tenant_id, seq_no DESC);

-- Immutabilité (append-only) : on rejette tout UPDATE/DELETE au niveau base
-- (§11.5 "logs immuables"). L'application n'insère que.
CREATE OR REPLACE FUNCTION anchor_receipts_immutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'anchor_receipts is append-only (UPDATE/DELETE forbidden)';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_anchor_receipts_immutable
    BEFORE UPDATE OR DELETE ON anchor_receipts
    FOR EACH ROW EXECUTE FUNCTION anchor_receipts_immutable();

COMMENT ON TABLE anchor_receipts IS
    'Reçus d''ancrage signés et chaînés (ADR 0012 Phase A). Append-only.';
COMMENT ON COLUMN anchor_receipts.merkle_root IS
    'Racine Merkle SHA-256 du lot d''audit_events ancrés par ce reçu.';
COMMENT ON COLUMN anchor_receipts.prev_receipt_hash IS
    'receipt_hash du reçu précédent du tenant (64 zéros = genèse) → chaîne.';
COMMENT ON COLUMN anchor_receipts.signature IS
    'Enveloppe de signature hybride Ed25519+ML-DSA-65 du merkle_root (Base64URL).';
