-- ---------------------------------------------------------------------------
-- Un dossier CAPA qui cite un écart EST le dossier de cet écart.
--
-- Le lien entre un écart et son dossier n'était posé que par l'escalade depuis
-- la fiche de l'écart. Ouvert depuis la liste CAPA en tapant la référence
-- (« Ext-NC-2026-0004 »), le dossier restait orphelin : la fiche de l'écart
-- n'affichait pas son tableau d'actions, son rapport 8D ne le voyait pas, et sa
-- clôture ne l'attendait pas. La même liste ne propose qu'une seule source
-- d'écart — « Non-conformité interne » — si bien qu'un dossier né d'une
-- réclamation client s'affichait « interne » sur l'écran qui sert à répondre à
-- ce client.
--
-- Le service pose désormais les deux faits à l'ouverture : la source est
-- DÉRIVÉE de l'origine de l'écart cité, et le lien est posé si l'écart n'en a
-- pas déjà un. Cette migration rattrape les dossiers déjà ouverts.
--
-- Deux UPDATE, dans cet ordre :
--   1. l'écart sans dossier reçoit le PLUS ANCIEN de ceux qui le citent — un
--      écart a un dossier (ADR 0088), et c'est le premier ouvert qui le traite ;
--   2. la source de tout dossier citant un écart s'aligne sur l'origine de cet
--      écart. Les autres sources (audit, risque, réclamation, alerte) ne sont
--      pas touchées : elles ne désignent pas le registre des écarts.
-- ---------------------------------------------------------------------------

UPDATE non_conformities n
   SET capa_case_id = premier.id
  FROM (SELECT DISTINCT ON (tenant_id, source_ref)
               id, tenant_id, source_ref
          FROM capa_cases
         WHERE source_type IN ('NON_CONFORMITY', 'EXTERNAL_NON_CONFORMITY')
           AND source_ref IS NOT NULL
         ORDER BY tenant_id, source_ref, created_at ASC) AS premier
 WHERE n.tenant_id = premier.tenant_id
   AND n.reference = premier.source_ref
   AND n.capa_case_id IS NULL;

UPDATE capa_cases c
   SET source_type = CASE WHEN n.origin = 'EXTERNAL'
                          THEN 'EXTERNAL_NON_CONFORMITY'
                          ELSE 'NON_CONFORMITY' END
  FROM non_conformities n
 WHERE n.tenant_id = c.tenant_id
   AND n.reference = c.source_ref
   AND c.source_type IN ('NON_CONFORMITY', 'EXTERNAL_NON_CONFORMITY')
   AND c.source_type <> CASE WHEN n.origin = 'EXTERNAL'
                             THEN 'EXTERNAL_NON_CONFORMITY'
                             ELSE 'NON_CONFORMITY' END;
