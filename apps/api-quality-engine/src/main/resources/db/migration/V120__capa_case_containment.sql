-- Troisième raison d'ouverture d'un dossier CAPA : l'ENDIGUEMENT (§4.2).
--
-- Jusqu'ici un dossier ne pouvait être que CORRECTIVE ou PREVENTIVE. Or le
-- premier réflexe devant un écart n'est ni l'un ni l'autre : on bloque le lot,
-- on arrête la ligne, on prévient le client — sans savoir encore d'où vient le
-- problème. Faute de valeur pour le dire, ces dossiers étaient ouverts comme
-- « correctifs », et un dossier qui a seulement protégé le client se lisait
-- comme un dossier qui avait supprimé la cause (ISO 9001 §10.2, 8D étape D3).
--
-- Aucune donnée n'est réécrite : les dossiers existants restent tels qu'ils ont
-- été déclarés. Requalifier après coup un dossier qu'on n'a pas instruit serait
-- inventer une intention.
ALTER TABLE capa_cases
    DROP CONSTRAINT chk_capa_cases_type;

ALTER TABLE capa_cases
    ADD CONSTRAINT chk_capa_cases_type
        CHECK (type IN ('CONTAINMENT', 'CORRECTIVE', 'PREVENTIVE'));
