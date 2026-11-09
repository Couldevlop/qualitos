-- Une exigence de partie intéressée peut faire naître une ligne du registre
-- des risques (§4.2 vers §6.1).
--
-- Jusqu'ici le risque énoncé sur une exigence restait dans la fiche de la
-- partie intéressée : il ne se cotait pas, ne se revoyait pas et ne comptait
-- dans aucun indicateur. Il entre maintenant au registre, avec sa référence,
-- sa cotation et son journal, et l'exigence le cite en retour.
--
-- L'origine ne porte pas de `source_id` : une exigence n'est pas un objet
-- autonome que le registre pourrait relire, c'est une ligne dans la fiche
-- d'une partie intéressée. La contrainte de source reste donc inchangée, et
-- `origin_ref` — « Client X / exigence … » — tient lieu de citation.

ALTER TABLE risk_register_risks
    DROP CONSTRAINT IF EXISTS ck_risk_register_risks_origin;

ALTER TABLE risk_register_risks
    ADD CONSTRAINT ck_risk_register_risks_origin
        CHECK (origin IN ('DIRECT', 'MANAGEMENT_REVIEW', 'AUDIT', 'CUSTOMER_FEEDBACK', 'MONITORING',
                          'FMEA', 'NON_CONFORMITY', 'INCIDENT', 'CHANGE', 'STAKEHOLDER'));

-- Les opportunités gardent leurs cinq origines : une exigence non tenue est un
-- risque, pas une occasion à saisir.
