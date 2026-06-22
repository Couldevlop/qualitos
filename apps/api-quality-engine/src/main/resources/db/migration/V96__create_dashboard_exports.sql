-- V96 — Signed & anchored dashboard PDF exports (CLAUDE.md §7.3/§7.4, ADR 0043).
-- One immutable receipt per official PDF export: SHA-256 fingerprint of the
-- rendered PDF, hybrid Ed25519+ML-DSA-65 signature envelope (ADR 0011), and the
-- blockchain anchor reference (§11.3). The opaque verification_code is embedded
-- in the PDF QR code and resolved by the public verify endpoint.
-- tenant_id + user_id ALWAYS come from the validated JWT (§18.2 #2).

CREATE TABLE dashboard_exports (
    id                 UUID          NOT NULL,
    tenant_id          UUID          NOT NULL,
    user_id            UUID          NOT NULL,
    dashboard_id       UUID          NOT NULL,
    dashboard_name     VARCHAR(200)  NOT NULL,
    verification_code  VARCHAR(64)   NOT NULL,
    sha256_hex         VARCHAR(64)   NOT NULL,
    signature_envelope TEXT          NOT NULL,
    anchor_tx_ref      VARCHAR(200)  NOT NULL,
    created_at         TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_dashboard_exports PRIMARY KEY (id),
    CONSTRAINT uq_de_verification_code UNIQUE (verification_code),
    CONSTRAINT chk_de_sha256 CHECK (sha256_hex ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_de_code   CHECK (verification_code ~ '^[A-Za-z0-9_-]{16,64}$')
);

CREATE INDEX idx_de_tenant_dashboard ON dashboard_exports (tenant_id, dashboard_id, created_at DESC);

COMMENT ON TABLE  dashboard_exports IS
    'Immutable receipts of signed & anchored dashboard PDF exports (CLAUDE.md §7.3/§7.4).';
COMMENT ON COLUMN dashboard_exports.verification_code IS
    'Opaque random code embedded in the PDF QR; the authority for public verification.';
COMMENT ON COLUMN dashboard_exports.sha256_hex IS
    'SHA-256 of the rendered PDF bytes (canonical fingerprint that is signed and anchored).';
COMMENT ON COLUMN dashboard_exports.signature_envelope IS
    'Base64URL hybrid Ed25519+ML-DSA-65 signature envelope over the fingerprint (ADR 0011).';
COMMENT ON COLUMN dashboard_exports.user_id IS
    'JWT sub of the exporting user — never taken from the request body (§18.2 #2).';
