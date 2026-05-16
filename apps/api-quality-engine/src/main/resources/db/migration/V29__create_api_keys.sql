-- V29: API keys (OWASP A01/A02/A07/A09)

CREATE TABLE api_keys (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    name            VARCHAR(120) NOT NULL,
    prefix          VARCHAR(16) NOT NULL,
    hashed_secret   VARCHAR(200) NOT NULL,
    scopes_csv      VARCHAR(2000),
    status          VARCHAR(32) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      UUID NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE,
    last_used_at    TIMESTAMP WITH TIME ZONE,
    revoked_at      TIMESTAMP WITH TIME ZONE,
    revoked_by      UUID,
    CONSTRAINT uk_api_key_prefix UNIQUE (prefix),
    CONSTRAINT chk_api_key_status CHECK (status IN ('ACTIVE','REVOKED','EXPIRED'))
);

CREATE INDEX idx_api_key_tenant         ON api_keys(tenant_id);
CREATE INDEX idx_api_key_tenant_status  ON api_keys(tenant_id, status);
CREATE INDEX idx_api_key_expirable      ON api_keys(status, expires_at);

COMMENT ON TABLE  api_keys IS 'Clés d''API par tenant — secret stocké en bcrypt (OWASP A02).';
COMMENT ON COLUMN api_keys.hashed_secret IS 'BCrypt strength 12 ; le plaintext n''est JAMAIS persisté.';
COMMENT ON COLUMN api_keys.prefix IS 'Préfixe public indexable, format base64url 8 chars.';
