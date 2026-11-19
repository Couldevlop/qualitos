-- Le commentaire libre de l'analyse des causes (D4 du traitement 8D).
--
-- Le modèle de traitement place sous les outils d'analyse un champ
-- « Commentaires » : observations, hypothèses écartées, cause racine retenue.
-- L'écran portait les outils -- Ishikawa, 5 Pourquoi -- et la cause racine
-- qu'ils concluent, mais rien pour ce qui ne tient dans aucun des deux : la
-- piste qu'on a suivie puis abandonnée, et POURQUOI on l'a abandonnée. C'est
-- précisément ce qu'un auditeur demande quand il conteste la cause retenue, et
-- cela s'écrivait jusqu'ici dans la description de l'écart, où plus personne ne
-- le rapprochait de l'analyse.
--
-- Distinct de `root_cause` : celle-ci est la CONCLUSION, une phrase que le
-- rapport 8D publie. Le commentaire est le raisonnement autour, et il ne la
-- remplace pas -- un dossier qui n'aurait que le commentaire n'aurait pas
-- conclu.
--
-- TEXT et non VARCHAR : la longueur utile n'est pas connue, et une borne
-- arbitraire ferait perdre la fin d'une explication. Le service borne l'entrée
-- de l'API (4 000 caractères), ce qui tient la surface sans tronquer la base.
ALTER TABLE non_conformities
    ADD COLUMN cause_analysis_comment TEXT;

COMMENT ON COLUMN non_conformities.cause_analysis_comment IS
    'Commentaire libre du D4 : observations, hypotheses ecartees. Ni la cause racine, ni une action.';
