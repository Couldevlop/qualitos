-- Routage IA par tenant (ADR 0102, CLAUDE.md §12.2 souveraineté).
--
-- Le fournisseur auquel un tenant épingle ses appels LLM. Un tenant régulé
-- (santé, défense, finance) choisit `ollama`, servi dans l'infrastructure du
-- client : ses prompts, et les données qu'ils portent, ne partent vers aucun
-- tiers. Sans ligne, le tenant suit le défaut de la plateforme
-- (AI_DEFAULT_PROVIDER de l'ai-service).
--
-- tenant_id et updated_by viennent du jeton, jamais du corps (§18.2). La
-- politique RLS est posée au démarrage sur toute table portant tenant_id
-- (ADR 0086).

CREATE TABLE tenant_ai_policy (
    tenant_id   UUID         NOT NULL,
    provider    VARCHAR(20)  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    updated_by  UUID         NOT NULL,

    CONSTRAINT pk_tenant_ai_policy PRIMARY KEY (tenant_id),
    CONSTRAINT ck_tenant_ai_policy_provider CHECK (provider IN ('ollama', 'anthropic', 'mistral'))
);

COMMENT ON TABLE tenant_ai_policy IS
    'Fournisseur des appels LLM du tenant (ADR 0102). Absente : défaut de la plateforme.';
