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

    -- Marque. Le logo lui-même vit dans `tenant_logos` (plus bas) : ses
    -- octets n'ont pas à voyager avec le slogan et les couleurs, lus eux à
    -- chaque ouverture d'écran.
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
        CHECK (country_code IS NULL OR country_code ~ '^[A-Z]{2}$')
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


-- =============================================================================
-- Table tenant_logos — les octets du logo d'un client
-- =============================================================================
--
-- POURQUOI EN BASE, et non dans le stockage d'objets.
--
-- api-core n'a aucun port de stockage d'objets : celui de la GED vit dans
-- api-quality-engine, avec sa configuration MinIO, son balayage des orphelins
-- et son mode désactivé. L'y brancher pour UN fichier par client ferait
-- dépendre l'ouverture de n'importe quel écran — le logo s'affiche partout —
-- de la disponibilité d'un service tiers, et ferait de la console de l'éditeur
-- un client de plus du stockage de la qualité.
--
-- Un logo est borné à 512 Kio par la politique de téléversement (LogoPolicy),
-- il y en a UN par client, et il est relu une fois par session puis mis en
-- cache par le navigateur. Le coût en base est donc une ligne de quelques
-- centaines de kilo-octets par client, jamais balayée par une requête
-- métier — c'est moins qu'un seul document de la GED.
--
-- Si un jour la plateforme sert des médias de client en nombre, c'est le
-- stockage d'objets qu'il faudra, et une migration qui déplace ces lignes.
-- Pas avant : ce serait de la dépendance payée d'avance.

CREATE TABLE tenant_logos (
    tenant_id    UUID         NOT NULL,
    content_type VARCHAR(32)  NOT NULL,
    size_bytes   INTEGER      NOT NULL,
    -- Empreinte du contenu : permet de répondre 304 sans relire les octets, et
    -- de reconnaître un téléversement identique.
    sha256       CHAR(64)     NOT NULL,
    content      BYTEA        NOT NULL,

    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_tenant_logos PRIMARY KEY (tenant_id),
    CONSTRAINT fk_tenant_logos_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE,
    -- Les trois formats acceptés. Le SVG n'y est pas : il porte <script> et
    -- serait servi depuis le domaine applicatif.
    CONSTRAINT chk_tenant_logos_type
        CHECK (content_type IN ('image/png', 'image/jpeg', 'image/webp')),
    -- La même borne que LogoPolicy.MAX_BYTES, tenue aussi par la base : un
    -- contrôle applicatif seul se contourne par une autre route.
    CONSTRAINT chk_tenant_logos_size
        CHECK (size_bytes > 0 AND size_bytes <= 524288),
    CONSTRAINT chk_tenant_logos_sha256
        CHECK (sha256 ~ '^[0-9a-f]{64}$')
);

CREATE TRIGGER trg_tenant_logos_updated_at
    BEFORE UPDATE ON tenant_logos
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE tenant_logos ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_logos_isolation_policy ON tenant_logos
    FOR ALL
    USING (tenant_id::TEXT = current_setting('app.tenant_id', TRUE)
        OR current_setting('app.bypass_rls', TRUE) = 'true');
