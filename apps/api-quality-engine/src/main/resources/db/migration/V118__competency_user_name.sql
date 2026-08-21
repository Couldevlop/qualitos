-- Le nom de la personne evaluee, en clair.
--
-- La matrice de competences met les collaborateurs en COLONNES. La
-- plateforme n'ayant pas d'annuaire, ces colonnes n'auraient pour en-tete
-- que des identifiants techniques : une figure illisible, donc inutile.
--
-- Facultatif a dessein. A defaut, l'identifiant est abrege pour que la
-- colonne porte quand meme quelque chose ; l'inventer serait pire que
-- l'abreger. Meme choix que le nom d'assigne du dossier CAPA, pour la meme
-- raison : ce qui se lit a l'ecran ne peut pas dependre d'un annuaire qui
-- n'existe pas.
ALTER TABLE training_user_skills
    ADD COLUMN user_name VARCHAR(250);
