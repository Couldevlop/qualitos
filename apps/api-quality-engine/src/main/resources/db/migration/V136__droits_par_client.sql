-- Les droits par client (ADR 0078).
--
-- Le catalogue des actions vit dans le code (Permission) ; la base ne garde que
-- ce que le client a CHANGÉ : les rôles système dont il a réglé les droits, les
-- rôles qu'il a créés, et les rôles qu'il a attribués à ses membres en plus de
-- ceux de leur compte. Un client qui n'a rien réglé n'a aucune ligne ici et
-- reçoit les droits livrés par la plateforme.

-- ---------------------------------------------------------------------------
-- 1. Les rôles réglés ou créés
-- ---------------------------------------------------------------------------

CREATE TABLE authz_roles (
    id           UUID PRIMARY KEY,
    tenant_id    UUID          NOT NULL,
    code         VARCHAR(64)   NOT NULL,
    name         VARCHAR(120),
    description  VARCHAR(500),
    -- Vrai pour un rôle livré par la plateforme (ADMIN_TENANT, QUALITY_MANAGER…).
    system_role  BOOLEAN       NOT NULL,
    updated_by   UUID          NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL,
    updated_at   TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_authz_roles_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT chk_authz_roles_code CHECK (code ~ '^[A-Z][A-Z0-9_]{1,63}$'),
    -- Le super administrateur appartient à l'éditeur : aucun client ne le règle.
    CONSTRAINT chk_authz_roles_not_super_admin CHECK (code <> 'SUPER_ADMIN'),
    -- Un rôle créé par le client porte toujours un nom.
    CONSTRAINT chk_authz_roles_custom_named CHECK (system_role OR name IS NOT NULL)
);

CREATE TABLE authz_role_permissions (
    role_id     UUID         NOT NULL REFERENCES authz_roles (id) ON DELETE CASCADE,
    permission  VARCHAR(80)  NOT NULL,
    PRIMARY KEY (role_id, permission),
    CONSTRAINT chk_authz_role_permissions_code CHECK (permission ~ '^[a-z][a-z0-9.]{1,79}$')
);

-- ---------------------------------------------------------------------------
-- 2. Les rôles attribués dans l'application
-- ---------------------------------------------------------------------------
--
-- Par CODE et non par clé étrangère : un rôle système encore aux droits livrés
-- n'a pas de ligne dans authz_roles, et s'attribue quand même.

CREATE TABLE authz_member_roles (
    tenant_id    UUID          NOT NULL,
    user_id      UUID          NOT NULL,
    role_code    VARCHAR(64)   NOT NULL,
    assigned_by  UUID          NOT NULL,
    assigned_at  TIMESTAMPTZ   NOT NULL,
    PRIMARY KEY (tenant_id, user_id, role_code),
    CONSTRAINT chk_authz_member_roles_code CHECK (role_code ~ '^[A-Z][A-Z0-9_]{1,63}$'),
    CONSTRAINT chk_authz_member_roles_not_super_admin CHECK (role_code <> 'SUPER_ADMIN')
);

CREATE INDEX ix_authz_member_roles_role ON authz_member_roles (tenant_id, role_code);
