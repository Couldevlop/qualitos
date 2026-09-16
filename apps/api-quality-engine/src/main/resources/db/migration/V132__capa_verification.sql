-- CAPA : une vérification peut être exigée, confiée à quelqu'un, et instruite.
--
-- Le dossier portait déjà `effectiveness_verified` : le CONSTAT qu'une
-- vérification a eu lieu. Il manquait ce qui vient avant — l'EXIGENCE. Sans
-- elle, rien ne distinguait un dossier qu'on a délibérément choisi de ne pas
-- vérifier d'un dossier qu'on a simplement oublié de vérifier, et c'est la
-- première question d'un auditeur.
--
-- Quatre colonnes, toutes facultatives en base : un dossier ouvert avant ce lot
-- n'a pris aucune décision sur le sujet, et lui en prêter une serait mentir.
-- C'est le SERVICE qui tient la règle « si requise, alors assignée » — la base ne
-- peut pas la porter proprement, puisque `NULL` y signifie « pas encore décidé »
-- et non « non requise ».

ALTER TABLE capa_cases
    -- NULL = la question n'a pas encore été tranchée. TRUE/FALSE = elle l'a été,
    -- et c'est une information en soi : dire « non » est une décision.
    ADD COLUMN verification_required BOOLEAN,

    -- Qui vérifie. L'identifiant vient de l'annuaire du client (/api/v1/users) ;
    -- le libellé est recopié pour que la fiche reste lisible même si le compte
    -- est désactivé plus tard — un rapport d'audit se relit des années après.
    ADD COLUMN verification_assignee_id   UUID,
    ADD COLUMN verification_assignee_name VARCHAR(255),

    -- Ce qu'il faut vérifier, et comment. Du texte libre : la consigne dépend de
    -- l'action corrective, et aucune liste fermée ne l'aurait couverte.
    ADD COLUMN verification_instructions  TEXT;

-- Les dossiers dont la vérification est exigée et encore à faire : c'est la
-- liste de travail du vérificateur, et la seule lecture fréquente de ces
-- colonnes. Index PARTIEL, donc minuscule — il n'indexe pas les dossiers pour
-- lesquels la question ne se pose pas.
CREATE INDEX idx_capa_verification_a_faire
    ON capa_cases (tenant_id, verification_assignee_id)
    WHERE verification_required = TRUE
      AND effectiveness_verified IS NOT TRUE;

COMMENT ON COLUMN capa_cases.verification_required IS
    'Une vérification d''efficacité est-elle exigée ? NULL = non encore tranché.';
COMMENT ON COLUMN capa_cases.verification_assignee_id IS
    'Membre de l''organisation à qui la vérification est confiée (annuaire api-core).';
COMMENT ON COLUMN capa_cases.verification_instructions IS
    'Ce qu''il faut vérifier et comment, tel que rédigé par le pilote du dossier.';
