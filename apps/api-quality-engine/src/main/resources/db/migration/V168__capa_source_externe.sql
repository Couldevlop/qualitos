-- ---------------------------------------------------------------------------
-- L'origine « écart externe » manquait à la contrainte, et bloquait la CAPA.
--
-- Le registre des écarts distingue depuis longtemps le constat interne de la
-- réclamation venue du dehors, et l'escalade vers une CAPA reporte cette
-- distinction dans le dossier : `NON_CONFORMITY` pour l'un,
-- `EXTERNAL_NON_CONFORMITY` pour l'autre. L'énumération Java porte cette
-- valeur ; la contrainte de la base, réécrite en V135, ne la connaît pas.
--
-- CONSÉQUENCE, mesurée en préproduction : escalader une réclamation client
-- vers une CAPA échouait en 409, toujours, sur la fiche même qui sert à
-- répondre au client. Le chemin interne passait, l'externe non — et rien à
-- l'écran ne disait pourquoi.
--
-- La V135 avait rattrapé le même retard pour SPC_ALERT et ANOMALY. Le motif se
-- répète : une valeur s'ajoute à l'énumération, la contrainte reste en arrière,
-- et le défaut n'apparaît qu'au premier usage réel. C'est pourquoi un banc
-- compare désormais les deux listes sur un vrai PostgreSQL
-- (`CapaSourceConstraintMatchesEnumPostgresTest`) : la prochaine valeur
-- oubliée fera rougir la CI au lieu d'attendre un client.
-- ---------------------------------------------------------------------------

ALTER TABLE capa_cases DROP CONSTRAINT chk_capa_cases_source;

ALTER TABLE capa_cases ADD CONSTRAINT chk_capa_cases_source CHECK (
    source_type IN ('NON_CONFORMITY', 'EXTERNAL_NON_CONFORMITY', 'AUDIT',
                    'COMPLAINT', 'INTERNAL', 'IOT_ALERT', 'SPC_ALERT',
                    'ANOMALY', 'RISK', 'OTHER'));
