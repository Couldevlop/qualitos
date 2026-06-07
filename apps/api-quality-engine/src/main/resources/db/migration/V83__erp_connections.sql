-- V83: ERP connectors (CLAUDE.md §13.3 : « ERP → indicateurs production, achats, fournisseurs »)
--
-- Stocke les connexions ERP par tenant (SAP, Oracle Fusion, Microsoft Dynamics).
-- Calqué sur itsm_connections (V16). Le secret est chiffré au repos (SecretCipher,
-- AES-256-GCM) : la colonne credential_cipher contient le ciphertext base64.
--
-- L'idempotence des imports ne nécessite PAS de table de mapping dédiée :
--   - fournisseurs : upsert sur suppliers(tenant_id, code) — contrainte existante V (uk_supplier_tenant_code) ;
--   - indicateurs  : upsert sur kpi_measurements(kpi_id, period_start) — contrainte existante (uk_kpi_measure_period).
-- On réutilise donc les référentiels cibles plutôt que de dupliquer un mapping (référentiel commun, §3.6).

CREATE TABLE erp_connections (
    id                    UUID PRIMARY KEY,
    tenant_id             UUID NOT NULL,
    name                  VARCHAR(120) NOT NULL,
    provider              VARCHAR(32)  NOT NULL,
    base_url              VARCHAR(512) NOT NULL,
    username              VARCHAR(200),
    credential_cipher     VARCHAR(2048) NOT NULL,
    external_scope        VARCHAR(200),
    status                VARCHAR(32) NOT NULL,
    consecutive_failures  INT NOT NULL DEFAULT 0,
    last_sync_at          TIMESTAMP WITH TIME ZONE,
    last_success_at       TIMESTAMP WITH TIME ZONE,
    created_by            UUID NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_erp_tenant_name UNIQUE (tenant_id, name),
    CONSTRAINT chk_erp_provider CHECK (provider IN ('SAP', 'ORACLE_FUSION', 'DYNAMICS')),
    CONSTRAINT chk_erp_status   CHECK (status IN ('ACTIVE', 'DISABLED', 'DISABLED_ON_ERRORS')),
    CONSTRAINT chk_erp_base_url CHECK (base_url LIKE 'https://%')
);

CREATE INDEX idx_erp_conn_tenant ON erp_connections(tenant_id);
CREATE INDEX idx_erp_conn_status ON erp_connections(tenant_id, status);

COMMENT ON TABLE  erp_connections IS 'Connexions ERP par tenant (SAP/Oracle Fusion/Dynamics, CLAUDE.md §13.3).';
COMMENT ON COLUMN erp_connections.credential_cipher IS 'Ciphertext AES-256-GCM base64 (SecretCipher). Vault Transit à terme.';
COMMENT ON COLUMN erp_connections.external_scope IS 'Portée fonctionnelle ERP (Company Code SAP / Business Unit Oracle / env Dynamics).';
