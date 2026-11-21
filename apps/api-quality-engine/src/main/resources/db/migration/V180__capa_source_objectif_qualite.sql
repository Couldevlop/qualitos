-- ---------------------------------------------------------------------------
-- L'origine « objectif qualité » manquait à la contrainte.
--
-- Un objectif qualité manqué propose sa CAPA (ADR 0133), et ce dossier porte
-- l'origine `QUALITY_OBJECTIVE` : c'est elle qui verrouille son critère
-- d'efficacité sur le retour du KPI sous la cible. L'énumération Java porte la
-- valeur depuis la V179 ; la contrainte de la base, elle, est restée sur la
-- liste de la V168.
--
-- CONSÉQUENCE sans ce rattrapage : accepter une proposition d'objectif échoue
-- en base, toujours — et sur le seul chemin que la revue d'objectifs propose.
--
-- Le motif se répète (SPC_ALERT et ANOMALY en V135, EXTERNAL_NON_CONFORMITY en
-- V168) : une valeur s'ajoute à l'énumération, la contrainte reste en arrière.
-- Le banc posé pour cela l'a vu cette fois en CI, et non chez un client.
-- ---------------------------------------------------------------------------

ALTER TABLE capa_cases DROP CONSTRAINT chk_capa_cases_source;

ALTER TABLE capa_cases ADD CONSTRAINT chk_capa_cases_source CHECK (
    source_type IN ('NON_CONFORMITY', 'EXTERNAL_NON_CONFORMITY', 'AUDIT',
                    'COMPLAINT', 'INTERNAL', 'IOT_ALERT', 'SPC_ALERT',
                    'ANOMALY', 'RISK', 'QUALITY_OBJECTIVE', 'OTHER'));
