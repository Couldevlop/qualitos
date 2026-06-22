-- Standards Hub §8.8 — génération documentaire IA AVANCÉE multi-documents.
-- Un dossier orchestre la génération en lot de plusieurs documents normatifs
-- (Manuel Qualité, Politique Qualité, procédures documentées), chacun lié à un
-- standard_norm_documents porteur du workflow de validation humaine (V88).
-- Multi-tenant : tenant_id obligatoire (issu du JWT côté applicatif).
-- Les pièces du dossier sont stockées en JSON (colonne TEXT) — pas de @Lob.

CREATE TABLE standard_doc_dossiers (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    standard_id         UUID NOT NULL,
    standard_code       VARCHAR(100) NOT NULL,
    standard_name       VARCHAR(500) NOT NULL,
    organization_name   VARCHAR(500) NOT NULL,
    language            VARCHAR(16) NOT NULL,
    documents_json      TEXT NOT NULL,
    status              VARCHAR(30) NOT NULL,
    ai_provider         VARCHAR(100),
    integrity_sha256    VARCHAR(64),
    integrity_signature TEXT,
    anchor_tx_ref       VARCHAR(300),
    finalized_at        TIMESTAMPTZ,
    finalized_by        UUID,
    created_by          UUID NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_sdd_status
        CHECK (status IN ('GENERATION_EN_COURS', 'GENERE', 'FINALISE')),
    -- Garde-fou dupliqué : un dossier FINALISE porte forcément empreinte + signature + ancrage.
    CONSTRAINT chk_sdd_finalized_sealed CHECK (
        status <> 'FINALISE'
        OR (integrity_sha256 IS NOT NULL
            AND integrity_signature IS NOT NULL
            AND anchor_tx_ref IS NOT NULL
            AND finalized_by IS NOT NULL
            AND finalized_at IS NOT NULL)
    )
);

CREATE INDEX idx_sdd_tenant ON standard_doc_dossiers (tenant_id);
CREATE INDEX idx_sdd_tenant_status ON standard_doc_dossiers (tenant_id, status);

COMMENT ON TABLE standard_doc_dossiers IS
    'Dossiers documentaires générés en lot par IA (Standards Hub §8.8) — multi-documents avec validation humaine par pièce et scellement d''intégrité à la finalisation.';
