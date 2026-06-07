-- Connecteur EHR / HL7 FHIR (§13.3 « EHR/HIS : HL7 FHIR R5 », §5.2 santé).
--
-- Importe les ressources FHIR « adverse event / abnormal / patient-safety »
-- (Observation, DiagnosticReport) et les matérialise en Non-Conformités côté
-- QualitOS, sans JAMAIS persister de donnée personnelle de santé en clair.
--
-- PRIVACY / RGPD (§11.3) : on ne stocke QUE des références techniques
-- (id de ressource FHIR, code, interprétation) — aucun identifiant patient
-- nominatif, aucune donnée clinique en clair. Les binaires/contenus restent
-- côté EHR ; QualitOS ne garde que le lien et la classification.

CREATE TABLE ehr_connections (
    id                   UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id            UUID         NOT NULL,
    name                 VARCHAR(120) NOT NULL,
    provider             VARCHAR(32)  NOT NULL,           -- FHIR_R4 | FHIR_R5
    fhir_base_url        VARCHAR(512) NOT NULL,
    -- Authentification : soit Basic (username + secret), soit Bearer (secret seul).
    auth_mode            VARCHAR(16)  NOT NULL,           -- BASIC | BEARER
    username             VARCHAR(200),
    credential_cipher    VARCHAR(2048) NOT NULL,          -- ciphertext base64 (AES-GCM), jamais en clair
    -- Catégorie/flag FHIR ciblé pour le filtrage des ressources « patient-safety ».
    resource_category    VARCHAR(120),
    status               VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    consecutive_failures INTEGER      NOT NULL DEFAULT 0,
    last_sync_at         TIMESTAMPTZ,
    last_success_at      TIMESTAMPTZ,
    created_by           UUID         NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_ehr_connections PRIMARY KEY (id),
    CONSTRAINT uk_ehr_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_ehr_connections_tenant ON ehr_connections (tenant_id);

-- Table d'idempotence : trace chaque ressource FHIR déjà importée → NC créée.
-- Garantit qu'une 2e synchronisation ne recrée pas de doublon de NC.
-- On ne conserve QUE l'identifiant TECHNIQUE de la ressource FHIR (pas de PII).
CREATE TABLE ehr_imported_resources (
    id                 UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id          UUID         NOT NULL,
    connection_id      UUID         NOT NULL,
    fhir_resource_type VARCHAR(64)  NOT NULL,             -- Observation | DiagnosticReport
    fhir_resource_id   VARCHAR(256) NOT NULL,             -- id technique FHIR, non nominatif
    nc_id              UUID         NOT NULL,
    imported_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_ehr_imported_resources PRIMARY KEY (id),
    -- Idempotence par connexion + id de ressource FHIR.
    CONSTRAINT uk_ehr_imported_resource UNIQUE (connection_id, fhir_resource_id),
    CONSTRAINT fk_ehr_imported_connection FOREIGN KEY (connection_id)
        REFERENCES ehr_connections (id) ON DELETE CASCADE
);

CREATE INDEX idx_ehr_imported_tenant ON ehr_imported_resources (tenant_id);
CREATE INDEX idx_ehr_imported_connection ON ehr_imported_resources (connection_id);
