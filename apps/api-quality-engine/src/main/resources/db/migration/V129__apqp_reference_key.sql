-- APQP : le référentiel garde sa CLÉ, pour que son texte suive la langue choisie.
--
-- Les phases et les livrables sont des données du client (ADR 0066), et une donnée
-- ne se traduit pas : l'écran affichait donc le cycle dans la langue de l'amorçage,
-- quelle que soit la langue de l'interface. Un utilisateur anglophone lisait un V
-- français, et inversement.
--
-- La clé change cela pour la part du cycle qui vient du RÉFÉRENTIEL — la seule
-- qu'on puisse traduire, puisqu'elle est écrite dans le code. Une ligne qui porte
-- sa clé et que personne n'a retouchée est rendue dans la langue demandée ; dès
-- que le client la renomme, c'est SON texte qui gagne, dans SA langue, et la
-- traduction se tait. C'est la même frontière que partout ailleurs : ce que la
-- plateforme fournit se traduit, ce que le client écrit lui appartient.
--
-- Nullable, et sans reprise : une ligne sans clé est du texte de client, qu'on ne
-- traduira jamais. Les cycles déjà amorcés par ce lot n'existent qu'en
-- développement — le référentiel du document n'a pas encore été déployé — et se
-- refont d'un « Réinitialiser depuis le référentiel ».

ALTER TABLE apqp_phases
    ADD COLUMN reference_key VARCHAR(80);

ALTER TABLE apqp_deliverables
    ADD COLUMN reference_key VARCHAR(80);

COMMENT ON COLUMN apqp_phases.reference_key IS
    'Clé du référentiel : la phase est traduite tant qu''elle n''a pas été retouchée.';
COMMENT ON COLUMN apqp_deliverables.reference_key IS
    'Clé du référentiel : le livrable est traduit tant qu''il n''a pas été retouché.';
