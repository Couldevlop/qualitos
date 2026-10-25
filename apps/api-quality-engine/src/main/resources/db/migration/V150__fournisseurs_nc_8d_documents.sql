-- NC fournisseur : référence et quantité de pièces touchées, suivi du 8D
-- demandé au fournisseur et reçu de lui ; documents joints aux NC (8D du
-- fournisseur, preuves de CAPA) et aux certificats (le certificat lui-même).

-- La référence et la quantité sont exigées à la saisie par l'API. Elles restent
-- facultatives en base : les NC enregistrées avant cette migration n'en portent
-- pas, et en inventer une fausserait l'historique.
ALTER TABLE supplier_non_conformities
    ADD COLUMN part_number       VARCHAR(100),
    ADD COLUMN part_quantity     INTEGER,
    ADD COLUMN eight_d_requested BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN eight_d_received  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT ck_supplier_nc_part_quantity CHECK (part_quantity IS NULL OR part_quantity > 0);

-- Même motif que audit_documents (V140) : la métadonnée ici, le binaire dans le
-- stockage objet sous une clé construite par le serveur.
CREATE TABLE supplier_documents (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id         UUID         NOT NULL,
    supplier_id       UUID         NOT NULL,
    -- Le porteur : une NC ou un certificat du fournisseur. Pas de clé étrangère
    -- possible sur un porteur à deux tables ; le service retire les documents
    -- d'un certificat supprimé, et la cascade sur le fournisseur couvre le reste.
    owner_type        VARCHAR(32)  NOT NULL,
    owner_id          UUID         NOT NULL,
    category          VARCHAR(32)  NOT NULL,
    object_key        VARCHAR(512) NOT NULL,
    content_type      VARCHAR(150) NOT NULL,
    size_bytes        BIGINT       NOT NULL,
    original_filename VARCHAR(255),
    uploaded_by       UUID,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_supplier_documents PRIMARY KEY (id),
    CONSTRAINT fk_supplier_documents_supplier FOREIGN KEY (supplier_id)
        REFERENCES suppliers (id) ON DELETE CASCADE,
    -- Une clé, une ligne : sinon retirer l'une effacerait le binaire de l'autre.
    CONSTRAINT uq_supplier_documents_object_key UNIQUE (object_key),
    CONSTRAINT ck_supplier_documents_size CHECK (size_bytes > 0),
    CONSTRAINT ck_supplier_documents_owner CHECK (owner_type IN ('NON_CONFORMITY', 'CERTIFICATE')),
    CONSTRAINT ck_supplier_documents_category
        CHECK (category IN ('EIGHT_D', 'CAPA_EVIDENCE', 'CERTIFICATE', 'OTHER'))
);

CREATE INDEX ix_supplier_documents_owner
    ON supplier_documents (tenant_id, owner_type, owner_id, created_at);
