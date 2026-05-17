-- AI Act (UE 2024/1689) — EU Database Registrations (Art. 49 / 71).
-- Suivi des enregistrements EUDB pour systèmes IA HIGH-risk.

CREATE TABLE ai_act_eudb_registrations (
    id                                  UUID         NOT NULL,
    tenant_id                           UUID         NOT NULL,
    reference                           VARCHAR(64)  NOT NULL,
    ai_system_id                        UUID         NOT NULL,
    provider_entity_name                VARCHAR(250),
    provider_eu_representative          VARCHAR(250),
    member_state_of_reference           VARCHAR(2),
    intended_purpose_summary            VARCHAR(4000),
    technical_documentation_reference   VARCHAR(250),
    eudb_id                             VARCHAR(64),
    status                              VARCHAR(32)  NOT NULL,
    submitted_at                        TIMESTAMPTZ,
    submitted_by                        UUID,
    registration_date                   TIMESTAMPTZ,
    last_update_date                    TIMESTAMPTZ,
    last_update_summary                 VARCHAR(4000),
    rejected_at                         TIMESTAMPTZ,
    rejection_reason                    VARCHAR(2000),
    retired_at                          TIMESTAMPTZ,
    retirement_reason                   VARCHAR(2000),
    created_by                          UUID         NOT NULL,
    created_at                          TIMESTAMPTZ  NOT NULL,
    updated_at                          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_ai_act_eudb_registrations PRIMARY KEY (id),
    CONSTRAINT uq_eudb_tenant_reference UNIQUE (tenant_id, reference),
    CONSTRAINT uq_eudb_tenant_eudbid    UNIQUE (tenant_id, eudb_id),
    CONSTRAINT chk_eudb_status CHECK
        (status IN ('DRAFT','SUBMITTED','REGISTERED','UPDATED','REJECTED','RETIRED')),
    CONSTRAINT chk_eudb_reference CHECK
        (reference ~ '^[A-Z][A-Z0-9_-]{1,63}$'),
    CONSTRAINT chk_eudb_eudb_id CHECK
        (eudb_id IS NULL OR eudb_id ~ '^EUDB-AI-[A-Z0-9]{6,32}$'),
    CONSTRAINT chk_eudb_member_state CHECK
        (member_state_of_reference IS NULL OR member_state_of_reference ~ '^[A-Z]{2}$'),
    -- SUBMITTED et au-delà ⇒ providerEntityName + memberState + intendedPurpose obligatoires
    CONSTRAINT chk_eudb_submitted_has_fields CHECK
        (status = 'DRAFT'
         OR (provider_entity_name IS NOT NULL
             AND length(trim(provider_entity_name)) > 0
             AND member_state_of_reference IS NOT NULL
             AND intended_purpose_summary IS NOT NULL
             AND length(trim(intended_purpose_summary)) > 0)),
    -- SUBMITTED et au-delà ⇒ submitter + submitted_at
    CONSTRAINT chk_eudb_submitted_has_actor CHECK
        (status = 'DRAFT'
         OR (submitted_at IS NOT NULL AND submitted_by IS NOT NULL)),
    -- REGISTERED/UPDATED ⇒ eudb_id + registration_date
    CONSTRAINT chk_eudb_registered_has_eudb_id CHECK
        (status NOT IN ('REGISTERED','UPDATED')
         OR (eudb_id IS NOT NULL AND registration_date IS NOT NULL)),
    -- UPDATED ⇒ lastUpdate {date, summary} ≥ registrationDate
    CONSTRAINT chk_eudb_updated_consistency CHECK
        (status <> 'UPDATED'
         OR (last_update_date IS NOT NULL
             AND last_update_summary IS NOT NULL
             AND length(trim(last_update_summary)) > 0
             AND registration_date IS NOT NULL
             AND last_update_date >= registration_date)),
    -- REJECTED ⇒ reason + rejected_at
    CONSTRAINT chk_eudb_rejected_has_reason CHECK
        (status <> 'REJECTED'
         OR (rejected_at IS NOT NULL
             AND rejection_reason IS NOT NULL
             AND length(trim(rejection_reason)) > 0)),
    -- RETIRED ⇒ reason + retired_at
    CONSTRAINT chk_eudb_retired_has_reason CHECK
        (status <> 'RETIRED'
         OR (retired_at IS NOT NULL
             AND retirement_reason IS NOT NULL
             AND length(trim(retirement_reason)) > 0))
);

CREATE INDEX idx_eudb_tenant        ON ai_act_eudb_registrations (tenant_id);
CREATE INDEX idx_eudb_tenant_status ON ai_act_eudb_registrations (tenant_id, status);
CREATE INDEX idx_eudb_tenant_system ON ai_act_eudb_registrations (tenant_id, ai_system_id);

-- Index partiel : enregistrements actifs avec eudb_id — accès rapide depuis EUDB.
CREATE INDEX idx_eudb_active_with_id
    ON ai_act_eudb_registrations (tenant_id, eudb_id)
    WHERE status IN ('REGISTERED','UPDATED');

COMMENT ON TABLE  ai_act_eudb_registrations IS
    'AI Act (UE 2024/1689 Art. 49/71) Registrations in the European AI database.';
COMMENT ON COLUMN ai_act_eudb_registrations.eudb_id IS
    'Identifiant officiel attribué par l''EUDB (format EUDB-AI-XXXXXX).';
COMMENT ON COLUMN ai_act_eudb_registrations.member_state_of_reference IS
    'État membre UE/EEE de référence (ISO 3166-1 alpha-2).';
