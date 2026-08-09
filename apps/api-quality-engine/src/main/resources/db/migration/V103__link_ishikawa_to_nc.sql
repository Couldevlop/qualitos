-- Un diagramme d'Ishikawa peut partir d'une non-conformité (§3.5, §4.3).
--
-- La fiche de non-conformité ouvrait déjà une CAPA et une analyse des 5 Pourquoi,
-- mais pas un Ishikawa : il fallait quitter la fiche, créer le diagramme à la
-- main, et recopier l'énoncé du problème. Le lien manquait, et avec lui le
-- chemin de retour vers l'écart qui a motivé l'analyse.
--
-- NULLABLE, contrairement à `five_whys_analyses.nc_id` : un diagramme existe
-- aussi pour lui-même — brainstorming d'atelier, revue de processus — et des
-- diagrammes existent déjà sans écart d'origine. Rendre la colonne obligatoire
-- exigerait de leur en inventer un.
ALTER TABLE ishikawa_diagrams
    ADD COLUMN nc_id UUID;

-- SET NULL et non CASCADE : l'analyse des causes garde sa valeur au-delà du
-- constat qui l'a déclenchée. Effacer une non-conformité ne doit pas emporter le
-- raisonnement qu'elle a produit — c'est souvent lui, le livrable.
ALTER TABLE ishikawa_diagrams
    ADD CONSTRAINT fk_ishikawa_diagrams_nc FOREIGN KEY (nc_id)
        REFERENCES non_conformities (id) ON DELETE SET NULL;

-- Lecture systématique « les diagrammes de CETTE non-conformité », du plus
-- récent au plus ancien : c'est ce que la fiche demande à l'ouverture.
CREATE INDEX idx_ishikawa_diagrams_tenant_nc
    ON ishikawa_diagrams (tenant_id, nc_id, created_at DESC);
