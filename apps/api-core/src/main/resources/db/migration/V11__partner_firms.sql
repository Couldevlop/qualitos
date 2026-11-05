-- =============================================================================
-- V11 : Les cabinets, leurs consultants, et l'accord par lequel un client les
--       laisse entrer chez lui
-- =============================================================================
--
-- POURQUOI TROIS TABLES, et pas un rôle de plus sur les utilisateurs du client.
--
-- Un consultant n'est pas un membre du client. Il appartient à un cabinet, il
-- accompagne plusieurs clients à la fois, et son départ du cabinet doit lui
-- retirer d'un coup l'accès à tous. En faire un utilisateur de chaque client
-- obligerait à le créer autant de fois qu'il a de missions, à le retirer autant
-- de fois, et ferait dépendre la fin de sa mission de la vigilance de chaque
-- administrateur de client pris séparément — c'est-à-dire d'un oubli.
--
-- LA RÈGLE QUE CES TABLES EXISTENT POUR TENIR : un consultant n'entre jamais
-- chez un client sans accord enregistré DE CE CLIENT. L'éditeur constitue le
-- portefeuille ; il ne donne pas l'accès. Seul le client le donne, borné dans le
-- temps et révocable à tout instant.
--
-- RLS. `partner_firms` et `partner_consultants` ne portent pas de `tenant_id` :
-- ce sont des données de plateforme, comme `module_prices` (V5), et elles sont
-- gardées côté applicatif par le rôle. `partner_engagements`, lui, désigne un
-- client : il entre donc sous la politique de cloisonnement (ADR 0086), et un
-- administrateur de client ne peut lire que les accords qui le concernent —
-- jamais la liste des autres clients d'un cabinet, qui est l'information
-- commerciale du cabinet.

-- =============================================================================
-- partner_firms — le cabinet
-- =============================================================================

CREATE TABLE partner_firms (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    slug                VARCHAR(63)  NOT NULL,
    name                VARCHAR(255) NOT NULL,

    -- De quoi identifier le cabinet sur un accord et le joindre. Le minimum :
    -- ce n'est pas un client, il n'a ni facturation ni thème ici.
    registration_number VARCHAR(64),
    contact_email       VARCHAR(254),
    contact_phone       VARCHAR(32),

    -- Suspendre un cabinet coupe l'accès de TOUS ses consultants, sans toucher
    -- aux accords : le jour où il est réactivé, les missions en cours
    -- reprennent telles qu'elles étaient. Supprimer les accords à la suspension
    -- ferait d'une mesure conservatoire une décision irréversible.
    active              BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_partner_firms PRIMARY KEY (id),
    CONSTRAINT uq_partner_firms_slug UNIQUE (slug),
    CONSTRAINT chk_partner_firms_slug
        CHECK (slug ~ '^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$'),
    CONSTRAINT chk_partner_firms_email
        CHECK (contact_email IS NULL OR contact_email LIKE '%@%')
);

CREATE INDEX idx_partner_firms_active ON partner_firms (active) WHERE active = TRUE;

CREATE TRIGGER trg_partner_firms_updated_at
    BEFORE UPDATE ON partner_firms
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE partner_firms IS
    'Cabinets partenaires : ils accompagnent des clients, ils n''en sont pas un';

-- =============================================================================
-- partner_consultants — les personnes du cabinet
-- =============================================================================
--
-- `account_id` est le `sub` du jeton, comme partout ailleurs (ADR 0091) : c'est
-- lui qui relie la personne connectée à sa fiche, jamais l'adresse de courriel,
-- qui change.

CREATE TABLE partner_consultants (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    firm_id       UUID         NOT NULL,

    -- Nul tant que la personne ne s'est pas connectée une première fois : on
    -- inscrit un consultant au cabinet avant qu'il ait un compte, sans quoi il
    -- faudrait créer le compte d'abord et le rattacher ensuite — deux gestes
    -- dans deux outils pour une seule arrivée.
    account_id    UUID,
    display_name  VARCHAR(255) NOT NULL,
    email         VARCHAR(254) NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_partner_consultants PRIMARY KEY (id),
    CONSTRAINT fk_partner_consultants_firm
        FOREIGN KEY (firm_id) REFERENCES partner_firms (id) ON DELETE CASCADE,
    -- Une personne appartient à UN cabinet : deux cabinets sur le même compte
    -- rendraient indécidable au nom de qui elle entre chez un client.
    CONSTRAINT uq_partner_consultants_account UNIQUE (account_id),
    CONSTRAINT uq_partner_consultants_email UNIQUE (firm_id, email),
    CONSTRAINT chk_partner_consultants_email CHECK (email LIKE '%@%')
);

