-- Une personne désignée par un dossier est un utilisateur de l'annuaire (ADR 0092).
--
-- Le propriétaire d'un risque ou d'une opportunité, le responsable d'un livrable
-- APQP, d'une écriture de coût de la qualité ou d'une action Ishikawa étaient
-- tapés en clair : deux orthographes devenaient deux personnes, et rien ne
-- reliait le nom à un compte qu'on peut relancer.
--
-- Chacun gagne le COMPTE de connexion de la personne (`sub` du jeton, l'identité
-- de référence du moteur), choisi dans l'annuaire. La colonne texte existante
-- reste, et devient le nom recopié au moment du choix : un compte désactivé
-- n'efface pas qui portait le sujet, et les anciennes saisies restent lisibles
-- telles quelles. Nul sur les lignes antérieures : on ne devine pas un compte à
-- partir d'un nom.

ALTER TABLE risk_register_risks         ADD COLUMN owner_user_id       UUID;
ALTER TABLE risk_register_opportunities ADD COLUMN owner_user_id       UUID;
ALTER TABLE apqp_deliverables           ADD COLUMN owner_user_id       UUID;
ALTER TABLE coq_entries                 ADD COLUMN responsible_user_id UUID;
ALTER TABLE ishikawa_actions            ADD COLUMN responsible_user_id UUID;

