-- V69 : Notifications in-app (CLAUDE.md §15 UX). Par utilisateur (recipient_user_id)
-- ou diffusion à tout le tenant (recipient_user_id NULL). Isolation tenant (OWASP A01).

CREATE TABLE notifications (
    id                 UUID PRIMARY KEY,
    tenant_id          UUID NOT NULL,
    recipient_user_id  VARCHAR(128),
    type               VARCHAR(16) NOT NULL,
    title              VARCHAR(255) NOT NULL,
    body               TEXT,
    link               VARCHAR(2048),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    read_at            TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_notification_type CHECK (type IN ('INFO', 'SUCCESS', 'WARNING', 'ALERT'))
);

-- Lecture du flux (récentes par destinataire/diffusion) + compteur non-lues.
CREATE INDEX idx_notifications_tenant_recipient_created
    ON notifications (tenant_id, recipient_user_id, created_at DESC);
CREATE INDEX idx_notifications_unread
    ON notifications (tenant_id, recipient_user_id)
    WHERE read_at IS NULL;

COMMENT ON TABLE notifications IS 'Notifications in-app par utilisateur ou diffusion tenant.';
