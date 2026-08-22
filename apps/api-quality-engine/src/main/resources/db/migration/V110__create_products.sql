-- Le référentiel Produit manquait entièrement : la plateforme savait tenir un FMEA,
-- mais ce FMEA ne se rattachait à rien de physique. C'est le sujet que réclament le
-- PFMEA et le Control Plan — et la question que pose un auditeur IATF.

CREATE TABLE products (
    id              UUID PRIMARY KEY,
    tenant_id       UUID         NOT NULL,
    code            VARCHAR(64)  NOT NULL,
    designation     VARCHAR(250) NOT NULL,
    family          VARCHAR(120),
    revision_index  VARCHAR(16),
    status          VARCHAR(20)  NOT NULL,
    customer_label  VARCHAR(250),
    site_label      VARCHAR(250),
    owner_user_id   UUID,
    created_by      UUID         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_products_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_products_tenant_status ON products (tenant_id, status);

-- La nomenclature porte le lien fournisseur : un fournisseur fournit un composant,
-- pas un produit. ON DELETE SET NULL parce que retirer un fournisseur du référentiel
-- ne doit pas emporter la nomenclature avec lui.
CREATE TABLE product_components (
    id           UUID PRIMARY KEY,
    tenant_id    UUID           NOT NULL,
    product_id   UUID           NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    sequence_no  INTEGER        NOT NULL,
    reference    VARCHAR(120)   NOT NULL,
    label        VARCHAR(250),
    quantity     NUMERIC(12, 4),
    unit         VARCHAR(24),
    supplier_id  UUID           REFERENCES suppliers (id) ON DELETE SET NULL
);

CREATE INDEX idx_product_components_product
    ON product_components (product_id, sequence_no);

-- La gamme. C'est elle que désigneront une ligne de PFMEA et une ligne de Control
-- Plan : sans opération commune, les deux documents ne se recoupent pas.
CREATE TABLE product_operations (
    id           UUID PRIMARY KEY,
    tenant_id    UUID         NOT NULL,
    product_id   UUID         NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    sequence_no  INTEGER      NOT NULL,
    code         VARCHAR(32)  NOT NULL,
    label        VARCHAR(250) NOT NULL,
    workstation  VARCHAR(120),
    CONSTRAINT uk_product_operations_code UNIQUE (product_id, code)
);

CREATE INDEX idx_product_operations_product
    ON product_operations (product_id, sequence_no);
