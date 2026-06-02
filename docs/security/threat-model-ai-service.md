# Threat model — ai-service (P5 sprint-1)

STRIDE applied to the AI gateway (`apps/ai-service`).

## Assets

1. Tenant prompts and responses (may contain regulated quality data).
2. Tenant corpus indexed in Qdrant (collection-per-tenant).
3. AI provider API keys (Anthropic, Mistral).
4. Prompt audit log (must be append-only, redacted).
5. JWT keys (Keycloak JWKS).

## Trust boundaries

- HTTP boundary (`/v1/ai/**`) — JWT validation (OAuth2/OIDC, Keycloak).
- AI provider boundary — outbound HTTPS to allow-listed hosts only.
- Qdrant boundary — internal, mTLS in prod (sprint-2).
- Postgres boundary — read-only role for NLQ executor only.

## STRIDE

| Threat                 | Vector                                        | Mitigation                                                                 |
| ---------------------- | --------------------------------------------- | -------------------------------------------------------------------------- |
| **S**poofing           | Forged JWT, expired token                     | Keycloak JWKS + signature + iss + aud + exp validation (`A07`)             |
|                        | Substituted dev-claims                        | `QOS_DEV_AUTH` must be unset in prod; checked at startup                   |
| **T**ampering          | Mutated prompt log                            | Append-only logger, future blockchain anchor                               |
|                        | Vector payload tampering                      | Per-tenant collection, signed firmware on edge gateways (sprint-2)         |
| **R**epudiation        | "I didn't ask that"                           | Audit log entry with `correlation_id`, `user_id`, `tenant_id`, redacted    |
| **I**nformation        | Cross-tenant RAG leakage                      | Hard tenant isolation: collection-per-tenant + `tenant_id` filter on every search |
|                        | PII in logs                                   | Presidio redaction BEFORE logger receives the entry (`LLM06`)              |
|                        | Cross-tenant NLQ exfil                        | sqlglot validator rejects literal tenant_id; tenant filter rewriter (`LLM02`/`LLM08`) |
|                        | LLM-side data exfil to external provider      | AIProvider allow-list (api.anthropic.com, api.mistral.ai, local Ollama)    |
|                        | Prompt-injection coaxing system prompt        | PromptInjectionFilter; system/user role separation                         |
| **D**enial of Service  | Massive prompt                                | FastAPI body size, max_tokens cap, slowapi (sprint-2) **+ garde-fou par tenant côté engine : `AiGuard` débit/min + quota journalier + borne de taille de prompt (ADR 0017)** |
|                        | Slow provider                                 | httpx timeout 30s, circuit breaker ai-service (sprint-2) **+ disjoncteur par tenant côté engine (`AiGuard`, ADR 0017)** |
|                        | NLQ runaway query                             | statement_timeout 5s, LIMIT cap 10 000, row cap 1 000                      |
| **E**levation          | Federated training abuse                      | Hard opt-in gate; non-opted-in tenant raises 403                           |
|                        | Tool-use sandbox escape                       | No shell exec, no file write, no eval in adapters                          |

## Open issues for sprint-2

- mTLS to Qdrant + Postgres.
- Rate-limit per tenant (slowapi).
- Per-provider circuit breaker.
- SBOM CycloneDX + image signing.
- Real ML-DSA signature on prompt audit log.
- Federated DP epsilon enforcement (Opacus).
