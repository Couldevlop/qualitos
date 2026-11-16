-- =============================================================================
-- V13 : L'identité légale et postale d'un cabinet
-- =============================================================================
--
-- CE QUE CETTE MIGRATION REVIENT SUR. La V11 avait tranché « le minimum : ce
-- n'est pas un client, il n'a ni facturation ni thème ici », et gardé quatre
-- champs : nom, numéro d'immatriculation, courriel, téléphone. Le raisonnement
-- tenait tant que le cabinet n'était qu'un porte-consultants.
--
-- Il ne tient plus pour UNE raison précise : le cabinet figure sur un accord
-- d'engagement, c'est-à-dire sur une pièce que le client conserve et qu'un
-- auditeur peut demander. Un accord qui désigne « Cabinet Durand » sans forme
-- juridique, sans adresse et sans numéro de TVA ne désigne personne de
-- vérifiable -- or c'est précisément ce qu'un accord sert à faire : nommer qui
-- entre chez le client, de façon opposable.
--
-- CE QUE CETTE MIGRATION N'AJOUTE PAS, et pourquoi. Pas de thème, pas de
-- couleurs, pas de logo, pas de slogan. Un client a un espace à habiller ; un
-- cabinet n'en a pas. Des colonnes d'image de marque qui ne s'afficheraient
-- nulle part se rempliraient quand même, et le jour où un écran les lirait
-- enfin, il afficherait des valeurs saisies des années plus tôt sans que
-- personne ait jamais pu les relire. La V11 avait raison sur ce point-là.
--
-- POURQUOI DANS LA MÊME TABLE, alors que l'identité d'un CLIENT est dans une
-- table à part (V10). La raison de V10 était la fréquence de lecture :
-- `tenants` est relu à chaque requête par le routage, la facturation et les
-- accès, son identité une fois par session. `partner_firms` n'a pas ce
-- problème : elle est lue dans la console des cabinets et sur les accords,
-- jamais sur un chemin chaud. Une seconde table coûterait une jointure et une
-- règle de cloisonnement de plus pour rien.
--
-- RLS : inchangé. `partner_firms` ne porte pas de `tenant_id` -- donnée de
-- plateforme gardée côté applicatif par le rôle (voir V11).

ALTER TABLE partner_firms
    -- La raison sociale complète, quand elle diffère du nom d'usage : « Durand
    -- & Associés SARL » sur l'accord, « Cabinet Durand » dans les écrans.
    ADD COLUMN legal_name    VARCHAR(255),
    ADD COLUMN legal_form    VARCHAR(64),
    ADD COLUMN vat_number    VARCHAR(32),
    ADD COLUMN address_line1 VARCHAR(255),
    ADD COLUMN address_line2 VARCHAR(255),
    ADD COLUMN postal_code   VARCHAR(16),
    ADD COLUMN city          VARCHAR(128),
    -- ISO 3166-1 alpha-2, en majuscules. La contrainte est portée ici ET par
    -- l'entité : une écriture qui passerait à côté de l'application -- une
    -- reprise de données, un correctif en base -- ne doit pas pouvoir semer un
    -- code pays que les écrans ne savent pas rendre.
    ADD COLUMN country_code  CHAR(2);

ALTER TABLE partner_firms
    ADD CONSTRAINT partner_firms_country_code_iso
        CHECK (country_code IS NULL OR country_code ~ '^[A-Z]{2}$');

COMMENT ON COLUMN partner_firms.legal_name IS
    'Raison sociale complète, telle qu''elle figure sur un accord d''engagement.';
COMMENT ON COLUMN partner_firms.vat_number IS
    'Numéro de TVA intracommunautaire. Non vérifié par la plateforme : le saisir faux est possible, le dire vérifié ne le serait pas.';