CREATE INDEX idx_partner_consultants_firm ON partner_consultants (firm_id);

CREATE TRIGGER trg_partner_consultants_updated_at
    BEFORE UPDATE ON partner_consultants
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- partner_engagements — l'accord du client, borné et révocable
-- =============================================================================
--
-- C'est la pièce qui porte la règle. Un accord dit QUI (le cabinet), CHEZ QUI
-- (le client), POUR QUOI FAIRE (lire, ou intervenir), JUSQU'À QUAND, et il
-- garde QUI L'A DONNÉ et QUI L'A REPRIS.
--
-- Pourquoi l'accord porte sur le CABINET et non sur chaque consultant : le
-- client contracte avec un cabinet, pas avec une personne, et le cabinet change
-- de consultant en cours de mission sans que le client ait à re-signer. La
-- personne est tracée à l'usage — chaque accès est journalisé nominativement —
-- mais le droit, lui, est celui du cabinet.
--
-- Pourquoi un statut ET des dates : un accord expiré n'est pas un accord
-- révoqué. Le premier se renouvelle, le second a été retiré, et un rapport
-- d'audit doit pouvoir distinguer les deux dix-huit mois plus tard.

CREATE TABLE partner_engagements (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id      UUID         NOT NULL,
    firm_id        UUID         NOT NULL,

    -- READ : le cabinet consulte. INTERVENE : il écrit, dans les limites de ses
    -- droits métier, qui restent ceux d'un utilisateur du client.
    scope          VARCHAR(16)  NOT NULL,

    -- PROPOSED : l'éditeur a constitué le portefeuille, le client n'a pas
    -- encore répondu — aucun accès. GRANTED : le client a accordé.
    -- REVOKED : il a repris. DECLINED : il a refusé d'emblée.
    status         VARCHAR(16)  NOT NULL DEFAULT 'PROPOSED',

    -- Bornes de la mission. `ends_on` est OBLIGATOIRE quand l'accord est
    -- accordé : un accès sans fin se transforme en accès oublié.
    starts_on      DATE,
    ends_on        DATE,

    proposed_by    UUID,
    proposed_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    decided_by     UUID,
    decided_at     TIMESTAMPTZ,
    revoked_by     UUID,
    revoked_at     TIMESTAMPTZ,

    -- L'objet de la mission, dit par le client. Il sert à l'audit : « pourquoi
    -- ce cabinet a-t-il eu accès à nos écarts en mars ? ».
    purpose        VARCHAR(500),

    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_partner_engagements PRIMARY KEY (id),
    CONSTRAINT fk_partner_engagements_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE,
    CONSTRAINT fk_partner_engagements_firm
        FOREIGN KEY (firm_id) REFERENCES partner_firms (id) ON DELETE CASCADE,
    CONSTRAINT chk_partner_engagements_scope
        CHECK (scope IN ('READ', 'INTERVENE')),
    CONSTRAINT chk_partner_engagements_status
        CHECK (status IN ('PROPOSED', 'GRANTED', 'DECLINED', 'REVOKED')),
    -- Un accord accordé est daté des deux bouts : sans quoi « jusqu'à quand »
    -- n'aurait pas de réponse, et la révocation deviendrait le seul moyen de
    -- clore une mission.
    CONSTRAINT chk_partner_engagements_granted_dates
        CHECK (status <> 'GRANTED' OR (starts_on IS NOT NULL AND ends_on IS NOT NULL)),
    CONSTRAINT chk_partner_engagements_period
        CHECK (starts_on IS NULL OR ends_on IS NULL OR ends_on >= starts_on)
);

-- Un seul accord vivant par couple client/cabinet : deux accords accordés en
-- même temps rendraient indécidable lequel borne l'accès. L'historique est
-- gardé — les accords révoqués ou refusés ne sont pas contraints.
CREATE UNIQUE INDEX uq_partner_engagements_vivant
    ON partner_engagements (tenant_id, firm_id)
    WHERE status IN ('PROPOSED', 'GRANTED');

CREATE INDEX idx_partner_engagements_firm ON partner_engagements (firm_id, status);
CREATE INDEX idx_partner_engagements_tenant ON partner_engagements (tenant_id, status);

CREATE TRIGGER trg_partner_engagements_updated_at
    BEFORE UPDATE ON partner_engagements
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE partner_engagements ENABLE ROW LEVEL SECURITY;

CREATE POLICY partner_engagements_isolation_policy ON partner_engagements
    FOR ALL
    USING (tenant_id::TEXT = current_setting('app.tenant_id', TRUE)
        OR current_setting('app.bypass_rls', TRUE) = 'true');

COMMENT ON TABLE partner_engagements IS
    'Accord par lequel un client laisse un cabinet entrer : borné, daté, révocable';
