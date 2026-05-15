-- V17: Industry Packs (CLAUDE.md §5)

CREATE TABLE industry_packs (
    id              UUID PRIMARY KEY,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    description     VARCHAR(1000),
    version         VARCHAR(32)  NOT NULL,
    locale          VARCHAR(16),
    tags_csv        VARCHAR(1000),
    manifest_json   TEXT         NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_industry_pack_code UNIQUE (code),
    CONSTRAINT chk_industry_pack_code CHECK (code ~ '^[a-z0-9][a-z0-9_-]{1,62}$')
);

CREATE TABLE tenant_industry_pack_activations (
    id                       UUID PRIMARY KEY,
    tenant_id                UUID NOT NULL,
    pack_code                VARCHAR(64) NOT NULL,
    status                   VARCHAR(32) NOT NULL,
    activated_by             UUID NOT NULL,
    activated_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    deactivated_at           TIMESTAMP WITH TIME ZONE,
    deactivated_by           UUID,
    config_overrides_json    TEXT,
    CONSTRAINT chk_tipa_status CHECK (status IN ('ACTIVE', 'DEACTIVATED'))
);

CREATE INDEX idx_tipa_tenant      ON tenant_industry_pack_activations(tenant_id);
CREATE INDEX idx_tipa_tenant_pack ON tenant_industry_pack_activations(tenant_id, pack_code);

-- Une seule activation ACTIVE par (tenant, pack) ; l'historique reste accessible.
CREATE UNIQUE INDEX uk_tipa_active_per_tenant_pack
    ON tenant_industry_pack_activations(tenant_id, pack_code)
    WHERE status = 'ACTIVE';

COMMENT ON TABLE industry_packs IS 'Catalogue de packs sectoriels chargés depuis YAML au démarrage.';
COMMENT ON TABLE tenant_industry_pack_activations IS 'Historique des activations de packs par tenant (audit ISO 19011).';
