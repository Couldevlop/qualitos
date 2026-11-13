-- =============================================================================
-- Vide les DONNÉES MÉTIER DES CLIENTS du moteur qualité, et rien d'autre.
--
-- Usage :
--     psql -U "$POSTGRES_USER" -d qualitos_quality -v ON_ERROR_STOP=1 \
--          -f scripts/vider-donnees-metier.sql
--
-- CE QUI DISPARAÎT : ce que les clients ont produit — écarts, CAPA, audits,
-- documents, indicateurs, analyses, rapports. Irréversible : la sauvegarde de
-- la nuit est la seule marche arrière
-- (docs/runbooks/sauvegarde-et-restauration.md).
--
-- CE QUI NE DISPARAÎT PAS :
--   * le RÉFÉRENTIEL livré avec le produit — les 60 normes du Standards Hub et
--     leurs 627 clauses, les packs sectoriels, la table des correspondances de
--     clauses. Voir « la règle du tenant_id » ci-dessous ;
--   * le PARAMÉTRAGE des clients — modules actifs, politique de routage IA,
--     normes adoptées, packs sectoriels, catalogues. Ce n'est pas une donnée
--     produite, c'est une décision prise par un admin de client (et par lui
--     seul). Remettre à zéro un jeu de démonstration ne doit pas obliger
--     quelqu'un à ré-activer quatorze modules et à re-choisir son fournisseur
--     de modèle — d'autant qu'un client sans politique IA voit tous ses appels
--     échouer, en silence du point de vue de l'utilisateur (ADR 0102) ;
--   * les COMPTES — identifiants, mots de passe, second facteur, rôles — qui
--     vivent dans la base `keycloak`. Personne n'a rien à ressaisir après ce
--     vidage ;
--   * les CLIENTS, leur facturation et l'annuaire des membres, qui vivent dans
--     `qualitos_core`. L'annuaire se reconstruirait de toute façon à la
--     connexion (ADR 0093), mais on n'y touche pas ;
--   * l'historique Flyway : l'effacer ferait rejouer toutes les migrations au
--     prochain démarrage, sur un schéma qui les porte déjà.
--
-- -----------------------------------------------------------------------------
-- LA RÈGLE DU `tenant_id`, et l'incident qui l'a écrite.
--
-- La première version de ce fichier vidait TOUTES les tables du schéma, sauf
-- l'historique Flyway. Lancée en préproduction le 13 novembre 2026, elle a
-- emporté avec les données des clients le référentiel normatif livré par les
-- migrations : 60 normes, 279 sections, 627 clauses, 663 exigences, 55 parcours
-- de certification. Ces lignes ne reviennent pas toutes seules — Flyway ne
-- rejoue pas une migration déjà enregistrée. Il a fallu les reprendre dans la
-- sauvegarde prise juste avant.
--
-- D'où la règle, qui est aussi la définition d'une donnée de client dans ce
-- schéma : **une table porte un `tenant_id` si, et seulement si, elle porte des
-- données de client.** Le référentiel, lui, n'appartient à personne : pas de
-- `tenant_id`, donc pas de vidage.
--
-- Les tables FILLES n'ont pas toutes un `tenant_id` — les actions d'une CAPA,
-- les étapes d'un PDCA, les causes d'un Ishikawa, les versions d'un document
-- l'héritent de leur parent. Elles ne sont pas listées ici, et c'est voulu :
-- `CASCADE` les emporte avec le parent qu'on vide. L'inverse — les lister à la
-- main — reviendrait à retenir deux cents noms, c'est-à-dire à en oublier.
--
-- UN SEUL `TRUNCATE` : vider table par table échouerait sur les clés
-- étrangères, et l'ordre de dépendance n'est écrit nulle part. Les séquences
-- repartent de 1 (`RESTART IDENTITY`) pour que les références lues à l'écran
-- recommencent à NC-2026-0001.
-- =============================================================================

\set ON_ERROR_STOP on

-- Garde-fou : ce fichier ne s'exécute que sur la base qu'il nomme. Lancé par
-- erreur sur `qualitos_core` ou sur `keycloak`, il effacerait les clients ou
-- les comptes — et ce sont précisément deux des choses qu'il promet de laisser
-- intactes.
DO $garde$
BEGIN
    IF current_database() <> 'qualitos_quality' THEN
        RAISE EXCEPTION
            'Ce script ne vide que qualitos_quality. Base courante : %. '
            'Rien n''a été effacé.', current_database();
    END IF;
END
$garde$;

DO $vidage$
DECLARE
    -- Le paramétrage d'un client : des tables qui portent bien un `tenant_id`,
    -- mais dont le contenu est une décision, pas une production. Elles sont
    -- nommées une par une, et c'est tenable : il y en a une poignée, et chaque
    -- ajout est un choix qu'on veut voir passer en revue.
    parametrage constant text[] := ARRAY[
        'tenant_module_activations',      -- les modules que le client a ouverts
        'tenant_industry_pack_activations',
        'tenant_ai_policy',               -- son fournisseur de modèle (ADR 0102)
        'tenant_standards',               -- les normes qu'il vise
        'tenant_catalog_entries',
        'nc_subcategory_settings'         -- son catalogue de natures d'écart
    ];
    liste text;
    combien int;
    epargnees int;
BEGIN
    SELECT string_agg(format('%I.%I', t.schemaname, t.tablename), ', '), count(*)
      INTO liste, combien
      FROM pg_tables t
     WHERE t.schemaname = 'public'
       AND t.tablename <> 'flyway_schema_history'
       AND NOT (t.tablename = ANY (parametrage))
       AND EXISTS (SELECT 1
                     FROM information_schema.columns c
                    WHERE c.table_schema = t.schemaname
                      AND c.table_name = t.tablename
                      AND c.column_name = 'tenant_id');

    SELECT count(*) INTO epargnees
      FROM pg_tables t
     WHERE t.schemaname = 'public'
       AND t.tablename <> 'flyway_schema_history'
       AND (t.tablename = ANY (parametrage) OR NOT EXISTS (SELECT 1
                         FROM information_schema.columns c
                        WHERE c.table_schema = t.schemaname
                          AND c.table_name = t.tablename
                          AND c.column_name = 'tenant_id'));

    IF liste IS NULL THEN
        RAISE NOTICE 'Aucune table de client à vider.';
        RETURN;
    END IF;

    EXECUTE 'TRUNCATE TABLE ' || liste || ' RESTART IDENTITY CASCADE';
    RAISE NOTICE '% tables de client vidées (et leurs filles, par CASCADE). '
                 '% tables épargnées : référentiel, tables filles et '
                 'paramétrage des clients.', combien, epargnees;
END
$vidage$;
