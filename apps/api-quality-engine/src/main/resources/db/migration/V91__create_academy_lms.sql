-- V91: Academy LMS-light + e-learning (CLAUDE.md §19.3)
--
-- Couche CONTENU d'apprentissage : Cours → Modules → Leçons + Quiz (questions),
-- distincte des "training_paths" (matrice de compétences §4.7). Un cours est un
-- parcours e-learning par rôle + secteur ; sa complétion réussie (quiz ≥ seuil)
-- octroie des points de gamification (réutilise training_learner_progress, V87)
-- et génère un certificat signé ML-DSA + ancré blockchain (réutilise l'infra
-- crypto/ancrage du Standards Hub — cf. academy_certificates).
--
-- Multi-tenant strict : tenant_id sur chaque table, jamais lu du body (JWT).

-- ===== Contenu (autoring) =====

CREATE TABLE academy_courses (
    id               UUID PRIMARY KEY,
    tenant_id        UUID NOT NULL,
    code             VARCHAR(100) NOT NULL,
    title            VARCHAR(200) NOT NULL,
    description      VARCHAR(2000),
    target_role      VARCHAR(100),
    industry_sector  VARCHAR(100),
    passing_score    INT NOT NULL DEFAULT 70,
    points_reward    INT NOT NULL DEFAULT 50,
    validity_months  INT,
    status           VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_by       UUID NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_academy_course_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT chk_academy_course_code CHECK (code ~ '^[a-z0-9][a-z0-9_-]{1,99}$'),
    CONSTRAINT chk_academy_course_status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    CONSTRAINT chk_academy_course_passing CHECK (passing_score BETWEEN 0 AND 100),
    CONSTRAINT chk_academy_course_points CHECK (points_reward BETWEEN 0 AND 10000),
    CONSTRAINT chk_academy_course_validity CHECK (validity_months IS NULL OR validity_months BETWEEN 1 AND 120)
);

CREATE INDEX idx_academy_course_tenant ON academy_courses(tenant_id);
CREATE INDEX idx_academy_course_role   ON academy_courses(tenant_id, target_role);
CREATE INDEX idx_academy_course_sector ON academy_courses(tenant_id, industry_sector);

CREATE TABLE academy_modules (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL,
    course_id     UUID NOT NULL,
    title         VARCHAR(200) NOT NULL,
    summary       VARCHAR(1000),
    order_index   INT NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_academy_module_course FOREIGN KEY (course_id)
        REFERENCES academy_courses(id) ON DELETE CASCADE,
    CONSTRAINT uk_academy_module_course_order UNIQUE (course_id, order_index),
    CONSTRAINT chk_academy_module_order CHECK (order_index >= 0)
);

CREATE INDEX idx_academy_module_course ON academy_modules(course_id);

CREATE TABLE academy_lessons (
    id               UUID PRIMARY KEY,
    tenant_id        UUID NOT NULL,
    module_id        UUID NOT NULL,
    title            VARCHAR(200) NOT NULL,
    content_type     VARCHAR(32) NOT NULL DEFAULT 'TEXT',
    body             TEXT,
    media_url        VARCHAR(1000),
    duration_minutes INT NOT NULL DEFAULT 0,
    order_index      INT NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_academy_lesson_module FOREIGN KEY (module_id)
        REFERENCES academy_modules(id) ON DELETE CASCADE,
    CONSTRAINT uk_academy_lesson_module_order UNIQUE (module_id, order_index),
    CONSTRAINT chk_academy_lesson_type CHECK (content_type IN ('TEXT','VIDEO','SIMULATION','EXTERNAL')),
    CONSTRAINT chk_academy_lesson_order CHECK (order_index >= 0),
    CONSTRAINT chk_academy_lesson_duration CHECK (duration_minutes BETWEEN 0 AND 100000)
);

CREATE INDEX idx_academy_lesson_module ON academy_lessons(module_id);

-- Un quiz par module (optionnel) : c'est l'évaluation notée du module.
CREATE TABLE academy_quizzes (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL,
    module_id     UUID NOT NULL,
    title         VARCHAR(200) NOT NULL,
    pass_score    INT NOT NULL DEFAULT 70,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_academy_quiz_module FOREIGN KEY (module_id)
        REFERENCES academy_modules(id) ON DELETE CASCADE,
    CONSTRAINT uk_academy_quiz_module UNIQUE (module_id),
    CONSTRAINT chk_academy_quiz_pass CHECK (pass_score BETWEEN 0 AND 100)
);

CREATE INDEX idx_academy_quiz_module ON academy_quizzes(module_id);

CREATE TABLE academy_quiz_questions (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    quiz_id         UUID NOT NULL,
    stem            VARCHAR(1000) NOT NULL,
    -- Options de réponse, JSON texte (liste de chaines). Pattern projet :
    -- @Column(columnDefinition=TEXT) + @JdbcTypeCode(LONGVARCHAR), jamais @Lob.
    options_json    TEXT NOT NULL,
    correct_index   INT NOT NULL,
    points          INT NOT NULL DEFAULT 1,
    order_index     INT NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_academy_question_quiz FOREIGN KEY (quiz_id)
        REFERENCES academy_quizzes(id) ON DELETE CASCADE,
    CONSTRAINT uk_academy_question_quiz_order UNIQUE (quiz_id, order_index),
    CONSTRAINT chk_academy_question_correct CHECK (correct_index >= 0),
    CONSTRAINT chk_academy_question_points CHECK (points BETWEEN 1 AND 100),
    CONSTRAINT chk_academy_question_order CHECK (order_index >= 0)
);

CREATE INDEX idx_academy_question_quiz ON academy_quiz_questions(quiz_id);

-- ===== Apprentissage (runtime) =====

