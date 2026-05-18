-- P5 — Marketplace of standards / industry packs (CLAUDE.md §8.11).
-- Cross-tenant catalog: a pack is published by a publisher and consumed by tenants.

CREATE TABLE marketplace_packs (
    id                UUID         NOT NULL,
    pack_id           VARCHAR(64)  NOT NULL,
    version           VARCHAR(32)  NOT NULL,
    publisher         VARCHAR(120) NOT NULL,
    title             VARCHAR(250) NOT NULL,
    description       VARCHAR(4000),
    sector            VARCHAR(80)  NOT NULL,
    price_cents       INTEGER      NOT NULL DEFAULT 0,
    currency          VARCHAR(8)   NOT NULL DEFAULT 'EUR',
    verified          BOOLEAN      NOT NULL DEFAULT FALSE,
    verified_by       UUID,
    verified_at       TIMESTAMPTZ,
    signature_hash    VARCHAR(128),
    manifest_url      VARCHAR(2000) NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_marketplace_packs PRIMARY KEY (id),
    CONSTRAINT uq_mp_packid_version  UNIQUE (pack_id, version),
    CONSTRAINT chk_mp_packid         CHECK (pack_id ~ '^[a-z][a-z0-9_-]{1,63}$'),
    CONSTRAINT chk_mp_version        CHECK (version ~ '^\d+\.\d+(\.\d+)?$'),
    CONSTRAINT chk_mp_price_nonneg   CHECK (price_cents >= 0),
    CONSTRAINT chk_mp_currency       CHECK (currency IN ('EUR','USD','GBP','CHF','JPY')),
    CONSTRAINT chk_mp_manifest_url   CHECK (manifest_url LIKE 'https://%' OR manifest_url LIKE 'oci://%')
);

CREATE INDEX idx_mp_sector            ON marketplace_packs (sector);
CREATE INDEX idx_mp_verified          ON marketplace_packs (verified);
CREATE INDEX idx_mp_publisher         ON marketplace_packs (publisher);

COMMENT ON TABLE  marketplace_packs IS
    'Cross-tenant marketplace of industry/standards packs (CLAUDE.md P5 §8.11).';
COMMENT ON COLUMN marketplace_packs.signature_hash IS
    'Publisher signature; the platform admin (super admin) verifies before flipping verified=true.';
