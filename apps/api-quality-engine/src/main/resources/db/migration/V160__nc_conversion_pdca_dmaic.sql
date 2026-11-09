-- Un ecart devient un cycle PDCA ou un projet DMAIC selon sa gravite (CLAUDE.md
-- section 3.6). Le lien part de l'ecart et non du cycle : c'est la fiche de
-- l'ecart qu'on ouvre pour savoir s'il est traite, et une seconde conversion se
-- refuse sans parcourir tous les cycles du client.
--
-- Pas de cle etrangere vers pdca_cycles ni vers dmaic_projects : le cycle, une
-- fois ouvert, vit sa vie et peut etre archive par son module sans que la fiche
-- de l'ecart devienne inenregistrable. Le lien est une trace, pas une
-- dependance.
ALTER TABLE non_conformities
    ADD COLUMN IF NOT EXISTS pdca_cycle_id UUID,
    ADD COLUMN IF NOT EXISTS dmaic_project_id UUID;

-- Un ecart se traite par UNE methode : deux dossiers ouverts sur le meme ecart
-- le traiteraient deux fois, et chacun compterait dans les indicateurs. La base
-- le refuse, pas seulement le service.
ALTER TABLE non_conformities
    DROP CONSTRAINT IF EXISTS ck_non_conformities_une_methode;
ALTER TABLE non_conformities
    ADD CONSTRAINT ck_non_conformities_une_methode
        CHECK (pdca_cycle_id IS NULL OR dmaic_project_id IS NULL);

-- Les deux sens de lecture utiles : « cet ecart est-il traite ? » (deja servi
-- par la ligne elle-meme) et « quel ecart a ouvert ce cycle ? », posee par
-- l'ecran du cycle. Index partiels : la grande majorite des ecarts ne sont
-- convertis ni en cycle ni en projet.
CREATE INDEX IF NOT EXISTS ix_non_conformities_pdca_cycle
    ON non_conformities (tenant_id, pdca_cycle_id) WHERE pdca_cycle_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_non_conformities_dmaic_project
    ON non_conformities (tenant_id, dmaic_project_id) WHERE dmaic_project_id IS NOT NULL;
