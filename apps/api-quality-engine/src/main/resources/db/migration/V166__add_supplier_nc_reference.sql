-- Reference d'un ecart fournisseur : Sup-NC-{annee}-{sequence par client}.
--
-- Le registre fournisseurs n'en avait aucune. Un ecart s'y citait par son
-- numero de lot -- qui designe la marchandise, pas le constat -- ou par la date
-- et le nom du fournisseur : deux ecarts du meme lot ne se distinguaient plus,
-- et aucun courriel, aucune piece jointe, aucune ligne de tableur ne pouvait
-- renvoyer a une fiche precise.
--
-- Les trois registres se citent dans les memes reunions, d'ou trois prefixes
-- distincts : Int-NC- (constat interne), Ext-NC- (reclamation client),
-- Sup-NC- (ecart fournisseur). Le prefixe porte l'information la ou elle est
-- lue, sans qu'il faille ouvrir la fiche.
--
-- NULLABLE a dessein : les ecarts anterieurs n'ont pas de reference, et leur en
-- inventer une apres coup rendrait citable un numero que personne n'a jamais
-- lu. L'ecran affiche « — » pour ceux-la.
ALTER TABLE supplier_non_conformities
    ADD COLUMN reference VARCHAR(32);

-- Unique PAR CLIENT, pas globalement : chaque client compte sa propre suite, et
-- deux clients peuvent legitimement detenir Sup-NC-2026-0001. L'index partiel
-- laisse coexister les lignes anterieures sans reference.
CREATE UNIQUE INDEX uq_supplier_nc_tenant_reference
    ON supplier_non_conformities (tenant_id, reference)
    WHERE reference IS NOT NULL;
