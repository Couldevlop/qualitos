-- =============================================================================
-- Migration V14 — La messagerie d'une organisation (ADR 0127)
-- =============================================================================
--
-- Jusqu'ici, la plateforme n'avait qu'UNE messagerie, celle du déploiement,
-- réglée par variables d'environnement. Tout partait d'elle : les factures, les
-- rappels d'audit. Un client recevait donc ses propres relances depuis une
-- adresse qui n'est pas la sienne, qui ne passe pas ses règles SPF/DKIM, et que
-- ses destinataires ne reconnaissent pas.
--
-- Chaque organisation règle désormais la sienne.
--
-- POURQUOI DEUX TABLES ET NON UNE.
--
-- Un client et un cabinet n'ont pas la même règle de cloisonnement, et c'est
-- une différence de nature, pas de forme :
--
--   * la messagerie d'un CLIENT est une donnée de client, isolée par RLS
--     (`tenant_id` = le client du jeton) ;
--   * celle d'un CABINET est une donnée de plateforme, gardée côté applicatif
--     par le rôle, comme `partner_firms` (V11) — un cabinet n'a pas de
--     `tenant_id`, il en sert plusieurs.
--
-- Une table unique aurait demandé un `tenant_id` nullable, et une ligne à
-- `tenant_id` NULL échappe à la politique RLS : selon l'écriture de la
-- politique, elle devient invisible à tous ou visible à tous. Ni l'un ni
-- l'autre n'est acceptable pour un mot de passe. On préfère deux tables dont
-- chacune a une règle qu'on peut énoncer en une phrase.
--
-- LE SECRET EST CHIFFRÉ, PAS HACHÉ.
--
-- C'est le premier secret relisible de la plateforme. Les mots de passe des
-- comptes et les clés d'API sont HACHÉS : on ne les relit jamais, on compare
-- une empreinte. Un mot de passe SMTP doit être présenté tel quel au serveur de
-- messagerie — il faut pouvoir le retrouver.
--
-- La colonne ne contient donc jamais de clair : elle porte un cryptogramme
-- AES-256-GCM versionné (`v1.<nonce>.<chiffré+étiquette>`), dont la clé vit
-- dans l'environnement et JAMAIS en base. Une copie de la base volée sans la
-- clé ne donne rien.
-- =============================================================================


-- =============================================================================
-- La messagerie d'un client
-- =============================================================================

CREATE TABLE tenant_mail_settings (
    tenant_id           UUID         NOT NULL,

    -- L'expéditeur, tel que le destinataire le lit.
    from_address        VARCHAR(320) NOT NULL,
    from_name           VARCHAR(120),
    -- Là où les réponses arrivent. Souvent différent de l'expéditeur : on émet
    -- depuis « qualite@ » et l'on veut les réponses dans une boîte partagée.
    reply_to            VARCHAR(320),

    -- Le serveur sortant.
    host                VARCHAR(255) NOT NULL,
    port                INTEGER      NOT NULL,
    -- NONE, STARTTLS ou TLS. Stocké en texte et contraint : un entier
    -- demanderait d'ouvrir le code pour relire une ligne.
    security            VARCHAR(16)  NOT NULL,
    username            VARCHAR(320),

    -- Le mot de passe, CHIFFRÉ. Jamais de clair ici, jamais renvoyé par l'API.
    -- NULL est permis : un relais interne authentifie parfois par adresse IP.
    password_cipher     TEXT,

    -- Une messagerie se règle d'abord, et ne sert que lorsqu'on l'active. Sans
    -- cet interrupteur, enregistrer un réglage incomplet détournerait aussitôt
    -- tout le courrier du client vers un serveur qui le refuse.
    enabled             BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Dernier envoi de test CONCLUANT. C'est une preuve, pas un réglage : elle
    -- est remise à NULL dès que le serveur, le compte ou le mot de passe
    -- changent, parce qu'un essai réussi ne dit plus rien sur une adresse qu'on
    -- vient de modifier.
    verified_at         TIMESTAMPTZ,

    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    -- Une organisation, une messagerie. La clé primaire le dit, plutôt qu'un
    -- index unique qu'on pourrait oublier d'ajouter.
    CONSTRAINT pk_tenant_mail_settings PRIMARY KEY (tenant_id),
    CONSTRAINT fk_tenant_mail_settings_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE,
    CONSTRAINT chk_tenant_mail_settings_security
        CHECK (security IN ('NONE', 'STARTTLS', 'TLS')),
    CONSTRAINT chk_tenant_mail_settings_port
        CHECK (port BETWEEN 1 AND 65535),
    -- Un courriel d'expédition sans arobase ne part pas : le refuser ici évite
    -- qu'une reprise de données le sème là où l'application ne passe pas.
    CONSTRAINT chk_tenant_mail_settings_from
        CHECK (POSITION('@' IN from_address) > 1),
    -- Le secret ne se stocke JAMAIS en clair : le cryptogramme porte sa
    -- version. Cette contrainte attrape l'erreur d'écriture directe en base,
    -- celle qu'aucune revue de code ne verrait.
    CONSTRAINT chk_tenant_mail_settings_cipher
        CHECK (password_cipher IS NULL OR password_cipher LIKE 'v%.%.%')
);

