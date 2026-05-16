-- V22: Change Management (CLAUDE.md §4.8)

CREATE TABLE change_requests (
    id                UUID PRIMARY KEY,
    tenant_id         UUID NOT NULL,
    code              VARCHAR(100) NOT NULL,
    title             VARCHAR(250) NOT NULL,
    description       VARCHAR(4000),
    type              VARCHAR(32) NOT NULL,
    priority          VARCHAR(32) NOT NULL,
    status            VARCHAR(32) NOT NULL,
    requester_user_id UUID NOT NULL,
    owner_user_id     UUID,
    planned_for       DATE,
    implemented_at    DATE,
    impact_summary    VARCHAR(2000),
    risk_assessment   VARCHAR(2000),
    rejection_reason  VARCHAR(1000),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_change_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT chk_change_type CHECK (type IN
        ('DOCUMENT','PROCESS','EQUIPMENT','SUPPLIER','IT_SYSTEM','ORGANIZATIONAL','OTHER')),
    CONSTRAINT chk_change_priority CHECK (priority IN
        ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT chk_change_status CHECK (status IN
        ('DRAFT','SUBMITTED','UNDER_REVIEW','APPROVED','REJECTED','IMPLEMENTED','CANCELLED'))
);

CREATE INDEX idx_change_tenant        ON change_requests(tenant_id);
CREATE INDEX idx_change_tenant_status ON change_requests(tenant_id, status);
CREATE INDEX idx_change_tenant_type   ON change_requests(tenant_id, type);

CREATE TABLE change_impacts (
    id          UUID PRIMARY KEY,
    tenant_id   UUID NOT NULL,
    change_id   UUID NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id   UUID NOT NULL,
    notes       VARCHAR(1000),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_change_impact_change FOREIGN KEY (change_id)
        REFERENCES change_requests(id) ON DELETE CASCADE,
    CONSTRAINT uk_change_impact_unique UNIQUE (change_id, target_type, target_id),
    CONSTRAINT chk_change_impact_type CHECK (target_type IN
        ('DOCUMENT','TRAINING_PATH','SUPPLIER','IOT_DEVICE','FMEA_PROJECT',
         'PDCA_CYCLE','STANDARD','OTHER'))
);

CREATE INDEX idx_change_impact_change ON change_impacts(change_id);

CREATE TABLE change_approvals (
    id               UUID PRIMARY KEY,
    tenant_id        UUID NOT NULL,
    change_id        UUID NOT NULL,
    approver_user_id UUID NOT NULL,
    approval_level   INT  NOT NULL DEFAULT 1,
    decision         VARCHAR(32) NOT NULL,
    comment          VARCHAR(1000),
    decided_at       TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_change_approval_change FOREIGN KEY (change_id)
        REFERENCES change_requests(id) ON DELETE CASCADE,
    CONSTRAINT uk_change_approval_unique UNIQUE (change_id, approver_user_id),
    CONSTRAINT chk_change_approval_decision CHECK (decision IN ('PENDING','APPROVED','REJECTED')),
    CONSTRAINT chk_change_approval_level CHECK (approval_level >= 1)
);

CREATE INDEX idx_change_approval_change   ON change_approvals(change_id);
CREATE INDEX idx_change_approval_approver ON change_approvals(tenant_id, approver_user_id, decision);

COMMENT ON TABLE change_requests IS 'Demandes de changement (§4.8).';
COMMENT ON COLUMN change_requests.status IS
    'DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED → IMPLEMENTED, ou REJECTED/CANCELLED.';
COMMENT ON TABLE change_approvals IS 'Approbations multi-niveaux. UNIQUE (change, approver).';
COMMENT ON TABLE change_impacts IS 'Liens d''impact vers les entités QualitOS impactées.';
