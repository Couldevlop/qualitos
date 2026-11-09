-- Les photos prises sur le terrain pendant un audit 5S (CLAUDE.md §3.2).
--
-- Une note de 3 sur 10 ne dit pas ce qui a ete vu ; la photo le montre, et c'est
-- elle qu'on ressort trois mois plus tard pour savoir si la zone a change.
-- Jusqu'ici l'audit ne portait qu'une colonne photo_url a remplir a la main :
-- personne ne tape une adresse d'image au milieu d'un atelier.
--
-- La photo vise le PILIER par son nom, et non la ligne de notation par son
-- identifiant. C'est ce qui la rend rejouable depuis la file hors ligne : en zone
-- blanche, la notation attend et n'a aucun identifiant serveur, alors que le
-- pilier est connu avant le reseau. Pas de cle etrangere vers
-- fives_audit_items donc, mais un couple (audit_id, pillar) ; l'unicite
-- (audit_id, pillar) de cette table garantit qu'il designe au plus une ligne.
--
-- Seule la metadonnee vit ici. Les octets vivent dans le stockage objet sous la
-- cle object_key, comme pour les preuves PDCA, CAPA et APQP.

CREATE TABLE fives_item_photos (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    -- Recopie depuis l'audit : la colonne fait entrer la table dans la politique
    -- de cloisonnement (ADR 0086) et permet de lire sur un seul index sans
    -- jointure.
    tenant_id         UUID         NOT NULL,
    audit_id          UUID         NOT NULL,
    pillar            VARCHAR(20)  NOT NULL,
    object_key        VARCHAR(512) NOT NULL,
    content_type      VARCHAR(150) NOT NULL,
    size_bytes        BIGINT       NOT NULL,
    original_filename VARCHAR(255),
    uploaded_by       UUID,
    created_at        TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_fives_item_photos PRIMARY KEY (id),
    CONSTRAINT fk_fives_item_photos_audit FOREIGN KEY (audit_id)
        REFERENCES fives_audits (id) ON DELETE CASCADE,
    -- Une cle d'objet ne designe qu'une ligne : deux metadonnees pour un meme
    -- binaire feraient disparaitre la photo de l'une quand l'autre est retiree.
    CONSTRAINT uk_fives_item_photos_object_key UNIQUE (object_key),
    CONSTRAINT ck_fives_item_photos_pillar CHECK (
        pillar IN ('SEIRI', 'SEITON', 'SEISO', 'SEIKETSU', 'SHITSUKE')
    ),
    -- Une photo vide n'est pas une preuve ; le plafond applicatif (10 Mo) reste
    -- au-dessus, celui-ci ne garde que l'absurde hors de la table.
    CONSTRAINT ck_fives_item_photos_size CHECK (size_bytes > 0)
);

COMMENT ON TABLE fives_item_photos IS
    'Photos de terrain attachees aux piliers d''un audit 5S ; metadonnee seule, binaire en stockage objet.';
COMMENT ON COLUMN fives_item_photos.pillar IS
    'Pilier photographie, designe par son nom pour rester adressable hors ligne (la ligne de notation n''a pas encore d''identifiant serveur).';
COMMENT ON COLUMN fives_item_photos.object_key IS
    'Cle de stockage tenants/{tenantId}/fives/{auditId}/{pillar}/{uuid}.{ext}, construite sans aucune part du nom de fichier client.';

CREATE INDEX ix_fives_item_photos_audit ON fives_item_photos (tenant_id, audit_id, pillar);
