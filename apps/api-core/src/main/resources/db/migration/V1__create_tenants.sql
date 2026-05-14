-- =============================================================================
-- V1 : Table tenants avec UUID primary key et Row-Level Security
-- =============================================================================

CREATE TABLE tenants (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    slug        VARCHAR(63) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    plan        VARCHAR(20)  NOT NULL DEFAULT 'STARTER',
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_tenants PRIMARY KEY (id),
    CONSTRAINT uq_tenants_slug UNIQUE (slug),
    CONSTRAINT chk_tenants_plan CHECK (plan IN ('STARTER', 'PRO', 'ENTERPRISE')),
    CONSTRAINT chk_tenants_slug CHECK (slug ~ '^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$')
);

-- Index pour les recherches par slug (fréquentes)
CREATE INDEX idx_tenants_slug ON tenants (slug);
CREATE INDEX idx_tenants_active ON tenants (active) WHERE active = TRUE;

-- Trigger pour maintenir updated_at automatiquement
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tenants_updated_at
    BEFORE UPDATE ON tenants
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Row-Level Security
-- La RLS sur la table tenants est gérée au niveau applicatif (Super Admin only).
-- Le Super Admin utilise un rôle PostgreSQL avec BYPASSRLS pour la gestion globale.
-- Les utilisateurs applicatifs voient uniquement leur propre tenant via la RLS.
ALTER TABLE tenants ENABLE ROW LEVEL SECURITY;

-- Policy : un utilisateur ne peut lire que son propre tenant
-- (le setting app.tenant_id est positionné par le service applicatif)
CREATE POLICY tenants_isolation_policy ON tenants
    FOR ALL
    USING (id::TEXT = current_setting('app.tenant_id', TRUE)
        OR current_setting('app.bypass_rls', TRUE) = 'true');

-- Commentaires de documentation
COMMENT ON TABLE tenants IS 'Organisations clientes multi-tenant de la plateforme QualitOS';
COMMENT ON COLUMN tenants.slug IS 'Identifiant URL-safe unique, ex: hopital-paris-nord';
COMMENT ON COLUMN tenants.plan IS 'Plan tarifaire : STARTER | PRO | ENTERPRISE';
