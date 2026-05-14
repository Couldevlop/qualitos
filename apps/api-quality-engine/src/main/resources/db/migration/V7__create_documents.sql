CREATE TABLE documents (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id           UUID         NOT NULL,
    code                VARCHAR(100) NOT NULL,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    type                VARCHAR(30)  NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    owner_id            UUID         NOT NULL,
    current_version_id  UUID,
    mandatory_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_documents PRIMARY KEY (id),
    CONSTRAINT uk_documents_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT chk_documents_type CHECK (
        type IN ('POLICY','PROCEDURE','WORK_INSTRUCTION','RECORD','FORM','MANUAL','OTHER')
    ),
    CONSTRAINT chk_documents_status CHECK (status IN ('ACTIVE','ARCHIVED'))
);

CREATE INDEX idx_documents_tenant ON documents (tenant_id);
CREATE INDEX idx_documents_tenant_status ON documents (tenant_id, status);
CREATE INDEX idx_documents_owner ON documents (tenant_id, owner_id);

CREATE TABLE document_versions (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    document_id         UUID         NOT NULL,
    version_number      INTEGER      NOT NULL,
    content             TEXT,
    content_uri         VARCHAR(1024),
    content_hash        VARCHAR(64),
    change_note         TEXT,
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    author_id           UUID         NOT NULL,
    approved_by         UUID,
    approved_at         TIMESTAMPTZ,
    published_at        TIMESTAMPTZ,
    blockchain_tx_hash  VARCHAR(128),
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_document_versions PRIMARY KEY (id),
    CONSTRAINT fk_document_versions_doc FOREIGN KEY (document_id)
        REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT uk_doc_version_number UNIQUE (document_id, version_number),
    CONSTRAINT chk_doc_versions_status CHECK (
        status IN ('DRAFT','IN_REVIEW','APPROVED','PUBLISHED','OBSOLETE')
    ),
    CONSTRAINT chk_doc_versions_number CHECK (version_number >= 1)
);

CREATE INDEX idx_doc_versions_doc ON document_versions (document_id);
CREATE INDEX idx_doc_versions_doc_status ON document_versions (document_id, status);

CREATE TABLE document_acknowledgments (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    version_id      UUID        NOT NULL,
    tenant_id       UUID        NOT NULL,
    user_id         UUID        NOT NULL,
    acknowledged_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_document_acknowledgments PRIMARY KEY (id),
    CONSTRAINT fk_document_acks_version FOREIGN KEY (version_id)
        REFERENCES document_versions (id) ON DELETE CASCADE,
    CONSTRAINT uk_doc_ack_version_user UNIQUE (version_id, user_id)
);

CREATE INDEX idx_doc_acks_version ON document_acknowledgments (version_id);
CREATE INDEX idx_doc_acks_user ON document_acknowledgments (tenant_id, user_id);
