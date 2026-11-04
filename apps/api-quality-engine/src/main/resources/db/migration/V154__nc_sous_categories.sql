-- Sous-catégories de non-conformité : la NATURE de l'écart, sous sa catégorie.
--
-- Pourquoi. Sept catégories (produit, processus, documentation, fournisseur,
-- sécurité, environnement, autre) suffisent à ranger, pas à analyser : « 412 NC
-- de processus » ne dit pas s'il s'agit de dérives de paramètre, de procédures
-- non respectées ou d'étalonnages périmés, et c'est cette distinction-là qui
-- décide de l'action. Le niveau fin manquait.
--
-- Pourquoi une DONNÉE et non une énumération. Les natures d'écart ne sont pas
-- les mêmes dans une usine, un CHU, une banque ou une exploitation agricole.
-- Les inscrire dans le code reviendrait à coder un secteur dans le cœur de la
-- plateforme (§18.2 #9). Le client reçoit donc un catalogue de départ transverse
-- et le remplace entièrement par le sien quand le sien existe.
--
-- Facultative à dessein : un écart se saisit d'abord — souvent sur le terrain,
-- au téléphone, dans l'urgence — et se classe finement ensuite. L'exiger à la
-- création ferait saisir n'importe quoi.
ALTER TABLE non_conformities
    ADD COLUMN subcategory_code VARCHAR(30);

-- Les tableaux de bord croisent catégorie et nature pour un client : sans cet
-- index, chaque croisement balaie toute la table du tenant.
CREATE INDEX idx_non_conformities_tenant_subcategory
    ON non_conformities (tenant_id, category, subcategory_code)
    WHERE subcategory_code IS NOT NULL;

-- Le catalogue du client. Aucune ligne = celui de la plateforme s'applique :
-- un client qui n'a rien réglé classe ses écarts dès le premier jour.
CREATE TABLE nc_subcategory_settings (
    id         UUID PRIMARY KEY,
    tenant_id  UUID        NOT NULL UNIQUE,
    catalogue  TEXT        NOT NULL,
    updated_by UUID,
    updated_at TIMESTAMPTZ NOT NULL
);
