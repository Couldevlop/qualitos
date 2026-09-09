-- La boîte à idées : une proposition cesse d'appartenir à un cercle.
--
-- Jusqu'ici, une proposition d'amélioration n'existait qu'à l'intérieur d'un
-- cercle de qualité. L'opérateur qui voit un défaut à son poste n'avait nulle
-- part où le dire. On rend donc le cercle facultatif -- mais l'isolation entre
-- clients reposait ENTIÈREMENT sur ce cercle parent, atteint par la clé
-- étrangère. Sans la colonne ci-dessous, une idée sans cercle serait une ligne
-- sans tenant déterminable : les idées de tous les clients dans la même liste.

ALTER TABLE circle_proposals ADD COLUMN tenant_id UUID;

-- La reprise : chaque proposition existante hérite du tenant de son cercle.
UPDATE circle_proposals p
   SET tenant_id = c.tenant_id
  FROM quality_circles c
 WHERE c.id = p.circle_id
   AND p.tenant_id IS NULL;

ALTER TABLE circle_proposals ALTER COLUMN tenant_id SET NOT NULL;

-- Le cercle devient facultatif. C'est la saisie hors cercle.
ALTER TABLE circle_proposals ALTER COLUMN circle_id DROP NOT NULL;

-- Le nom de l'auteur, tel que l'annuaire le connaissait AU DÉPÔT.
--
-- La carte le montre : reconnaître qui propose est ce qui fait vivre une boîte
-- à idées, et un identifiant technique n'y sert à personne. Copié à l'écriture
-- plutôt que résolu à l'affichage, comme le fait déjà la non-conformité
-- (`reporter_name`) : le service qualité ne dépend pas de la disponibilité de
-- l'annuaire pour afficher un tableau, et un départ ne réécrit pas l'histoire.
--
-- Nullable : les propositions antérieures n'ont pas ce nom, et le reconstituer
-- serait l'inventer.
ALTER TABLE circle_proposals ADD COLUMN proposed_by_name VARCHAR(255);

-- Une proposition rattachée à un cercle appartient au tenant de CE cercle.
-- Un CHECK ne saurait pas l'exprimer : il ne peut pas interroger une autre
-- table. D'où un déclencheur, qui reste le filet -- le service refuse plus tôt,
-- avec un message lisible.
CREATE OR REPLACE FUNCTION circle_proposals_tenant_matches_circle()
RETURNS TRIGGER AS $$
DECLARE
    circle_tenant UUID;
BEGIN
    IF NEW.circle_id IS NULL THEN
        RETURN NEW;
    END IF;
    SELECT tenant_id INTO circle_tenant FROM quality_circles WHERE id = NEW.circle_id;
    IF circle_tenant IS DISTINCT FROM NEW.tenant_id THEN
        RAISE EXCEPTION
            'circle_proposals.tenant_id (%) does not match the tenant of circle % (%)',
            NEW.tenant_id, NEW.circle_id, circle_tenant;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_circle_proposals_tenant_matches_circle
    BEFORE INSERT OR UPDATE OF circle_id, tenant_id ON circle_proposals
    FOR EACH ROW EXECUTE FUNCTION circle_proposals_tenant_matches_circle();

-- Le vote.
--
-- La clé primaire EST la règle « une voix par personne et par idée » : la poser
-- en base plutôt que dans le service la rend vraie même pour deux clics
-- simultanés, que le code seul laisserait passer.
--
-- `tenant_id` y figure bien qu'il se déduise de l'idée : la table reste
-- filtrable sans jointure, comme le reste du schéma, et une politique RLS par
-- ligne pourra s'y poser sans reprise.
CREATE TABLE proposal_votes (
    proposal_id UUID        NOT NULL,
    voter_id    UUID        NOT NULL,
    tenant_id   UUID        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_proposal_votes PRIMARY KEY (proposal_id, voter_id),
    CONSTRAINT fk_proposal_votes_proposal FOREIGN KEY (proposal_id)
        REFERENCES circle_proposals (id) ON DELETE CASCADE
);

-- L'écran lit toujours « les idées de CE client, par colonne ».
CREATE INDEX idx_circle_proposals_tenant_status ON circle_proposals (tenant_id, status);
CREATE INDEX idx_proposal_votes_tenant ON proposal_votes (tenant_id);
