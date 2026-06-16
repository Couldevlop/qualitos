-- V87: Gamification de la formation (CLAUDE.md §4.7 + §19.3)
-- Une ligne par apprenant (tenant_id, user_id) : points cumulés, ceinture
-- (WHITE/YELLOW/GREEN/BLACK), badges (CSV), meilleur score et compteur de
-- complétions. Ceinture et badges sont dérivés des points/complétions par des
-- règles pures côté domaine — persistés ici pour servir la lecture sans recalcul.

CREATE TABLE training_learner_progress (
    id               UUID PRIMARY KEY,
    tenant_id        UUID NOT NULL,
    user_id          UUID NOT NULL,
    points           INT NOT NULL DEFAULT 0,
    completed_count  INT NOT NULL DEFAULT 0,
    best_score       INT,
    belt_level       VARCHAR(16) NOT NULL DEFAULT 'WHITE',
    badges           VARCHAR(512) NOT NULL DEFAULT '',
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_learner_progress_tenant_user UNIQUE (tenant_id, user_id),
    CONSTRAINT chk_learner_progress_points CHECK (points >= 0),
    CONSTRAINT chk_learner_progress_completed CHECK (completed_count >= 0),
    CONSTRAINT chk_learner_progress_best_score CHECK (best_score IS NULL OR best_score BETWEEN 0 AND 100),
    CONSTRAINT chk_learner_progress_belt CHECK (belt_level IN ('WHITE','YELLOW','GREEN','BLACK'))
);

CREATE INDEX idx_learner_progress_tenant_points
    ON training_learner_progress(tenant_id, points);

COMMENT ON TABLE training_learner_progress IS
    'Progression de gamification par apprenant (points, ceinture, badges) — §19.3.';
