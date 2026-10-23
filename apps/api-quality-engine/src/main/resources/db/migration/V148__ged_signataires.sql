-- GED : le circuit de signature d'une version (ADR 0099).
--
-- À la soumission, l'auteur désigne de un à trois signataires dans l'annuaire ;
-- ils signent dans l'ordre (le signataire 2 après le 1, le 3 après le 2). Un
-- refus renvoie la version en brouillon. Une nouvelle soumission remplace la
-- liste : le refus précédent reste lisible sur la version (rejection_*).
--
-- La table porte une colonne tenant_id : la politique d'isolation par client
-- (ADR 0086) lui est posée automatiquement après la migration.
CREATE TABLE document_version_signatories (
    id           UUID          NOT NULL DEFAULT gen_random_uuid(),
    tenant_id    UUID          NOT NULL,
    version_id   UUID          NOT NULL,
    position     SMALLINT      NOT NULL,
    signer_id    UUID          NOT NULL,
    signer_name  VARCHAR(255)  NOT NULL,
    decision     VARCHAR(10)   NOT NULL DEFAULT 'PENDING',
    decided_at   TIMESTAMPTZ,
    comment      VARCHAR(1000),

    CONSTRAINT pk_document_version_signatories PRIMARY KEY (id),
    CONSTRAINT fk_doc_signatory_version FOREIGN KEY (version_id)
        REFERENCES document_versions (id) ON DELETE CASCADE,
    CONSTRAINT uk_doc_signatory_position UNIQUE (version_id, position),
    CONSTRAINT uk_doc_signatory_signer UNIQUE (version_id, signer_id),
    CONSTRAINT chk_doc_signatory_position CHECK (position BETWEEN 1 AND 3),
    CONSTRAINT chk_doc_signatory_decision CHECK (decision IN ('PENDING', 'APPROVED', 'REJECTED'))
);

-- « À valider par moi » : les signatures en attente d'une personne.
CREATE INDEX ix_doc_signatories_signer ON document_version_signatories (tenant_id, signer_id)
    WHERE decision = 'PENDING';
