-- Le commentaire d'efficacite : le texte libre qui accompagne les preuves du
-- dossier -- « verifie sur trois cycles, aucun rebut entre le 2 et le 20 ».
--
-- Il se distingue de deux voisins avec lesquels on le confondrait :
--   * `effectiveness_criterion` dit CE QU'ON MESURERA, et se pose a l'ouverture ;
--   * le commentaire d'un verdict (table des evaluations) porte un JUGEMENT rendu,
--     signe par son auteur, et ne s'ecrit qu'au moment de juger.
-- Celui-ci n'est ni l'un ni l'autre : c'est la note qui court a cote des pieces,
-- avant tout verdict, et que personne n'est tenu de remplir.
ALTER TABLE capa_cases
    ADD COLUMN IF NOT EXISTS effectiveness_comment TEXT;

COMMENT ON COLUMN capa_cases.effectiveness_comment IS
    'Note libre accompagnant les preuves du dossier ; ni critere, ni verdict.';
