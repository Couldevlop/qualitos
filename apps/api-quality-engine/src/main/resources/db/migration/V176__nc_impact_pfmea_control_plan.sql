-- L'IMPACT SUR L'ANALYSE DE RISQUE, DECIDE A LA CLOTURE DE L'ECART.
--
-- Un ecart traite enseigne quelque chose sur l'analyse des risques du produit :
-- soit un mode de defaillance que l'AMDEC n'avait pas prevu, soit un controle
-- que le plan de surveillance ne portait pas. L'ISO 9001 10.2 attend que cette
-- lecon soit tiree -- et l'IATF 16949 en fait une exigence explicite : toute
-- non-conformite se repercute sur la PFMEA et le plan de surveillance.
--
-- Jusqu'ici rien ne le demandait. L'ecart se closait sans que personne n'ait
-- eu a se prononcer, et le PFMEA restait muet sur un defaut qui etait survenu
-- pour de bon -- l'ecart exact qu'un auditeur cherche en premier.
--
-- Deux colonnes, et NULL n'est pas FALSE : NULL veut dire « la question n'a pas
-- encore ete posee », FALSE veut dire « elle l'a ete, et on a juge que non ».
-- La difference est toute l'affaire : la premiere ne prouve rien, la seconde
-- est une decision datee et imputable, et c'est elle que l'auditeur vient lire.
ALTER TABLE non_conformities
    ADD COLUMN pfmea_impacted        BOOLEAN,
    ADD COLUMN control_plan_impacted BOOLEAN;
