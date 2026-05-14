-- =============================================================================
-- V2 : Table tenant_modules — modules activés par tenant
-- =============================================================================

CREATE TABLE tenant_modules (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL,
    module_name VARCHAR(100) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_tenant_modules PRIMARY KEY (id),
    CONSTRAINT fk_tenant_modules_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE,
    CONSTRAINT uq_tenant_modules_tenant_module
        UNIQUE (tenant_id, module_name),
    CONSTRAINT chk_tenant_modules_name
        CHECK (module_name ~ '^[a-z][a-z0-9_-]{0,99}$')
);

-- Index pour les lookups fréquents
CREATE INDEX idx_tenant_modules_tenant_id ON tenant_modules (tenant_id);
CREATE INDEX idx_tenant_modules_active ON tenant_modules (tenant_id, active) WHERE active = TRUE;

-- Trigger updated_at
CREATE TRIGGER trg_tenant_modules_updated_at
    BEFORE UPDATE ON tenant_modules
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Row-Level Security — isolation par tenant
ALTER TABLE tenant_modules ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_modules_isolation_policy ON tenant_modules
    FOR ALL
    USING (tenant_id::TEXT = current_setting('app.tenant_id', TRUE)
        OR current_setting('app.bypass_rls', TRUE) = 'true');

-- Modules de base activés par défaut pour tous les tenants (insérés lors de la création du tenant)
-- Cette insertion est gérée par le code applicatif (TenantService.create) — pas ici.

COMMENT ON TABLE tenant_modules IS 'Modules QualitOS activés par tenant (PDCA, 5S, DMAIC, etc.)';
COMMENT ON COLUMN tenant_modules.module_name IS 'Identifiant du module, ex: pdca, five-s, ishikawa, dmaic, quality-circle';
