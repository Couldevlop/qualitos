-- AI Act (UE 2024/1689) — Post-Market Monitoring plans (Art. 72).
-- Plans de surveillance post-marché des systèmes IA.

CREATE TABLE ai_act_pmm_plans (
    id                              UUID         NOT NULL,
    tenant_id                       UUID         NOT NULL,
    reference                       VARCHAR(64)  NOT NULL,
    ai_system_id                    UUID         NOT NULL,
    name                            VARCHAR(250) NOT NULL,
    description                     VARCHAR(4000),
    metrics_monitored               VARCHAR(4000),
    collection_method               VARCHAR(4000),
    review_frequency                VARCHAR(32),
    responsible_party_description   VARCHAR(4000),
    trigger_criteria                VARCHAR(4000),
    qms_link_reference              VARCHAR(250),
    status                          VARCHAR(32)  NOT NULL,
    activated_at                    TIMESTAMPTZ,
    last_reviewed_at                TIMESTAMPTZ,
    last_reviewed_by                UUID,
    suspended_at                    TIMESTAMPTZ,
    suspension_reason               VARCHAR(2000),
    effective_to                    TIMESTAMPTZ,
    closure_reason                  VARCHAR(2000),
    created_by                      UUID         NOT NULL,
    created_at                      TIMESTAMPTZ  NOT NULL,
    updated_at                      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_ai_act_pmm_plans PRIMARY KEY (id),
    CONSTRAINT uq_pmm_tenant_reference UNIQUE (tenant_id, reference),
    CONSTRAINT chk_pmm_status CHECK
        (status IN ('DRAFT','ACTIVE','SUSPENDED','CLOSED')),
    CONSTRAINT chk_pmm_review_frequency CHECK
        (review_frequency IS NULL
         OR review_frequency IN ('WEEKLY','MONTHLY','QUARTERLY','SEMI_ANNUAL','ANNUAL')),
    CONSTRAINT chk_pmm_reference CHECK
        (reference ~ '^[A-Z][A-Z0-9_-]{1,63}$'),
    -- ACTIVE/SUSPENDED/CLOSED ⇒ metrics + collection + frequency requis
    CONSTRAINT chk_pmm_active_has_metadata CHECK
        (status = 'DRAFT'
         OR (metrics_monitored IS NOT NULL
             AND length(trim(metrics_monitored)) > 0
             AND collection_method IS NOT NULL
             AND length(trim(collection_method)) > 0
             AND review_frequency IS NOT NULL)),
    -- ACTIVE/SUSPENDED/CLOSED ⇒ activated_at requis
    CONSTRAINT chk_pmm_lifecycle_has_activation CHECK
        (status = 'DRAFT' OR activated_at IS NOT NULL),
    -- SUSPENDED ⇒ suspended_at + reason
    CONSTRAINT chk_pmm_suspended_has_reason CHECK
        (status <> 'SUSPENDED'
         OR (suspended_at IS NOT NULL
             AND suspension_reason IS NOT NULL
             AND length(trim(suspension_reason)) > 0)),
    -- CLOSED ⇒ effective_to + closure_reason
    CONSTRAINT chk_pmm_closed_has_to_and_reason CHECK
        (status <> 'CLOSED'
         OR (effective_to IS NOT NULL
             AND closure_reason IS NOT NULL
             AND length(trim(closure_reason)) > 0))
);

CREATE INDEX idx_pmm_tenant        ON ai_act_pmm_plans (tenant_id);
CREATE INDEX idx_pmm_tenant_status ON ai_act_pmm_plans (tenant_id, status);
CREATE INDEX idx_pmm_tenant_system ON ai_act_pmm_plans (tenant_id, ai_system_id);

-- Index partiel : plans actifs — pour le scan des revues en retard.
CREATE INDEX idx_pmm_active_review
    ON ai_act_pmm_plans (tenant_id, COALESCE(last_reviewed_at, activated_at))
    WHERE status = 'ACTIVE';

COMMENT ON TABLE  ai_act_pmm_plans IS
    'AI Act (UE 2024/1689 Art. 72) Post-market monitoring plans.';
COMMENT ON COLUMN ai_act_pmm_plans.review_frequency IS
    'Determines next_review_due_at = COALESCE(last_reviewed_at, activated_at) + period.';
