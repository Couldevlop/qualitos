-- Les colonnes de la trame AIAG que le premier lot n'avait pas reprises.
--
-- Elles viennent d'un control plan réel — faisceau électrique aéronautique —
-- confronté ligne à ligne au modèle livré. Cinq écarts, dont un qui n'était pas
-- un oubli mais une erreur de type.

-- 1. La taille d'échantillon n'est PAS un nombre.
--
-- « 100 % (automatisé) », « 5 pièces au réglage puis 1 sur 50 », « 1 essai
-- destructif par lot » : ce sont des tailles d'échantillon parfaitement
-- valides, et aucune ne tient dans un entier. La colonne était typée INTEGER,
-- ce qui obligeait soit à tronquer la règle, soit à l'écrire dans un autre
-- champ où personne ne la chercherait.
--
-- La conversion préserve les valeurs déjà saisies : un 5 devient « 5 ».
ALTER TABLE control_plan_lines
    ALTER COLUMN sample_size TYPE VARCHAR(120) USING sample_size::text;

-- 2. La référence de procédure (colonne « SOP # »).
--
-- Texte libre et non lien vers le référentiel de procédures internes : un
-- control plan cite couramment des procédures antérieures à la plateforme, et
-- exiger qu'elles y soient d'abord saisies bloquerait la mise en service. Le
-- jour où le référentiel sera complet, ce champ pourra devenir une clé
-- étrangère sans perte — les références sont déjà là.
ALTER TABLE control_plan_lines
    ADD COLUMN sop_reference VARCHAR(64);

-- 3. Entrée ou sortie surveillée.
--
-- Axe distinct de « caractéristique produit / procédé », qui existe déjà.
-- Contrôler une sortie constate un défaut déjà fait ; contrôler une entrée
-- l'empêche. Un plan qui ne surveille que des sorties trie, il ne maîtrise pas,
-- et la colonne rend ce déséquilibre visible.
ALTER TABLE control_plan_lines
    ADD COLUMN input_output VARCHAR(10);

-- 4. Qui — ou quoi — mesure.
--
-- « Opérateur de ligne / capteur automatisé » : la trame accepte une personne
-- comme une machine, et c'est voulu. Séparer les deux obligerait à trancher au
-- moment de la saisie une question qui ne se pose pas au poste.
ALTER TABLE control_plan_lines
    ADD COLUMN who_measures VARCHAR(250);

-- 5. Où l'enregistrement est conservé.
--
-- C'est la colonne que l'auditeur suit : elle dit où aller chercher la preuve
-- que le contrôle a bien eu lieu. Sans elle, le plan décrit une intention.
ALTER TABLE control_plan_lines
    ADD COLUMN recording_location VARCHAR(250);

-- EXPLOITATION — ce que cette migration coûte et ce qu'elle interdit.
--
-- Le changement de type prend un verrou exclusif sur `control_plan_lines` le
-- temps de réécrire la colonne. Sur le volume attendu — quelques milliers de
-- lignes par tenant — c'est de l'ordre de la seconde ; sur un tenant très
-- chargé, le prévoir hors des heures de production.
--
-- Le RETOUR ARRIÈRE n'est pas symétrique : une fois qu'une taille d'échantillon
-- vaut « 100 % (automatisé) », elle ne redevient pas un entier. Revenir en
-- arrière suppose donc de restaurer la sauvegarde prise avant le déploiement,
-- pas de rejouer une migration inverse. C'est le cas prévu par le vidage de
-- sûreté que `deploy.sh` prend juste avant chaque mise à jour.