CREATE TABLE academy_enrollments (
    id                UUID PRIMARY KEY,
    tenant_id         UUID NOT NULL,
    user_id           UUID NOT NULL,
    course_id         UUID NOT NULL,
    status            VARCHAR(32) NOT NULL DEFAULT 'ENROLLED',
    progress_pct      INT NOT NULL DEFAULT 0,
    final_score       INT,
    enrolled_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at        TIMESTAMP WITH TIME ZONE,
    completed_at      TIMESTAMP WITH TIME ZONE,
    expires_on        DATE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_academy_enrollment_course FOREIGN KEY (course_id)
        REFERENCES academy_courses(id) ON DELETE CASCADE,
    CONSTRAINT uk_academy_enrollment_user_course UNIQUE (tenant_id, user_id, course_id),
    CONSTRAINT chk_academy_enrollment_status CHECK (status IN
        ('ENROLLED','IN_PROGRESS','COMPLETED','FAILED','CANCELLED')),
    CONSTRAINT chk_academy_enrollment_progress CHECK (progress_pct BETWEEN 0 AND 100),
    CONSTRAINT chk_academy_enrollment_score CHECK (final_score IS NULL OR final_score BETWEEN 0 AND 100)
);

CREATE INDEX idx_academy_enrollment_user   ON academy_enrollments(tenant_id, user_id);
CREATE INDEX idx_academy_enrollment_course ON academy_enrollments(tenant_id, course_id);

-- Trace de la complétion d'une leçon par un apprenant (idempotent : 1 par leçon).
CREATE TABLE academy_lesson_completions (
    id             UUID PRIMARY KEY,
    tenant_id      UUID NOT NULL,
    enrollment_id  UUID NOT NULL,
    lesson_id      UUID NOT NULL,
    completed_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_academy_lc_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES academy_enrollments(id) ON DELETE CASCADE,
    CONSTRAINT uk_academy_lc_enrollment_lesson UNIQUE (enrollment_id, lesson_id)
);

CREATE INDEX idx_academy_lc_enrollment ON academy_lesson_completions(enrollment_id);

-- Tentative de quiz notée (auto-correction). Plusieurs tentatives possibles.
CREATE TABLE academy_quiz_attempts (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    enrollment_id   UUID NOT NULL,
    quiz_id         UUID NOT NULL,
    score           INT NOT NULL,
    passed          BOOLEAN NOT NULL,
    answers_json    TEXT NOT NULL,
    attempted_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_academy_attempt_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES academy_enrollments(id) ON DELETE CASCADE,
    CONSTRAINT fk_academy_attempt_quiz FOREIGN KEY (quiz_id)
        REFERENCES academy_quizzes(id) ON DELETE CASCADE,
    CONSTRAINT chk_academy_attempt_score CHECK (score BETWEEN 0 AND 100)
);

CREATE INDEX idx_academy_attempt_enrollment ON academy_quiz_attempts(tenant_id, enrollment_id);
CREATE INDEX idx_academy_attempt_quiz       ON academy_quiz_attempts(quiz_id);

-- ===== Certificat signé ML-DSA + ancré blockchain (vérifiable par QR, §19.3) =====
-- code = identifiant public porté par le QR (autorité = le code lui-même).
CREATE TABLE academy_certificates (
    id               UUID PRIMARY KEY,
    tenant_id        UUID NOT NULL,
    user_id          UUID NOT NULL,
    course_id        UUID NOT NULL,
    enrollment_id    UUID NOT NULL,
    code             VARCHAR(64) NOT NULL,
    course_code      VARCHAR(100) NOT NULL,
    course_title     VARCHAR(200) NOT NULL,
    final_score      INT NOT NULL,
    -- Empreinte SHA-256 (hex) du contenu HTML du certificat.
    sha256           CHAR(64) NOT NULL,
    -- Enveloppe de signature hybride (Ed25519 + ML-DSA-65), Base64URL.
    signature        TEXT NOT NULL,
    -- Référence d'ancrage blockchain (stub/Phase A/Fabric).
    anchor_tx_ref    VARCHAR(200),
    issued_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_on       DATE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_academy_cert_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES academy_enrollments(id) ON DELETE CASCADE,
    CONSTRAINT uk_academy_cert_code UNIQUE (code),
    CONSTRAINT uk_academy_cert_enrollment UNIQUE (enrollment_id),
    CONSTRAINT chk_academy_cert_score CHECK (final_score BETWEEN 0 AND 100),
    CONSTRAINT chk_academy_cert_sha256 CHECK (sha256 ~ '^[0-9a-f]{64}$')
);

CREATE INDEX idx_academy_cert_code ON academy_certificates(code);
CREATE INDEX idx_academy_cert_user ON academy_certificates(tenant_id, user_id);

COMMENT ON TABLE academy_courses IS 'Cours e-learning (parcours par rôle/secteur) — §19.3.';
COMMENT ON TABLE academy_modules IS 'Modules ordonnés d''un cours.';
COMMENT ON TABLE academy_lessons IS 'Leçons ordonnées d''un module (texte/vidéo/simulation).';
COMMENT ON TABLE academy_quizzes IS 'Quiz noté d''un module (seuil de réussite).';
COMMENT ON TABLE academy_quiz_questions IS 'Questions de quiz (QCM auto-corrigé).';
COMMENT ON TABLE academy_enrollments IS 'Inscription d''un apprenant à un cours + progression.';
COMMENT ON TABLE academy_lesson_completions IS 'Complétion de leçon (idempotent).';
COMMENT ON TABLE academy_quiz_attempts IS 'Tentatives de quiz notées (auto-correction).';
COMMENT ON TABLE academy_certificates IS 'Certificats de complétion signés ML-DSA + ancrés blockchain, vérifiables par QR (§19.3).';
