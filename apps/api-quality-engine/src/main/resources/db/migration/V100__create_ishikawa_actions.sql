-- Plan d'actions d'un diagramme Ishikawa (§3.5, §3.6).
--
-- Identifier les causes est un moyen ; décider qui fait quoi et pour quand est la
-- fin. Sans cette table, les décisions prises devant le diagramme vivaient
-- ailleurs — compte rendu, tableur, mémoire — et le diagramme restait un exercice.
--
-- Ce n'est PAS une action CAPA : une CAPA est un dossier formel, avec instruction
-- et preuve d'efficacité. En ouvrir un pour « refaire le réglage de la butée,
-- Karim, vendredi » découragerait la saisie et remplirait le registre de
-- broutilles. L'escalade reste possible : c'est un lien, pas une contrainte.
CREATE TABLE ishikawa_actions (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    -- Tenant porté par la ligne, et pas seulement déduit du diagramme : toute
    -- lecture est ainsi bornée sans jointure, ce qui rend impossible une requête
    -- qui oublierait le cloisonnement.
    tenant_id    UUID         NOT NULL,
    diagram_id   UUID         NOT NULL,
    label        VARCHAR(500) NOT NULL,
    responsible  VARCHAR(255),
    -- Date de la DÉCISION (réunion, comité, revue), et non une échéance : ce qui
    -- est demandé, c'est de savoir depuis quand une décision attend.
    decided_on   DATE,
    status       VARCHAR(20)  NOT NULL DEFAULT 'TODO',
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_ishikawa_actions PRIMARY KEY (id),
    -- Le plan disparaît avec son diagramme : une action orpheline ne veut rien dire.
    CONSTRAINT fk_ishikawa_actions_diagram FOREIGN KEY (diagram_id)
        REFERENCES ishikawa_diagrams (id) ON DELETE CASCADE,
    CONSTRAINT chk_ishikawa_actions_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT chk_ishikawa_actions_label CHECK (length(trim(label)) > 0)
);

-- L'écran liste toujours par (diagramme, tenant), dans l'ordre de décision.
CREATE INDEX idx_ishikawa_actions_diagram
    ON ishikawa_actions (diagram_id, tenant_id, created_at);
