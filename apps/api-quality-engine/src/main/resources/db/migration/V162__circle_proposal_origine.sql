-- Une idée de cercle peut naître ailleurs : d'un audit 5S dont un pilier est
-- faible, par exemple (§3.6). Elle cite alors son origine en clair.
--
-- Une citation plutôt qu'une clé étrangère, comme au registre des risques :
-- l'audit se clôture, se corrige, s'archive, et un lien dur se briserait sur une
-- idée qui, elle, reste à traiter. La colonne naît nulle : les idées déjà
-- déposées l'ont été de la main de quelqu'un, et c'est ce que nul veut dire.
ALTER TABLE circle_proposals
    ADD COLUMN origin_ref VARCHAR(255);

COMMENT ON COLUMN circle_proposals.origin_ref IS
    'D''où vient l''idée, citée en clair (audit 5S, zone, pilier, note) ; NULL si déposée à la main';
