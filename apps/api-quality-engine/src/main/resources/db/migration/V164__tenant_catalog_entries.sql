-- Les listes déroulantes du client : une seule table pour les douze listes
-- ouvertes au paramétrage, et pour celles à venir. Douze tables coûteraient
-- douze fois (§5.4, règle 18.2 #9).
--
-- Un client qui n'a rien réglé n'a AUCUNE ligne ici : il travaille avec les
-- valeurs de la plateforme. Aucune reprise de données, aucune régression.
CREATE TABLE tenant_catalog_entries (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL,
    catalogue   VARCHAR(60)  NOT NULL,
    code        VARCHAR(40)  NOT NULL,
    label       VARCHAR(120) NOT NULL,
    position    INTEGER      NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_by  UUID,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_tenant_catalog_entries PRIMARY KEY (id),
    -- Un code ne peut pas désigner deux choses dans la même liste : c'est lui
    -- que portent les écarts, les audits, les fournisseurs.
    CONSTRAINT uk_tenant_catalog_entries UNIQUE (tenant_id, catalogue, code),
    CONSTRAINT ck_tenant_catalog_entries_position CHECK (position >= 0),
    CONSTRAINT ck_tenant_catalog_entries_code CHECK (code = upper(code)),
    CONSTRAINT ck_tenant_catalog_entries_label CHECK (btrim(label) <> '')
);

-- Toute lecture demande une liste d'un client, dans l'ordre d'affichage.
CREATE INDEX ix_tenant_catalog_entries_liste
    ON tenant_catalog_entries (tenant_id, catalogue, position);

COMMENT ON TABLE tenant_catalog_entries IS
    'Listes déroulantes paramétrées par un client ; aucune ligne = valeurs de la plateforme';
COMMENT ON COLUMN tenant_catalog_entries.catalogue IS
    'Nom de la liste (CatalogId.slug) : nc-category, audit-type, fives-zone…';
COMMENT ON COLUMN tenant_catalog_entries.code IS
    'Identité durable portée par les données ; ne change jamais de sens';
COMMENT ON COLUMN tenant_catalog_entries.label IS
    'Le mot du client, dans sa langue ; la plateforme ne le traduit pas';
COMMENT ON COLUMN tenant_catalog_entries.position IS
    'Rang d''affichage à partir de 0 : l''ordre est un choix du client';
COMMENT ON COLUMN tenant_catalog_entries.active IS
    'FALSE pour une valeur retirée des nouvelles saisies et conservée pour l''historique';
