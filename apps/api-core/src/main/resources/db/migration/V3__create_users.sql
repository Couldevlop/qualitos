-- =============================================================================
-- V3 : Table app_users + app_user_roles avec RLS policies
-- =============================================================================

CREATE TABLE app_users (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id    UUID         NOT NULL,
    keycloak_id  VARCHAR(36)  NOT NULL,
    email        VARCHAR(255) NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_app_users PRIMARY KEY (id),
    CONSTRAINT fk_app_users_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE,
    CONSTRAINT uq_app_users_keycloak_id UNIQUE (keycloak_id),
    CONSTRAINT uq_app_users_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT chk_app_users_email
        CHECK (email ~* '^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$')
);

-- Index pour les lookups fréquents
CREATE INDEX idx_app_users_tenant_id ON app_users (tenant_id);
CREATE INDEX idx_app_users_keycloak_id ON app_users (keycloak_id);
CREATE INDEX idx_app_users_active ON app_users (tenant_id, active) WHERE active = TRUE;

-- Trigger updated_at
CREATE TRIGGER trg_app_users_updated_at
    BEFORE UPDATE ON app_users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Rôles de l'utilisateur (collection @ElementCollection Hibernate)
CREATE TABLE app_user_roles (
    user_id  UUID        NOT NULL,
    role     VARCHAR(50) NOT NULL,

    CONSTRAINT pk_app_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_app_user_roles_user
        FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
    CONSTRAINT chk_app_user_roles_role
        CHECK (role IN (
            'SUPER_ADMIN',
            'ADMIN',
            'QUALITY_DIRECTOR',
            'QUALITY_MANAGER',
            'AUDITOR',
            'USER',
            'EXTERNAL_AUDITOR'
        ))
);

CREATE INDEX idx_app_user_roles_user_id ON app_user_roles (user_id);

-- Row-Level Security sur app_users — isolation par tenant
ALTER TABLE app_users ENABLE ROW LEVEL SECURITY;

CREATE POLICY app_users_isolation_policy ON app_users
    FOR ALL
    USING (tenant_id::TEXT = current_setting('app.tenant_id', TRUE)
        OR current_setting('app.bypass_rls', TRUE) = 'true');

-- RLS sur app_user_roles — isolation par tenant via jointure avec app_users
ALTER TABLE app_user_roles ENABLE ROW LEVEL SECURITY;

CREATE POLICY app_user_roles_isolation_policy ON app_user_roles
    FOR ALL
    USING (
        EXISTS (
            SELECT 1 FROM app_users u
            WHERE u.id = app_user_roles.user_id
              AND (
                  u.tenant_id::TEXT = current_setting('app.tenant_id', TRUE)
                  OR current_setting('app.bypass_rls', TRUE) = 'true'
              )
        )
    );

COMMENT ON TABLE app_users IS 'Utilisateurs applicatifs QualitOS — liés à un compte Keycloak';
COMMENT ON COLUMN app_users.keycloak_id IS 'UUID du user dans Keycloak — clé de réconciliation';
COMMENT ON COLUMN app_users.tenant_id IS 'Tenant propriétaire — jamais modifiable après création';
COMMENT ON TABLE app_user_roles IS 'Rôles métier QualitOS de chaque utilisateur dans son tenant';
