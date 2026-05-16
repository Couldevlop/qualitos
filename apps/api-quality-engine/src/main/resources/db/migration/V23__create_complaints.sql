-- V23: Customer Complaints / VoC (CLAUDE.md §4.9)

CREATE TABLE complaints (
    id                    UUID PRIMARY KEY,
    tenant_id             UUID NOT NULL,
    code                  VARCHAR(100) NOT NULL,
    channel               VARCHAR(32) NOT NULL,
    customer_name         VARCHAR(250),
    customer_email        VARCHAR(320),
    customer_external_id  VARCHAR(200),
    subject               VARCHAR(250) NOT NULL,
    description           VARCHAR(4000),
    severity              VARCHAR(32) NOT NULL,
    category              VARCHAR(32) NOT NULL,
    status                VARCHAR(32) NOT NULL,
    supplier_id           UUID,
    capa_case_id          UUID,
    assigned_to_user_id   UUID,
    satisfaction_score    INT,
    received_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    first_response_at     TIMESTAMP WITH TIME ZONE,
    resolved_at           TIMESTAMP WITH TIME ZONE,
    closed_at             TIMESTAMP WITH TIME ZONE,
    rejection_reason      VARCHAR(1000),
    created_by            UUID NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_complaint_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT chk_complaint_channel CHECK (channel IN
        ('EMAIL','PHONE','WEB_FORM','IN_PERSON','SOCIAL','ITSM_IMPORT','API','POSTAL','OTHER')),
    CONSTRAINT chk_complaint_severity CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT chk_complaint_category CHECK (category IN
        ('PRODUCT','SERVICE','DELIVERY','BILLING','QUALITY','SAFETY','OTHER')),
    CONSTRAINT chk_complaint_status CHECK (status IN
        ('RECEIVED','UNDER_INVESTIGATION','RESPONDED','RESOLVED','CLOSED','REJECTED','REOPENED')),
    CONSTRAINT chk_complaint_satisfaction CHECK (
        satisfaction_score IS NULL OR satisfaction_score BETWEEN 0 AND 10)
);

CREATE INDEX idx_complaint_tenant          ON complaints(tenant_id);
CREATE INDEX idx_complaint_tenant_status   ON complaints(tenant_id, status);
CREATE INDEX idx_complaint_tenant_category ON complaints(tenant_id, category);
CREATE INDEX idx_complaint_tenant_supplier ON complaints(tenant_id, supplier_id);

CREATE TABLE complaint_responses (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    complaint_id    UUID NOT NULL,
    author_user_id  UUID NOT NULL,
    channel         VARCHAR(32) NOT NULL,
    body            VARCHAR(4000) NOT NULL,
    internal_note   BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_complaint_response_complaint FOREIGN KEY (complaint_id)
        REFERENCES complaints(id) ON DELETE CASCADE,
    CONSTRAINT chk_complaint_response_channel CHECK (channel IN
        ('EMAIL','PHONE','WEB_FORM','IN_PERSON','SOCIAL','ITSM_IMPORT','API','POSTAL','OTHER'))
);

CREATE INDEX idx_complaint_response_complaint ON complaint_responses(complaint_id, sent_at);
CREATE INDEX idx_complaint_response_tenant    ON complaint_responses(tenant_id);

COMMENT ON TABLE complaints IS 'Réclamations clients multi-canal (§4.9).';
COMMENT ON COLUMN complaints.satisfaction_score IS '0..10 (NPS-like; cf. §6.2).';
