-- V19: Risk / FMEA (CLAUDE.md §4.5)

CREATE TABLE fmea_projects (
    id                       UUID PRIMARY KEY,
    tenant_id                UUID NOT NULL,
    code                     VARCHAR(120) NOT NULL,
    name                     VARCHAR(250) NOT NULL,
    scope                    VARCHAR(1000),
    type                     VARCHAR(32)  NOT NULL,
    status                   VARCHAR(32)  NOT NULL,
    critical_rpn_threshold   INT NOT NULL DEFAULT 100,
    revision                 INT NOT NULL DEFAULT 1,
    owner_user_id            UUID,
    last_reviewed_at         TIMESTAMP WITH TIME ZONE,
    created_by               UUID NOT NULL,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_fmea_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT chk_fmea_type CHECK (type IN (
        'PROCESS_FMEA','DESIGN_FMEA','SYSTEM_FMEA','SERVICE_FMEA','BOW_TIE')),
    CONSTRAINT chk_fmea_status CHECK (status IN ('DRAFT','ACTIVE','ARCHIVED')),
    CONSTRAINT chk_fmea_threshold CHECK (critical_rpn_threshold BETWEEN 1 AND 1000),
    CONSTRAINT chk_fmea_revision CHECK (revision >= 1)
);

CREATE INDEX idx_fmea_tenant         ON fmea_projects(tenant_id);
CREATE INDEX idx_fmea_tenant_status  ON fmea_projects(tenant_id, status);

CREATE TABLE fmea_items (
    id                     UUID PRIMARY KEY,
    tenant_id              UUID NOT NULL,
    project_id             UUID NOT NULL,
    sequence_no            INT NOT NULL,
    function_text          VARCHAR(500),
    failure_mode           VARCHAR(500),
    failure_effect         VARCHAR(500),
    failure_cause          VARCHAR(1000),
    current_controls       VARCHAR(1000),
    severity               INT NOT NULL,
    occurrence             INT NOT NULL,
    detection              INT NOT NULL,
    rpn                    INT NOT NULL,
    recommended_action     VARCHAR(1000),
    action_owner_user_id   UUID,
    action_due_date        DATE,
    resulting_severity     INT,
    resulting_occurrence   INT,
    resulting_detection    INT,
    rpn_after              INT,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_fmea_item_project FOREIGN KEY (project_id)
        REFERENCES fmea_projects(id) ON DELETE CASCADE,
    CONSTRAINT chk_fmea_severity   CHECK (severity   BETWEEN 1 AND 10),
    CONSTRAINT chk_fmea_occurrence CHECK (occurrence BETWEEN 1 AND 10),
    CONSTRAINT chk_fmea_detection  CHECK (detection  BETWEEN 1 AND 10),
    CONSTRAINT chk_fmea_rpn        CHECK (rpn = severity * occurrence * detection),
    CONSTRAINT chk_fmea_res_severity   CHECK (resulting_severity   IS NULL OR resulting_severity   BETWEEN 1 AND 10),
    CONSTRAINT chk_fmea_res_occurrence CHECK (resulting_occurrence IS NULL OR resulting_occurrence BETWEEN 1 AND 10),
    CONSTRAINT chk_fmea_res_detection  CHECK (resulting_detection  IS NULL OR resulting_detection  BETWEEN 1 AND 10)
);

CREATE INDEX idx_fmea_item_project ON fmea_items(project_id, sequence_no);
CREATE INDEX idx_fmea_item_rpn     ON fmea_items(tenant_id, rpn);

COMMENT ON TABLE fmea_projects IS 'Projets FMEA (Process/Design/System/Service/Bow-tie).';
COMMENT ON TABLE fmea_items    IS 'Lignes FMEA. RPN auto-calculé côté serveur (CHECK SQL en doublon).';
