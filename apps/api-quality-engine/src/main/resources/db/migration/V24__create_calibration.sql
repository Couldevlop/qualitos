-- V24: Calibration & Equipment Management (CLAUDE.md §4.10)

CREATE TABLE calibration_equipments (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    code            VARCHAR(120) NOT NULL,
    name            VARCHAR(250) NOT NULL,
    manufacturer    VARCHAR(200),
    model           VARCHAR(200),
    serial_number   VARCHAR(200),
    location        VARCHAR(500),
    status          VARCHAR(32) NOT NULL,
    critical        BOOLEAN NOT NULL DEFAULT FALSE,
    iot_device_id   UUID,
    owner_user_id   UUID,
    created_by      UUID NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_calibration_equipment_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT chk_calibration_equipment_status CHECK (status IN
        ('ACTIVE','OUT_OF_SERVICE','RETIRED'))
);

CREATE INDEX idx_calibration_equipment_tenant ON calibration_equipments(tenant_id);
CREATE INDEX idx_calibration_equipment_status ON calibration_equipments(tenant_id, status);
CREATE INDEX idx_calibration_equipment_iot    ON calibration_equipments(iot_device_id);

CREATE TABLE calibration_plans (
    id                   UUID PRIMARY KEY,
    tenant_id            UUID NOT NULL,
    equipment_id         UUID NOT NULL,
    frequency_months     INT NOT NULL,
    procedure_reference  VARCHAR(500),
    tolerance            VARCHAR(500),
    accreditation_ref    VARCHAR(250),
    last_calibrated_on   DATE,
    next_due_on          DATE NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_calibration_plan_equipment FOREIGN KEY (equipment_id)
        REFERENCES calibration_equipments(id) ON DELETE CASCADE,
    CONSTRAINT uk_calibration_plan_equipment UNIQUE (equipment_id),
    CONSTRAINT chk_calibration_plan_frequency CHECK (frequency_months BETWEEN 1 AND 120)
);

CREATE INDEX idx_calibration_plan_tenant ON calibration_plans(tenant_id);
CREATE INDEX idx_calibration_plan_due    ON calibration_plans(tenant_id, next_due_on);

CREATE TABLE calibration_records (
    id                     UUID PRIMARY KEY,
    tenant_id              UUID NOT NULL,
    equipment_id           UUID NOT NULL,
    performed_on           DATE NOT NULL,
    performed_by_user_id   UUID,
    performed_by_org       VARCHAR(250),
    result                 VARCHAR(32) NOT NULL,
    measurements           VARCHAR(4000),
    certificate_reference  VARCHAR(250),
    next_due_override      DATE,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_calibration_record_equipment FOREIGN KEY (equipment_id)
        REFERENCES calibration_equipments(id) ON DELETE CASCADE,
    CONSTRAINT chk_calibration_record_result CHECK (result IN
        ('PASS','CONDITIONAL','FAIL'))
);

CREATE INDEX idx_calibration_record_equipment ON calibration_records(equipment_id, performed_on);
CREATE INDEX idx_calibration_record_tenant    ON calibration_records(tenant_id, performed_on);

CREATE TABLE calibration_msa_studies (
    id                 UUID PRIMARY KEY,
    tenant_id          UUID NOT NULL,
    equipment_id       UUID NOT NULL,
    type               VARCHAR(32) NOT NULL,
    performed_on       DATE NOT NULL,
    study_value        NUMERIC(12, 4) NOT NULL,
    passing_threshold  NUMERIC(12, 4),
    result             VARCHAR(32) NOT NULL,
    notes              VARCHAR(2000),
    created_by         UUID NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_calibration_msa_equipment FOREIGN KEY (equipment_id)
        REFERENCES calibration_equipments(id) ON DELETE CASCADE,
    CONSTRAINT chk_calibration_msa_type CHECK (type IN
        ('GAGE_R_R','BIAS','LINEARITY','STABILITY')),
    CONSTRAINT chk_calibration_msa_result CHECK (result IN
        ('PASS','MARGINAL','FAIL'))
);

CREATE INDEX idx_msa_equipment   ON calibration_msa_studies(equipment_id, performed_on);
CREATE INDEX idx_msa_tenant_type ON calibration_msa_studies(tenant_id, type);

COMMENT ON TABLE calibration_equipments IS 'Équipements de mesure soumis à calibration (§4.10).';
COMMENT ON COLUMN calibration_equipments.critical IS
    'Critique pour la qualité produit — déclenche les garde-fous (FAIL → OUT_OF_SERVICE).';
COMMENT ON TABLE calibration_plans IS 'Plan de calibration ; un seul actif par équipement.';
COMMENT ON COLUMN calibration_plans.next_due_on IS 'Dérivé du dernier record + frequencyMonths (ou override).';
