-- Origine d'une non-conformité : détectée en interne, ou signalée du dehors.
--
-- Les deux ne se traitent pas de la même façon (délais, obligation de réponse,
-- destinataires) et une revue de direction demande d'abord de les compter
-- séparément. La colonne porte l'information ; deux entrées de navigation en
-- découlent.
--
-- L'existant est classé INTERNAL : c'est le cas majoritaire, et surtout la
-- valeur qui n'invente rien. Classer d'office en EXTERNAL gonflerait à tort
-- l'indicateur le plus exposé vis-à-vis des clients — mieux vaut sous-estimer
-- une donnée qu'on n'a pas que la fabriquer.
ALTER TABLE non_conformities
    ADD COLUMN origin VARCHAR(20) NOT NULL DEFAULT 'INTERNAL';

ALTER TABLE non_conformities
    ADD CONSTRAINT chk_non_conformities_origin CHECK (origin IN ('INTERNAL', 'EXTERNAL'));

-- Les deux écrans filtrent systématiquement sur (tenant, origine) : sans cet
-- index, chaque ouverture de la liste balaie toute la table du tenant.
CREATE INDEX idx_non_conformities_tenant_origin
    ON non_conformities (tenant_id, origin);
