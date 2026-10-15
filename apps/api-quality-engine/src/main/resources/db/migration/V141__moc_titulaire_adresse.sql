-- Les titulaires d'un rôle de validation MOC portent leur nom ET leur adresse
-- (ADR 0087, 0091).
--
-- Jusqu'ici, le libellé recopié était l'adresse : c'est tout ce que l'annuaire
-- réservé aux administrateurs montrait, et le courriel aux validateurs partait
-- vers ce libellé. Les titulaires se choisissent désormais dans l'annuaire en
-- lecture restreinte, qui donne le nom de la personne : le libellé devient ce
-- nom, et l'adresse a sa propre colonne pour que le courriel continue de partir.
--
-- Les titulaires nommés avant gardent leur libellé ; quand c'était une adresse,
-- elle est reprise dans la nouvelle colonne.

ALTER TABLE moc_role_holders ADD COLUMN user_email VARCHAR(320);

UPDATE moc_role_holders SET user_email = user_label WHERE user_label LIKE '%@%';
