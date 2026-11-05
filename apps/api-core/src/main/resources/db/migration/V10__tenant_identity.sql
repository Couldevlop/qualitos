-- =============================================================================
-- V10 : Table tenant_identities — l'identité visuelle et légale d'un client
-- =============================================================================
--
-- POURQUOI UNE TABLE À PART, et non des colonnes de plus sur `tenants`.
--
-- `tenants` porte ce dont la plateforme a besoin pour FONCTIONNER : le slug qui
-- route, le palier qui facture, l'actif qui ouvre ou ferme l'accès. Il est lu à
-- chaque requête, par tous les services. L'identité, elle, est lue une fois par
-- session pour habiller l'écran : logo, slogan, couleurs, mentions légales. Les
-- mélanger ferait voyager quatorze colonnes d'affichage dans le chemin chaud de
-- l'authentification, et ferait d'un changement de slogan une écriture sur la
-- table la plus sollicitée du socle.
--
-- La clé primaire EST le tenant_id : un client a une identité, pas une liste.
--
-- La table porte `tenant_id` : elle entre donc sous la politique de
-- cloisonnement (ADR 0086), seconde barrière derrière le contrôle applicatif.
-- Le super administrateur y accède par le drapeau de contournement, comme pour
-- `tenants` lui-même, et cet accès est journalisé côté applicatif.

CREATE TABLE tenant_identities (
    tenant_id           UUID         NOT NULL,

    -- Marque
    logo_object_key     VARCHAR(512),
    logo_content_type   VARCHAR(64),
    slogan              VARCHAR(160),

    -- Mentions légales : ce qui doit figurer sur un rapport signé ou une facture.
    legal_name          VARCHAR(255),
    legal_form          VARCHAR(64),
    registration_number VARCHAR(64),
    vat_number          VARCHAR(32),
    address_line1       VARCHAR(255),
    address_line2       VARCHAR(255),
    postal_code         VARCHAR(16),
    city                VARCHAR(128),
    country_code        CHAR(2),
    contact_email       VARCHAR(254),
    contact_phone       VARCHAR(32),

    -- Apparence. Les couleurs sont stockées en notation longue (#RRGGBB) : une
    -- seule écriture pour une couleur, sans quoi la comparaison de contraste et
    -- le stockage divergeraient.
    primary_color       CHAR(7),
    secondary_color     CHAR(7),
    theme_code          VARCHAR(20)  NOT NULL DEFAULT 'AIRY_LIGHT',

    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_tenant_identities PRIMARY KEY (tenant_id),
    CONSTRAINT fk_tenant_identities_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE,
    -- Les quatre thèmes livrés. Une valeur hors liste serait un écran sans
    -- feuille de style : la base refuse plutôt que de servir du cassé.
    CONSTRAINT chk_tenant_identities_theme
        CHECK (theme_code IN ('DENSE_LIGHT', 'AIRY_LIGHT', 'DARK', 'HIGH_CONTRAST')),
    CONSTRAINT chk_tenant_identities_primary_color
        CHECK (primary_color IS NULL OR primary_color ~ '^#[0-9a-fA-F]{6}$'),
    CONSTRAINT chk_tenant_identities_secondary_color
        CHECK (secondary_color IS NULL OR secondary_color ~ '^#[0-9a-fA-F]{6}$'),
    CONSTRAINT chk_tenant_identities_country
        CHECK (country_code IS NULL OR country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT chk_tenant_identities_logo_type
        CHECK (logo_content_type IS NULL
               OR logo_content_type IN ('image/png', 'image/jpeg', 'image/webp'))
);

-- Pas d'index supplémentaire : la clé primaire est le seul accès (lecture par
-- tenant_id, une ligne).

CREATE TRIGGER trg_tenant_identities_updated_at
    BEFORE UPDATE ON tenant_identities
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE tenant_identities ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_identities_isolation_policy ON tenant_identities
    FOR ALL
    USING (tenant_id::TEXT = current_setting('app.tenant_id', TRUE)
        OR current_setting('app.bypass_rls', TRUE) = 'true');
