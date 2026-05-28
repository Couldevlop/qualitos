# OWASP checklist — P5 sprint-1

Covers OWASP Top 10 2021, OWASP API Security Top 10 2023, and **explicitly the
OWASP Top 10 for LLM Applications 2025** (the AI-service surface).

## OWASP Top 10 2021

| Item                                | Status | Where                                                                                  |
| ----------------------------------- | ------ | -------------------------------------------------------------------------------------- |
| A01 Broken Access Control           | DONE   | tenant filter from JWT only; cross-tenant access returns 404; `@PreAuthorize` / `Depends(require_role)` |
| A02 Cryptographic Failures          | DONE   | TLS terminator on gateway; secrets via env only; no secrets in repo                    |
| A03 Injection                       | DONE   | NLQ defended in depth (see ADR 0009); JPA parameter binding everywhere                 |
| A04 Insecure Design                 | DONE   | STRIDE threat models for ai-service & NLQ                                              |
| A05 Security Misconfiguration       | DONE   | distroless image; `/docs` hidden in prod (`QOS_ENV=prod`); restricted Spring actuator  |
| A06 Vulnerable & Outdated Comp.     | TODO   | SBOM CycloneDX + Trivy/Grype scan in CI (sprint-2)                                     |
| A07 Identification & Auth Failures  | DONE   | Keycloak JWKS validation (signature + iss + aud + exp); dev mode behind env var        |
| A08 Software & Data Integrity       | PART   | `signature_hash` placeholder in `dashboard_layouts` + `marketplace_packs`; ML-DSA sign + Fabric anchor wired in sprint-2 |
| A09 Logging & Monitoring            | DONE   | Structured JSON, Presidio redaction BEFORE logger receives entries                     |
| A10 SSRF                            | DONE   | AIProvider allow-list (api.anthropic.com, api.mistral.ai, local Ollama)                |

## OWASP Top 10 for LLM Applications 2025 — load-bearing for ai-service

| Item                                  | Status | Control                                                                                                                                  |
| ------------------------------------- | ------ | ---------------------------------------------------------------------------------------------------------------------------------------- |
| LLM01 Prompt Injection                | DONE   | `HeuristicInjectionFilter` (11 patterns, threshold 0.6); separate system/user roles; 13 adversarial unit tests pass                      |
| LLM02 Insecure Output Handling        | DONE   | NLQ output goes through `SqlglotValidator` → SELECT-only → table allow-list → tenant filter; 35 adversarial NLQ tests pass               |
| LLM03 Training Data Poisoning         | TODO   | RAG corpus signed in sprint-2 (dataset hash in vector payload)                                                                           |
| LLM04 Model DoS                       | PART   | httpx timeout 30s, `max_tokens` cap; slowapi rate-limit per tenant in sprint-2                                                           |
| LLM05 Supply Chain                    | PART   | BGE-M3 model pinned by revision (`BGE_M3_REVISION` env); SBOM in sprint-2                                                                |
| LLM06 Sensitive Information Disclosure| DONE   | PII scrubbing on INPUT and OUTPUT before `PromptAuditLogger`; runtime uses `HeuristicPiiFilter` (Presidio excluded from the distroless image — ADR 0013; re-enable in prod by bundling spaCy) |
| LLM07 Insecure Plugin Design          | DONE   | No tool-use in sprint-1; AIProvider host allow-list; tests `test_provider_allowlist.py`                                                  |
| LLM08 Excessive Agency                | DONE   | NLQ runs only SELECT through a separate Postgres role with `SELECT`-only grants and `SET TRANSACTION READ ONLY`; statement_timeout 5s    |
| LLM09 Overreliance                    | DONE   | Every completion exposes `confidence` (with `confidence_method`); RAG result lists `citations[document_id, score]`                       |
| LLM10 Model Theft                     | PART   | No `/models/download` endpoint; rate-limit per tenant in sprint-2                                                                        |

## OWASP API Security Top 10 2023

