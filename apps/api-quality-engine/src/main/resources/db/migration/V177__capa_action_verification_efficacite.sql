-- CHAQUE ACTION PORTE SA PROPRE EVALUATION D'EFFICACITE.
--
-- Jusqu'ici, le critere et la decision de verification etaient ceux du DOSSIER,
-- poses une seule fois a l'ouverture (ADR 0073, ADR 0108). Un dossier porte
-- pourtant plusieurs actions de natures differentes (ADR 0115) : « recalibrer
-- la presse » et « former les operateurs de nuit » ne se mesurent pas a la meme
-- chose, et ne se verifient pas forcement par la meme personne. La question
-- n'etait posee qu'une fois, de sorte que la deuxieme action et les suivantes
-- s'ecrivaient sans qu'on sache jamais si leur effet serait verifie.
--
-- LES DEUX COEXISTENT, et c'est voulu : le dossier garde les siens, qui restent
-- ce sur quoi s'appuie sa CLOTURE (ADR 0073) et son verdict d'efficacite
-- (ADR 0108). Les colonnes ci-dessous disent, action par action, ce qu'on
-- attend de CELLE-LA. Voir l'ADR 0131 pour le partage des roles entre les deux.
--
-- Toutes nullables, sans valeur par defaut : `NULL` veut dire « la question n'a
-- pas ete tranchee pour cette action », ce qui est l'etat exact de toutes les
-- lignes existantes. Un `DEFAULT false` aurait affirme que des milliers
-- d'actions ont ete deliberement jugees non verifiables, ce que personne n'a
-- decide.
ALTER TABLE capa_actions
    ADD COLUMN IF NOT EXISTS effectiveness_criterion    TEXT,
    ADD COLUMN IF NOT EXISTS verification_required      BOOLEAN,
    ADD COLUMN IF NOT EXISTS verification_assignee_id   UUID,
    ADD COLUMN IF NOT EXISTS verification_assignee_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS verification_instructions  TEXT;

COMMENT ON COLUMN capa_actions.effectiveness_criterion IS
    'A quoi on reconnaitra que CETTE action a produit son effet ; pose a l''ecriture de l''action, verrouille ensuite (ADR 0108).';
COMMENT ON COLUMN capa_actions.verification_required IS
    'L''effet de cette action sera-t-il verifie ? NULL = question non tranchee pour cette action.';
COMMENT ON COLUMN capa_actions.verification_assignee_id IS
    'Compte de connexion (sub) de la personne qui verifiera l''effet de cette action.';
COMMENT ON COLUMN capa_actions.verification_assignee_name IS
    'Nom fige du verificateur au moment de la decision ; jamais resolu a l''affichage (ADR 0052).';
COMMENT ON COLUMN capa_actions.verification_instructions IS
    'Ce qu''il y a a verifier sur cette action, en mots.';
