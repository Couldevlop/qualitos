-- APQP : « nouveau client » devient « modification majeure », et le projet de
-- reprise s'appelle en anglais.
--
-- ---------------------------------------------------------------------------
-- 1. Le quatrieme type nommait la mauvaise chose
-- ---------------------------------------------------------------------------
--
-- Les quatre natures d'un projet APQP disaient : NPI, ToW, NEW_CUSTOMER, OTHER.
-- « Produit connu, client nouveau » decrit un CONTEXTE COMMERCIAL, pas un
-- travail d'ingenierie : le cycle en V qu'il ouvre est exactement celui d'un
-- NPI ou d'un ToW selon ce qui change reellement, et le type n'apprenait donc
-- rien a qui filtrait la liste.
--
-- Ce qui manquait, en revanche, c'est la MODIFICATION MAJEURE : un produit deja
-- en serie dont on change la definition, l'outillage ou un procede special, et
-- qui repasse par un cycle APQP resserre sans etre une introduction. C'est le
-- cas le plus frequent apres le NPI, et il tombait faute de mieux dans OTHER —
-- ou il se melait a tout le reste.
--
-- Les projets deja saisis en NEW_CUSTOMER sont CONVERTIS et non supprimes : un
-- projet porte un cycle, des phases, des livrables et des pieces jointes. Le
-- type le mieux aligne est MAJOR_MODIFICATION, qui ouvre le meme cycle
-- resserre ; laisser ces lignes hors du CHECK aurait casse toute lecture.

ALTER TABLE apqp_projects DROP CONSTRAINT ck_apqp_projects_type;

UPDATE apqp_projects
   SET type       = 'MAJOR_MODIFICATION',
       updated_at = now()
 WHERE type = 'NEW_CUSTOMER';

ALTER TABLE apqp_projects
    ADD CONSTRAINT ck_apqp_projects_type
        CHECK (type IN ('NPI', 'TOW', 'MAJOR_MODIFICATION', 'OTHER'));

COMMENT ON COLUMN apqp_projects.type IS
    'Ce que le projet ouvre : NPI (produit nouveau), TOW (transfert d''outillage '
    'ou de site), MAJOR_MODIFICATION (produit en serie dont la definition change), '
    'OTHER.';

-- ---------------------------------------------------------------------------
-- 2. Le projet de reprise s'appelle en anglais
-- ---------------------------------------------------------------------------
--
-- La V131 a cree un « Projet par defaut » par client pour ne rien perdre au
-- passage aux projets multiples. Ce nom est une DONNEE, pas un libelle
-- d'interface : il ne traverse pas la traduction et s'affichait en francais
-- quelle que soit la langue choisie — y compris dans le dossier PPAP, qui est
-- remis a un donneur d'ordre et se lit en anglais (meme parti que le rapport
-- 8D, ADR 0071).
--
-- On ne renomme QUE les projets restes intacts : le nom d'origine ET la
-- description d'origine. Un client qui a renomme son projet, ou qui en a
-- reecrit la description, a fait un choix — le lui defaire serait pire que le
-- laisser en francais. C'est la meme regle que pour le referentiel APQP :
-- ce que le client ecrit lui appartient (ADR 0070).

UPDATE apqp_projects
   SET name        = 'Default project',
       description = 'APQP cycle carried over when multi-project support was '
                     'introduced. Rename it, or split its deliverables into new '
                     'projects.',
       updated_at  = now()
 WHERE name = 'Projet par défaut'
   AND description = 'Cycle APQP repris lors du passage aux projets multiples. '
                     'Renommez-le, ou répartissez ses livrables dans de nouveaux projets.';
