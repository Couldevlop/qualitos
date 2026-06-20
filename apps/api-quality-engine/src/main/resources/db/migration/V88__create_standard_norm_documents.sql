-- Standards Hub §8.8 — documents normatifs générés par IA + workflow de
-- validation humaine (BROUILLON_IA → EN_VALIDATION → APPROUVE / REJETE).
-- Multi-tenant : tenant_id obligatoire (issu du JWT côté applicatif).
-- Les sections sont stockées en JSON (colonne TEXT) — pas de @Lob.

CREATE TABLE standard_norm_documents (
    id                UUID PRIMARY KEY,
    tenant_id         UUID NOT NULL,
    standard_id       UUID NOT NULL,
    standard_code     VARCHAR(100) NOT NULL,
    kind              VARCHAR(20) NOT NULL,
    title             VARCHAR(500) NOT NULL,
    sections_json     TEXT NOT NULL,
    status            VARCHAR(20) NOT NULL,
    ai_provider       VARCHAR(100),
    submitted_at      TIMESTAMPTZ,
    submitted_by      UUID,
    approved_at       TIMESTAMPTZ,
    approved_by       UUID,
    approval_notes    VARCHAR(4000),
    human_signature   VARCHAR(512),
    rejection_reason  VARCHAR(2000),
    created_by        UUID NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_snd_kind CHECK (kind IN ('MANUAL', 'POLICY', 'PROCEDURE')),
    CONSTRAINT chk_snd_status
        CHECK (status IN ('BROUILLON_IA', 'EN_VALIDATION', 'APPROUVE', 'REJETE')),
    -- Garde-fou dupliqué : un document APPROUVE porte forcément approbateur + signature.
    CONSTRAINT chk_snd_approved_signed CHECK (
        status <> 'APPROUVE'
        OR (approved_by IS NOT NULL AND human_signature IS NOT NULL AND approved_at IS NOT NULL)
    )
);

CREATE INDEX idx_snd_tenant ON standard_norm_documents (tenant_id);
CREATE INDEX idx_snd_tenant_status ON standard_norm_documents (tenant_id, status);

COMMENT ON TABLE standard_norm_documents IS
    'Documents normatifs générés par IA (Standards Hub §8.8) avec workflow de validation humaine.';
