-- « Actions Taken » : ce qui a réellement été fait, et quand.
--
-- Le modèle portait l'action RECOMMANDÉE et la nouvelle cotation qui suit, mais
-- rien entre les deux. Or c'est ce qui manque qui justifie le reste : une
-- recommandation dit une intention, une action prise dit un fait, et c'est le
-- fait qui autorise à recoter. Sans cette colonne, un PFMEA montre une note qui
-- a baissé sans que rien n'explique pourquoi — exactement ce qu'un auditeur
-- relève.
ALTER TABLE fmea_items
    ADD COLUMN actions_taken    VARCHAR(2000),
    ADD COLUMN actions_taken_at DATE;

-- Le responsable de l'action, en clair.
--
-- La colonne « Resp. » de la trame attend un nom — « Manufacturing Eng »,
-- « Quality Eng » — et souvent une ÉQUIPE plutôt qu'une personne. Le modèle ne
-- portait qu'un identifiant d'utilisateur, que la plateforme ne sait pas
-- traduire en nom faute d'annuaire, et qui n'aurait de toute façon pas su
-- désigner un service. Même choix que le nom d'assigné du dossier CAPA :
-- l'identifiant reste pour ce qui est technique, le nom pour ce qui se lit.
ALTER TABLE fmea_items
    ADD COLUMN action_owner_name VARCHAR(250);
