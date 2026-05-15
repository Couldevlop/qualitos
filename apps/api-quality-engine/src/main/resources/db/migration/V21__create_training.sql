-- V21: Training & Competency (CLAUDE.md §4.7)

CREATE TABLE training_skills (
    id           UUID PRIMARY KEY,
    tenant_id    UUID NOT NULL,
    code         VARCHAR(100) NOT NULL,
    name         VARCHAR(200) NOT NULL,
    description  VARCHAR(1000),
    category     VARCHAR(64),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_training_skill_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT chk_training_skill_code CHECK (code ~ '^[a-z0-9][a-z0-9_-]{1,99}$')
);

CREATE INDEX idx_training_skill_tenant_category ON training_skills(tenant_id, category);

CREATE TABLE training_user_skills (
    id           UUID PRIMARY KEY,
    tenant_id    UUID NOT NULL,
    user_id      UUID NOT NULL,
    skill_id     UUID NOT NULL,
    level        INT  NOT NULL,
    source       VARCHAR(32) NOT NULL,
    assessed_by  UUID,
    assessed_at  DATE NOT NULL,
    expires_on   DATE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_user_skill_skill FOREIGN KEY (skill_id)
        REFERENCES training_skills(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_skill_unique UNIQUE (tenant_id, user_id, skill_id),
    CONSTRAINT chk_user_skill_level CHECK (level BETWEEN 0 AND 4),
    CONSTRAINT chk_user_skill_source CHECK (source IN
        ('SELF','MANAGER','TRAINING','CERTIFICATION','AUDIT'))
);

CREATE INDEX idx_user_skill_tenant_user ON training_user_skills(tenant_id, user_id);
CREATE INDEX idx_user_skill_skill       ON training_user_skills(skill_id, level);

CREATE TABLE training_paths (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    code            VARCHAR(100) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    description     VARCHAR(2000),
    target_role     VARCHAR(100),
    duration_hours  INT NOT NULL,
    passing_score   INT NOT NULL DEFAULT 70,
    validity_months INT,
    status          VARCHAR(32) NOT NULL,
    created_by      UUID NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_training_path_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT chk_training_path_status CHECK (status IN ('DRAFT','ACTIVE','ARCHIVED')),
    CONSTRAINT chk_training_path_duration CHECK (duration_hours BETWEEN 1 AND 10000),
    CONSTRAINT chk_training_path_passing CHECK (passing_score BETWEEN 0 AND 100),
    CONSTRAINT chk_training_path_validity CHECK (validity_months IS NULL OR validity_months BETWEEN 1 AND 120)
);

CREATE INDEX idx_training_path_tenant ON training_paths(tenant_id);
CREATE INDEX idx_training_path_role   ON training_paths(tenant_id, target_role);

CREATE TABLE training_path_skill_requirements (
    id           UUID PRIMARY KEY,
    tenant_id    UUID NOT NULL,
    path_id      UUID NOT NULL,
    skill_id     UUID NOT NULL,
    target_level INT  NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_tpsr_path  FOREIGN KEY (path_id)  REFERENCES training_paths(id)  ON DELETE CASCADE,
    CONSTRAINT fk_tpsr_skill FOREIGN KEY (skill_id) REFERENCES training_skills(id) ON DELETE CASCADE,
    CONSTRAINT uk_tpsr_path_skill UNIQUE (path_id, skill_id),
    CONSTRAINT chk_tpsr_level CHECK (target_level BETWEEN 0 AND 4)
);

CREATE INDEX idx_tpsr_path ON training_path_skill_requirements(path_id);

CREATE TABLE training_enrollments (
    id               UUID PRIMARY KEY,
    tenant_id        UUID NOT NULL,
    user_id          UUID NOT NULL,
    path_id          UUID NOT NULL,
    status           VARCHAR(32) NOT NULL,
    progress_pct     INT NOT NULL DEFAULT 0,
    final_score      INT,
    enrolled_on      DATE NOT NULL,
    started_on       DATE,
    completed_on     DATE,
    expires_on       DATE,
    certificate_code VARCHAR(36),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_enrollment_path FOREIGN KEY (path_id) REFERENCES training_paths(id) ON DELETE CASCADE,
    CONSTRAINT uk_enrollment_user_path UNIQUE (tenant_id, user_id, path_id),
    CONSTRAINT uk_enrollment_cert_code UNIQUE (certificate_code),
    CONSTRAINT chk_enrollment_status CHECK (status IN
        ('ENROLLED','IN_PROGRESS','COMPLETED','FAILED','CANCELLED')),
    CONSTRAINT chk_enrollment_progress CHECK (progress_pct BETWEEN 0 AND 100),
    CONSTRAINT chk_enrollment_score CHECK (final_score IS NULL OR final_score BETWEEN 0 AND 100)
);

CREATE INDEX idx_enrollment_user ON training_enrollments(tenant_id, user_id);
CREATE INDEX idx_enrollment_cert ON training_enrollments(certificate_code);

COMMENT ON TABLE training_skills IS 'Catalogue de compétences référençables (§4.7).';
COMMENT ON TABLE training_user_skills IS 'Matrice de compétences par utilisateur.';
COMMENT ON TABLE training_paths IS 'Parcours de formation par rôle.';
COMMENT ON TABLE training_enrollments IS 'Inscriptions + certificats (UUID code public, ancrage blockchain à venir).';
