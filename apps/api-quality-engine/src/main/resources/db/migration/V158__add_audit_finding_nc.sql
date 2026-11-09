-- Le constat d'audit qui devient une non-conformité à traiter.
--
-- Le lien vit du côté de l'audit, exactement comme `capa_id` : c'est la fiche
-- d'audit qui doit dire « NC déclarée » au lieu de reproposer le bouton, et
-- c'est elle qui sait si un constat a déjà été instruit. Le registre des écarts,
-- lui, n'a pas à connaître les audits pour fonctionner.
--
-- Nullable à dessein : la plupart des constats n'ouvrent pas d'écart — une
-- conformité, une observation, une piste de progrès ne se « traitent » pas.

ALTER TABLE audit_findings
    ADD COLUMN IF NOT EXISTS nc_id UUID;

-- Index partiel : seuls les constats effectivement instruits sont recherchés
-- par leur écart, et ils sont une minorité de la table.
CREATE INDEX IF NOT EXISTS idx_audit_findings_nc
    ON audit_findings (nc_id)
    WHERE nc_id IS NOT NULL;