COMMENT ON COLUMN tenant_mail_settings.password_cipher IS
    'Mot de passe SMTP chiffre (AES-256-GCM versionne). Jamais de clair, jamais renvoye par API.';
COMMENT ON COLUMN tenant_mail_settings.verified_at IS
    'Dernier envoi de test concluant. Remis a NULL des que le serveur ou le compte change.';

ALTER TABLE tenant_mail_settings ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_mail_settings_isolation_policy ON tenant_mail_settings
    FOR ALL
    USING (tenant_id::TEXT = current_setting('app.tenant_id', TRUE)
        OR current_setting('app.bypass_rls', TRUE) = 'true');


-- =============================================================================
-- La messagerie d'un cabinet partenaire
-- =============================================================================
--
-- Mêmes colonnes, autre règle d'accès : donnée de plateforme, gardée par le
-- rôle côté applicatif, comme `partner_firms` (V11). Un cabinet sert plusieurs
-- clients ; il n'a pas de `tenant_id`, et sa messagerie non plus.
--
-- Ce qu'un cabinet ne peut PAS faire : régler celle de ses clients. Il peut la
-- demander, pas l'écrire — chaque organisation reste maîtresse de l'adresse
-- d'où partent les courriels qui l'engagent.
-- =============================================================================

CREATE TABLE partner_firm_mail_settings (
    firm_id             UUID         NOT NULL,

    from_address        VARCHAR(320) NOT NULL,
    from_name           VARCHAR(120),
    reply_to            VARCHAR(320),

    host                VARCHAR(255) NOT NULL,
    port                INTEGER      NOT NULL,
    security            VARCHAR(16)  NOT NULL,
    username            VARCHAR(320),
    password_cipher     TEXT,

    enabled             BOOLEAN      NOT NULL DEFAULT FALSE,
    verified_at         TIMESTAMPTZ,

    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_partner_firm_mail_settings PRIMARY KEY (firm_id),
    CONSTRAINT fk_partner_firm_mail_settings_firm
        FOREIGN KEY (firm_id) REFERENCES partner_firms (id) ON DELETE CASCADE,
    CONSTRAINT chk_partner_firm_mail_settings_security
        CHECK (security IN ('NONE', 'STARTTLS', 'TLS')),
    CONSTRAINT chk_partner_firm_mail_settings_port
        CHECK (port BETWEEN 1 AND 65535),
    CONSTRAINT chk_partner_firm_mail_settings_from
        CHECK (POSITION('@' IN from_address) > 1),
    CONSTRAINT chk_partner_firm_mail_settings_cipher
        CHECK (password_cipher IS NULL OR password_cipher LIKE 'v%.%.%')
);

COMMENT ON COLUMN partner_firm_mail_settings.password_cipher IS
    'Mot de passe SMTP chiffre (AES-256-GCM versionne). Jamais de clair, jamais renvoye par API.';
