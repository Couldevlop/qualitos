-- UNE EVALUATION D'EFFICACITE PEUT PORTER SUR UNE ACTION, ET PLUS SEULEMENT
-- SUR LE DOSSIER.
--
-- La V177 a donne a chaque action sa propre exigence de verification. Restait
-- qu'un dossier se cloturait sans que ces verifications aient eu lieu : seule
-- celle du dossier etait exigee (ADR 0073). Une action promise verifiee
-- pouvait donc etre marquee « faite », le dossier clos, et personne n'avait
-- jamais regarde si elle avait produit son effet.
--
-- POURQUOI DANS LA TABLE EXISTANTE, et non dans des colonnes sur l'action : un
-- jugement est une ligne d'HISTORIQUE immuable (ADR 0108). La suite des verdicts
-- -- inefficace, puis partiel, puis efficace -- est exactement ce qu'un auditeur
-- lit : elle montre un systeme qui corrige. Un champ ecrase sur l'action ne
-- montrerait que la fin, et donnerait de l'action une image de reussite
-- immediate qui est fausse. Voir l'ADR 0132.
--
-- NULL = l'evaluation porte sur LE DOSSIER, ce qui est le cas de toutes les
-- lignes existantes. Renseignee, elle porte sur l'action citee.
ALTER TABLE capa_effectiveness_reviews
    ADD COLUMN IF NOT EXISTS capa_action_id UUID;

-- Effacement en cascade : une action supprimee emporte ses evaluations. Le
-- refus de supprimer une action qui porte une preuve vit dans le service, ou il
-- peut s'expliquer ; ici, la contrainte ne doit pas laisser de lignes
-- orphelines pointant un identifiant qui n'existe plus.
ALTER TABLE capa_effectiveness_reviews
    ADD CONSTRAINT fk_capa_eff_reviews_action
        FOREIGN KEY (capa_action_id) REFERENCES capa_actions (id) ON DELETE CASCADE;

-- Index PARTIEL : les lignes du dossier -- la majorite -- n'ont rien a y faire.
-- La seule question posee est « les evaluations de ces actions-la », et elle se
-- pose a chaque affichage d'un dossier.
CREATE INDEX IF NOT EXISTS ix_capa_eff_reviews_action
    ON capa_effectiveness_reviews (tenant_id, capa_action_id)
    WHERE capa_action_id IS NOT NULL;

COMMENT ON COLUMN capa_effectiveness_reviews.capa_action_id IS
    'Action jugee ; NULL = l''evaluation porte sur le dossier entier (ADR 0132).';
