-- « Détecté par » : le nom du signalant, à côté de son identifiant (§4.3).
--
-- La NC portait déjà reporter_id, un UUID Keycloak. Illisible dans une liste, et
-- surtout périssable : le jour où le compte est désactivé ou supprimé de
-- l'annuaire, plus personne ne peut dire qui a vu l'écart. Le nom est donc
-- recopié au moment du signalement, comme capa_actions.assignee_name l'est déjà.
--
-- Nul pour les NC existantes, volontairement : reconstituer un nom depuis un
-- annuaire d'aujourd'hui pour un signalement d'hier produirait une attribution
-- plausible et fausse. Une colonne vide dit ce qu'elle sait.
ALTER TABLE non_conformities
    ADD COLUMN reporter_name VARCHAR(255);
