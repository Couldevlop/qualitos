-- L'objectif qualité ne porte plus ni pilote de CAPA ni délai d'analyse de
-- cause racine : les deux se saisissent dans la fenêtre d'ouverture du dossier,
-- au moment où l'on décide d'agir. Recette du 27/11/2026, ADR 0143.
--
-- Les colonnes sont RELÂCHÉES, pas supprimées. Pendant un déploiement progressif,
-- l'ancienne version du service est encore en ligne et écrit ces trois colonnes :
-- les retirer ferait échouer ses insertions. Leur suppression appartient à une
-- migration ultérieure, une fois la bascule faite.

ALTER TABLE quality_objectives ALTER COLUMN capa_owner_id DROP NOT NULL;
ALTER TABLE quality_objectives ALTER COLUMN root_cause_due_days DROP NOT NULL;

-- La contrainte porterait sur une colonne devenue facultative : NULL la
-- satisfait déjà, mais la garder laisserait croire que la valeur est encore lue.
ALTER TABLE quality_objectives DROP CONSTRAINT IF EXISTS ck_quality_objectives_due_days;

COMMENT ON COLUMN quality_objectives.capa_owner_id IS
    'Obsolète (ADR 0143) : le responsable se désigne à l''ouverture de la CAPA.';
COMMENT ON COLUMN quality_objectives.capa_owner_name IS
    'Obsolète (ADR 0143) : le responsable se désigne à l''ouverture de la CAPA.';
COMMENT ON COLUMN quality_objectives.root_cause_due_days IS
    'Obsolète (ADR 0143) : l''échéance se saisit à l''ouverture de la CAPA.';
