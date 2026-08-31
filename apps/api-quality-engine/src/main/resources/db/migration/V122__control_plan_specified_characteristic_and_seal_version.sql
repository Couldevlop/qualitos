-- Deux manques constatés en confrontant le control plan livré à la trame réelle
-- (classeur de référence, feuille 3), ligne à ligne et colonne à colonne.

-- 1. La caractéristique QUI PORTE LA SPÉCIFICATION.
--
-- La trame tient deux colonnes là où le modèle n'en avait qu'une : « what's
-- controlled » (ce qu'on surveille : la longueur du fil, l'état de l'isolant)
-- et « specification characteristic » (la grandeur spécifiée : la cote de
-- coupe, la hauteur de sertissage et l'effort d'arrachement).
--
-- Les confondre fait disparaître la grandeur réellement mesurée — celle qui
-- porte la tolérance et sur laquelle le contrôle statue. L'opérateur lit alors
-- « crimp height / crimp integrity » et doit deviner ce qu'il mesure.
--
-- Facultative : un control plan se remplit par passes successives, et exiger
-- cette colonne empêcherait simplement d'ouvrir une ligne.
ALTER TABLE control_plan_lines
    ADD COLUMN specified_characteristic VARCHAR(500);

-- 2. La version du calcul d'empreinte employée au scellement.
--
-- L'empreinte d'un plan approuvé se voulait celle de « chaque ligne dans son
-- intégralité ». Elle ne l'était plus : les cinq colonnes arrivées avec la V116
-- et celle ci-dessus n'y entraient pas. On pouvait donc déplacer le lieu
-- d'enregistrement d'un contrôle — la colonne même que l'auditeur suit pour
-- retrouver la preuve — sans que l'empreinte du document ne bouge.
--
-- Compléter le calcul sans le versionner aurait rendu INVÉRIFIABLES tous les
-- plans déjà scellés : rejouer le hachage d'un document ancien avec le nouveau
-- calcul donne une autre valeur, c'est-à-dire un verdict de falsification sur
-- un document intact.
--
-- DÉFAUT 0 : « pas encore scellé ». Les plans déjà scellés reçoivent 1 juste
-- après — la seule version qui existait quand ils l'ont été. On ne recalcule
-- aucune empreinte : une preuve passée ne se réécrit pas.
ALTER TABLE control_plans
    ADD COLUMN seal_version SMALLINT NOT NULL DEFAULT 0;

UPDATE control_plans
   SET seal_version = 1
 WHERE seal_sha256 IS NOT NULL;
