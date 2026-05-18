# STATUS — Phase P5 sprint-1

Branch: `feature/p5-foundations`
Sprint window: 2026-05-17 (delivered in one sprint by a single agent)

## What ships in this sprint

### 1. AI service (`apps/ai-service/`, Python 3.13, FastAPI on port 8085)

- Hexagonal architecture: `domain/` (entities + ports + pure services),
  `application/usecase/` (orchestrators), `infrastructure/` (adapters),
  `presentation/` (FastAPI routers + Pydantic + DI container).
- `AIProvider` port + 3 adapters: `OllamaProvider`, `AnthropicProvider`,
  `MistralProvider`. Host allow-list, env-only API keys.
- `POST /v1/ai/complete` — guarded completion (PII redaction, prompt-injection
  filter, audit log).
- `POST /v1/ai/rag/query` — RAG over the tenant's collection in Qdrant
  (or in-memory fallback). Embeddings via BGE-M3 (pinned revision).
- `POST /v1/ai/nlq/ask` — Natural Language Query. sqlglot validator + read-only
  Postgres executor + ECharts spec inference + narrative.
- `POST /v1/ai/federated/round` — Flower scaffold, opt-in only.
- `GET /healthz`, `/readyz`.
- Distroless Dockerfile, `pyproject.toml` with `import-linter` contract that
  forbids framework imports from `domain/`.

### 2. NLQ safety (defence in depth)

- 35 adversarial test cases in `tests/nlq_adversarial/`.
- Validator enforces SELECT-only, table allow-list, function allow-list,
  no system schemas, single statement, tenant_id placeholder binding,
  LIMIT cap 10 000.
- Cross-tenant literal rejected; missing tenant filter auto-rewritten.

### 3. Dashboards builder (frontend + backend)

- Angular: `DashboardBuilderModule` (lazy, NgModule, separate HTML/SCSS).
  Clean Architecture inside the feature (domain/application/infrastructure/
  presentation). Premium Material 3 styling.
- Backend: Flyway V50 + `DashboardLayout` aggregate + use cases + JPA
  adapter + REST `/api/v1/dashboards/custom`.
- `signature_hash` placeholder for ML-DSA + Hyperledger Fabric anchor.

### 4. Marketplace data model

- Flyway V51 + `MarketplacePack` aggregate (verified/unverified, signature
  hash, https/oci manifest URL).
- REST `/api/v1/marketplace/packs` — list (any user, verified only),
  register (super-admin only), verify (super-admin only).

### 5. Federated learning scaffold

- `FederatedConfig.from_env()` reads `QOS_FEDERATED_OPTIN_TENANTS`.
- `OptInFederatedClient` enforces hard opt-in gate; non-opted tenants → 403.
- No real cross-tenant training in sprint-1.

### 6. Docker compose

- `ai-service` (8085), `qdrant` (6333/6334), `ollama` (11434) added.
- Existing services untouched.

### 7. Documentation

- ADRs 0008 (AI architecture), 0009 (NLQ safety), 0010 (dashboards builder).
- Threat models: `docs/security/threat-model-ai-service.md`,
  `docs/security/threat-model-nlq.md`.
- `docs/security/owasp-checklist-p5.md` — full OWASP Top 10 + API Top 10 +
  **LLM Top 10** coverage.

## Tests

| Suite                              | Count | Status      |
| ---------------------------------- | ----- | ----------- |
| ai-service (pytest, total)         | 112   | passing     |
| of which adversarial NLQ           | 35    | passing     |
| coverage                           | 86%   | ≥ 85% gate  |
| api-quality-engine (new tests)     | 31    | passing     |
| dashboard-builder Karma specs      | 19    | shipped     |

## What is NOT done (sprint-2 backlog)

- Real ML-DSA signature + Hyperledger Fabric anchor on dashboard layouts
  and marketplace packs.
- Per-tenant rate-limit (slowapi) on AI endpoints.
- mTLS between ai-service ↔ Qdrant ↔ Postgres.
- SBOM CycloneDX in CI + image signing.
- Real federated training (Flower server + Opacus DP enforcement).
- Marketplace UI in Angular.
- ngx-echarts wiring in widgets (placeholder ships, dependency added to
  `package.json`).
- angular-gridster2 wiring in editor (placeholder grid ships, dependency
  added).
- Karma run + Java mvn full build in CI verified.

## Conflict surface with parallel agents

- **P3 agent** (IoT + Industry Packs) — no overlap. P3 owns `iot/`,
  `industry/` packages and migrations V17-V18 already merged.
- **P4 agent** (Standards Hub extension + connectors) — no overlap. P4 owns
  `standards/`, `itsm/`, `webhooks/`. New migrations V52+ collide-free.
- **docker-compose** — added `ai-service`, `qdrant`, `ollama`. New ports
  (8085, 6333, 6334, 11434) — no clash with existing 4200/8080/8081/8082/5432.
- **api-quality-engine** — new packages `dashboards/`, `marketplace/`; new
  migrations V50/V51. `GlobalExceptionHandler` extended (additive).
- **web** — new lazy module `dashboard-builder/`; route appended. No edit
  to existing modules.
