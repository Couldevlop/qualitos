-- Le client qui réclame, le lot incriminé, et la reprise d'un écart refermé.
--
-- Trois données que le traitement 8D d'un écart demande dès sa deuxième
-- discipline (« Quoi, où, quand, combien »), et que la fiche ne savait pas
-- porter : on les écrivait dans la description, en texte libre, d'où elles ne
-- ressortaient ni en filtre, ni en indicateur, ni dans le rapport remis au
-- client.
--
-- `customer_name` et non un lien vers un référentiel client : la plateforme n'en
-- a pas, et une réclamation arrive souvent avec le seul nom que le client a
-- donné au téléphone. Le jour où un référentiel existera, cette colonne sera ce
-- qu'on y rattachera -- la perdre maintenant coûterait la donnée elle-même.
--
-- `batch_number` en texte : un numéro de lot est une référence d'atelier
-- (« L2611-08 »), pas un nombre. L'arithmétique n'a aucun sens dessus, et son
-- format change d'une ligne de production à l'autre.
ALTER TABLE non_conformities
    ADD COLUMN customer_name VARCHAR(255),
    ADD COLUMN batch_number VARCHAR(100);

-- La REPRISE d'un écart refermé : récidive constatée après clôture, réclamation
-- que le client relance, constat écarté à tort.
--
-- Le motif est séparé de `rejection_reason` et de `resolution_note` : ceux-là
-- disent pourquoi le dossier s'est FERMÉ, celui-ci dit pourquoi il se ROUVRE.
-- Les confondre effacerait la raison lue par le client au moment de la clôture.
--
-- Rien n'est remis à zéro à la réouverture : `resolved_at`, `closed_at` et
-- `rejected_at` restent ce qu'ils furent. Ce sont des faits datés, et le statut
-- suffit à dire où en est l'écart aujourd'hui.
ALTER TABLE non_conformities
    ADD COLUMN reopened_at TIMESTAMPTZ,
    ADD COLUMN reopen_reason TEXT;

-- Les écarts rouverts se comptent (taux de récidive, §6.2) et se retrouvent :
-- partiel, parce que seule une petite part des écarts est concernée et qu'un
-- index plein paierait la colonne vide sur tous les autres.
CREATE INDEX idx_non_conformities_reopened
    ON non_conformities (tenant_id, reopened_at)
    WHERE reopened_at IS NOT NULL;
