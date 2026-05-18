# ADR 0008 — AI service architecture (P5)

Status: Accepted (P5 sprint-1, 2026-05-17)

## Context

QualitOS needs an AI gateway to deliver the P5 promises: predictive KPIs, RAG
over the tenant corpus, Natural Language Query, federated learning. CLAUDE.md
§12 mandates the **AIProvider** abstraction (no direct external calls), §12.2
mandates Qdrant for RAG, §11.2 mandates OWASP LLM Top 10 hardening.

## Decision

A dedicated **ai-service** lives under `apps/ai-service/`, written in Python
3.13 with FastAPI, packaged as a **distroless** image on port **8085**.
It follows **strict Hexagonal Architecture**:

- `domain/` — entities + ports + pure services. Zero framework imports.
- `application/usecase/` — orchestrators using ports only.
- `infrastructure/` — concrete adapters (Ollama, Anthropic, Mistral, Qdrant,
  Presidio, sqlglot, Keycloak JWKS, psycopg).
- `presentation/` — FastAPI routers + Pydantic schemas + DI container.
- `federated/` — Flower client scaffold (opt-in only, OFF by default).

Ports are pure ABCs (`AIProvider`, `VectorStore`, `PiiFilter`,
`PromptInjectionFilter`, `SqlValidator`, `ReadOnlySqlExecutor`,
`PromptAuditLogger`, `Embedder`, `FederatedClient`). An `import-linter`
contract in `pyproject.toml` forbids domain code from importing FastAPI,
SQLAlchemy, sqlglot, presidio, qdrant, httpx, etc.

## Consequences

- Adapters can be swapped (Anthropic-only deployments, on-prem Mistral via
  Ollama, in-memory test doubles) without touching domain or application.
- A second backend (Java) could re-implement the same ports if needed for
  perf-critical paths.
- Coverage gates apply only to deterministic layers (domain, application,
  guardrails, sql validator). External adapters need integration tests with
  Testcontainers in P5-sprint-2.

## Alternatives rejected

- LangChain as the orchestration spine — too leaky, too many implicit calls,
  prompt visibility opaque.
- Spring AI inside `api-quality-engine` — couples ML lifecycle (Python-centric)
  to a Java release train. Rejected.

## References

- CLAUDE.md §12 (AI architecture), §18.2 (no external AI call without
  AIProvider gateway), §11.2 (OWASP LLM Top 10).
- Files: `apps/ai-service/domain/`, `apps/ai-service/application/`,
  `apps/ai-service/infrastructure/`, `apps/ai-service/presentation/`.
