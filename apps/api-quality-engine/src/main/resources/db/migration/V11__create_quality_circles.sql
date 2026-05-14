CREATE TABLE quality_circles (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    topic       VARCHAR(255),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_quality_circles PRIMARY KEY (id),
    CONSTRAINT chk_quality_circles_status CHECK (status IN ('ACTIVE','PAUSED','ARCHIVED'))
);

CREATE INDEX idx_quality_circles_tenant ON quality_circles (tenant_id);
CREATE INDEX idx_quality_circles_tenant_status ON quality_circles (tenant_id, status);

CREATE TABLE circle_members (
    id        UUID        NOT NULL DEFAULT gen_random_uuid(),
    circle_id UUID        NOT NULL,
    user_id   UUID        NOT NULL,
    role      VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_circle_members PRIMARY KEY (id),
    CONSTRAINT fk_circle_members_circle FOREIGN KEY (circle_id)
        REFERENCES quality_circles (id) ON DELETE CASCADE,
    CONSTRAINT uk_circle_member_user UNIQUE (circle_id, user_id),
    CONSTRAINT chk_circle_member_role CHECK (role IN ('FACILITATOR','SECRETARY','MEMBER'))
);

CREATE INDEX idx_circle_members_circle ON circle_members (circle_id);
CREATE INDEX idx_circle_members_user ON circle_members (user_id);

CREATE TABLE circle_meetings (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    circle_id        UUID         NOT NULL,
    title            VARCHAR(255) NOT NULL,
    agenda           TEXT,
    scheduled_at     TIMESTAMPTZ  NOT NULL,
    duration_minutes INTEGER,
    location         VARCHAR(500),
    status           VARCHAR(20)  NOT NULL DEFAULT 'PLANNED',
    minutes          TEXT,
    held_at          TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_circle_meetings PRIMARY KEY (id),
    CONSTRAINT fk_circle_meetings_circle FOREIGN KEY (circle_id)
        REFERENCES quality_circles (id) ON DELETE CASCADE,
    CONSTRAINT chk_circle_meetings_status CHECK (status IN ('PLANNED','HELD','CANCELLED'))
);

CREATE INDEX idx_circle_meetings_circle ON circle_meetings (circle_id);
CREATE INDEX idx_circle_meetings_scheduled ON circle_meetings (circle_id, scheduled_at);

CREATE TABLE circle_proposals (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    circle_id         UUID         NOT NULL,
    meeting_id        UUID,
    title             VARCHAR(255) NOT NULL,
    description       TEXT,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PROPOSED',
    proposed_by       UUID         NOT NULL,
    validated_by      UUID,
    validated_at      TIMESTAMPTZ,
    implemented_at    TIMESTAMPTZ,
    measured_at       TIMESTAMPTZ,
    impact_note       TEXT,
    rejection_reason  TEXT,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_circle_proposals PRIMARY KEY (id),
    CONSTRAINT fk_circle_proposals_circle FOREIGN KEY (circle_id)
        REFERENCES quality_circles (id) ON DELETE CASCADE,
    CONSTRAINT fk_circle_proposals_meeting FOREIGN KEY (meeting_id)
        REFERENCES circle_meetings (id) ON DELETE SET NULL,
    CONSTRAINT chk_circle_proposals_status CHECK (
        status IN ('PROPOSED','UNDER_REVIEW','APPROVED','REJECTED','IMPLEMENTED','MEASURED')
    )
);

CREATE INDEX idx_circle_proposals_circle ON circle_proposals (circle_id);
CREATE INDEX idx_circle_proposals_circle_status ON circle_proposals (circle_id, status);
CREATE INDEX idx_circle_proposals_meeting ON circle_proposals (meeting_id);