| Item                                       | Status | Where                                                                                |
| ------------------------------------------ | ------ | ------------------------------------------------------------------------------------ |
| API1 BOLA                                  | DONE   | Owner check in `DashboardLayoutService.loadForUser/loadForVisibility`                |
| API2 Broken Authentication                 | DONE   | Keycloak JWKS validation in ai-service AND api-quality-engine                        |
| API3 Broken Object Property Level Auth.    | DONE   | Pydantic / Jakarta-Validation schemas reject unknown fields; tenantId never in body  |
| API4 Unrestricted Resource Consumption     | PART   | NLQ row cap, LIMIT cap, max_tokens; per-tenant rate limit in sprint-2                |
| API5 Broken Function Level Auth.           | DONE   | `KeycloakSuperAdminProvider` for marketplace register/verify                         |
| API6 Unrestricted Access to Sensitive Flow | DONE   | Federated learning OFF by default; explicit opt-in via env var                       |
| API7 SSRF                                  | DONE   | AIProvider host allow-list; `manifestUrl` must be https/oci, validated in domain     |
| API8 Security Misconfiguration             | DONE   | distroless; non-root user; /docs hidden in prod                                      |
| API9 Improper Inventory Management         | DONE   | OpenAPI auto-published in dev; versioned routes `/v1/`                                |
| API10 Unsafe Consumption of APIs           | DONE   | AIProvider adapters validate HTTP status; httpx timeout                              |

## Update 2026-05-28 — new surfaces (NLQ engine relay, Fabric anchoring, hybrid TLS)

This session added attack surfaces in **api-quality-engine**; controls below.

### NLQ engine relay `POST /api/v1/ai/nlq/ask` (`NlqController` → `NlqService` → `AiGatewayClient`)

| OWASP item | Control |
| --- | --- |
| A01 / API1 BOLA | `tenant_id` derived from the validated JWT (`TenantContext`), **never** from the body (rule 18.2 #2). The ai-service `SqlglotValidator` injects/enforces the tenant filter and runs SELECT-only via a read-only PG role — the SQL gatekeeper of ADR 0009 still applies. |
| A03 Injection / LLM02 | Unchanged: the engine relays; SQL is generated + validated in ai-service (allow-list tables, no statement stacking). |
| LLM04 / API4 DoS | Input bounded at the engine edge: `question` ≤ 500 chars, `maxRows` ≤ 1000 (`@Size`/`@Min`/`@Max`). Gateway timeouts now **configurable** (`qualitos.ai.{connect,read}-timeout-ms`) — tune per model without code change. |
| A10 / API7 SSRF | Outbound target is a fixed configured base-url (`qualitos.ai.base-url`), not user-controlled. |
| Error handling | Gateway failure → `502` ProblemDetail (`AiGatewayException`); missing tenant → `403`. Covered by unit tests (success / empty / 5xx / no-tenant). |

### Fabric anchoring (WS4, ADR 0012 — advances **A08 Software & Data Integrity**)

| OWASP item | Control |
| --- | --- |
| A08 Integrity | ML-DSA hybrid signing live (ADR 0011 WS1-3). `FabricBlockchainAnchorAdapter` (`@Profile("fabric")`) anchors Merkle roots to Hyperledger Fabric via `blockchain-service`; **falls back to the signed chained-receipt notarisation** (Phase A) if Fabric is down — anchoring is never lost. |
| A01 isolation | Chaincode composite key `(objectType, tenantId, root)` — a tenant can only verify its own roots. |
| Privacy §11.3 | **Hashes only** on-chain (Merkle roots) — no personal data. |
| A10 SSRF | `blockchain-service` URL is a fixed configured value (`qualitos.blockchain.service-url`), not user input; timeouts configurable. |

### Hybrid PQ TLS (WS5, ADR 0015 — advances **A02 Cryptographic Failures**)

| OWASP item | Control |
| --- | --- |
| A02 | Inbound TLS hybrid group `X25519MLKEM768` specified (terminated at ingress OQS or app-level BC-JSSE), driven by the `tls-hybrid-p3` suite (crypto-agility). FIPS path = `bc-fips`/`bctls-fips`. CBOM updated. Handshake validation pending a TLS-capable env. |
