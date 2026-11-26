-- LE RENVOI EN MODIFICATION : ce que le second regard répond quand il ne clôt pas.
--
-- Le second regard n'avait qu'une réponse. La personne désignée pour valider un
-- écart résolu pouvait le clôturer, et c'est tout : si l'action corrective ne
-- répondait pas à la cause, si une preuve manquait, si le rapport se trompait,
-- elle n'avait aucun geste pour le dire. Dans les faits, on téléphonait au
-- pilote, et le registre affichait un écart « résolu » que personne ne clôturait
-- -- sans qu'aucune fiche ne porte la raison de cette attente.
--
-- La reprise (`reopened_at`) ne remplaçait pas ce geste : elle ramène l'écart au
-- DÉBUT de l'instruction, et elle dit la récidive -- un problème revenu après la
-- clôture. Un renvoi dit l'inverse : le traitement n'est pas encore bon, et
-- l'analyse reste valable. Les confondre ferait compter comme récidive un écart
-- qui n'a jamais été clos.
--
-- Le motif est FACULTATIF, comme celui de la reprise : exiger une justification
-- découragerait le renvoi lui-même, et un renvoi sans phrase vaut mieux qu'une
-- clôture de complaisance. Le nom de qui renvoie, lui, est toujours copié -- le
-- second regard ne se démontre que signé (ISO 9001 §10.2).
ALTER TABLE non_conformities
    ADD COLUMN sent_back_at TIMESTAMPTZ,
    ADD COLUMN sent_back_reason TEXT,
    ADD COLUMN sent_back_by_id UUID,
    ADD COLUMN sent_back_by_name VARCHAR(255);

-- Les écarts renvoyés se comptent : c'est la mesure de ce que le second regard
-- attrape, et elle manquait. Index partiel -- seule une part des écarts est
-- concernée, et un index plein paierait la colonne vide sur tous les autres.
CREATE INDEX idx_non_conformities_sent_back
    ON non_conformities (tenant_id, sent_back_at)
    WHERE sent_back_at IS NOT NULL;
