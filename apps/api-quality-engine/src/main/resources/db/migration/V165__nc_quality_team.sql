-- L'équipe qui prend l'écart en charge, nommée à l'ouverture de la fiche.
--
-- C'est la première discipline du 8D (D1 « Constituer l'équipe ») : jusqu'ici,
-- elle se retapait à la main dans le rapport, alors que la fiche sait qui s'en
-- occupe depuis le premier jour. Une discipline qu'on resaisit est une
-- discipline qu'on oublie, et un 8D dont le D1 est vide se discute mal en revue.
--
-- Table fille plutôt qu'une colonne de texte : on veut pouvoir demander « les
-- écarts où untel est dans l'équipe », et un nom caché au milieu d'une chaîne
-- séparée par des virgules ne se cherche pas. L'ordre de saisie est conservé —
-- le premier nommé est, par convention d'atelier, celui qui pilote.
--
-- Deux colonnes pour une personne, comme pour le signalant de la fiche :
-- l'identifiant de compte, qui ne change pas, et le nom figé au moment de la
-- saisie. Le nom est une PHOTOGRAPHIE : un rapport 8D relu trois ans plus tard
-- doit porter les noms de l'époque, pas ceux d'un annuaire qui a bougé.
CREATE TABLE nc_team_members (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id    UUID         NOT NULL,
    nc_id        UUID         NOT NULL,
    account_id   VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    position     INTEGER      NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_nc_team_members PRIMARY KEY (id),
    CONSTRAINT fk_nc_team_members_nc FOREIGN KEY (nc_id)
        REFERENCES non_conformities (id) ON DELETE CASCADE,
    -- Une personne ne figure qu'une fois dans une équipe : deux lignes
    -- identiques gonfleraient le D1 sans rien y ajouter.
    CONSTRAINT uq_nc_team_members_nc_account UNIQUE (nc_id, account_id)
);

-- La fiche charge son équipe à chaque ouverture, toujours par (client, écart).
CREATE INDEX idx_nc_team_members_tenant_nc ON nc_team_members (tenant_id, nc_id);

-- « Sur quels écarts suis-je sollicité ? » : la question se pose depuis
-- l'annuaire, donc par compte, à l'intérieur du client.
CREATE INDEX idx_nc_team_members_tenant_account ON nc_team_members (tenant_id, account_id);
