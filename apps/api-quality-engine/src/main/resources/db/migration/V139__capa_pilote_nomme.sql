-- Une CAPA = une action, et son pilote est nommé (ADR 0088).
--
-- Le pilote du dossier est désormais le responsable choisi dans l'annuaire du
-- client à l'ouverture, et non plus celui qui a cliqué. Son libellé est
-- recopié ici, comme celui du vérificateur : un compte désactivé ne doit pas
-- effacer le nom de qui pilotait le dossier, et l'écran ne doit jamais
-- retomber sur un identifiant que personne ne reconnaît.
--
-- Nul sur les dossiers antérieurs : on ne reconstitue pas un nom que personne
-- n'a saisi. L'écran le résout alors par l'annuaire, ou affiche « — ».

ALTER TABLE capa_cases ADD COLUMN owner_name VARCHAR(255);
