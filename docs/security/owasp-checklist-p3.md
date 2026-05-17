# OWASP Checklist — P3 (IoT Hub + Industry Packs + Vision 5S)

> Cf. CLAUDE.md §11.1 (OWASP Top 10), §11.2 (LLM Top 10 — non applicable P3), OWASP ML Top 10 pour ai-vision-5s.

Légende : ✅ done | 🟡 partial | ⏳ deferred (sprint 2+) | N/A non applicable.

## OWASP Top 10 (2021)

| ID | Risque | Statut P3 | Implémentation |
|----|--------|-----------|----------------|
| A01 | Broken Access Control | ✅ | `@PreAuthorize` par endpoint (industry-packs/activate = SUPER_ADMIN ; devices CRUD = ADMIN_TENANT min). `tenant_id` extrait **uniquement** du JWT (cf. ADR 0001). RLS Postgres en plus du Hibernate `@Filter`. |
| A02 | Cryptographic Failures | 🟡 | TLS 1.3 (gateway), AES-256-GCM at rest pour secrets Vault. Post-quantum hybrid : CBOM rédigé (`cbom.md`), implémentation lib `security-commons/crypto` ⏳ P3.2. |
| A03 | Injection | ✅ | JPA Criteria + named params partout. Jakarta Validation sur DTOs (api-iot-hub + vision). Topics MQTT validés par regex allow-list. YAML loader = SnakeYAML `SafeConstructor` (rejet `!!java/`). |
| A04 | Insecure Design | ✅ | Threat model STRIDE rédigé (`threat-model-iot.md`). Decision logs : ADR 0004 (iot-hub) + ADR 0005 (industry-packs SPI). |
| A05 | Security Misconfiguration | ✅ | Images distroless `gcr.io/distroless/java21-debian12:nonroot` (Java) et `gcr.io/distroless/python3-debian12` (Python). Actuator restreint à `/health` + `/info`. CSP via OWASP Secure Headers filter. Whitelabel error page désactivée. |
| A06 | Vulnerable & Outdated Components | 🟡 | OWASP Dependency-Check intégré au CI (gaté par `vars.OWASP_DC_ENABLED`). SBOM CycloneDX généré au build. Gate `0 Critical / 0 High` à activer ⏳. |
| A07 | Identification & Authentication Failures | ✅ | Keycloak JWKS (issuer, audience, expiry, signature). Pas de JWT en URL/query. mTLS X.509 pour Edge Gateways → certs (sprint 2). |
| A08 | Software & Data Integrity Failures | ✅ | Pack YAML : SHA-256 fingerprint stocké en DB (`tenant_industry_packs.pack_sha256`). Signature Cosign stub pour pack publication (P3.2). |
| A09 | Security Logging & Monitoring Failures | ✅ | Logs JSON structurés (logstash format), `tenant_id` + `correlation_id` propagés via MDC. Audit log immutable chaîné SHA-256. Redaction PII via filter (sprint 2 — Presidio). |
| A10 | SSRF | ✅ | Egress allow-list dans `WebClient.Builder` (uniquement `keycloak:8080`, `api-quality-engine:8082` interne). Aucun fetch d'URL fournie par user. |

## OWASP API Top 10 (2023)

| ID | Risque | Statut | Note |
|----|--------|--------|------|
| API1 | BOLA | ✅ | tenant_id JWT-filter sur toutes les lectures |
| API2 | Broken Authentication | ✅ | Keycloak |
| API3 | Property-level Authorization | ✅ | DTOs explicites (pas de `BeanUtils.copyProperties`) |
| API4 | Unrestricted Resource Consumption | 🟡 | Rate-limit per tenant via `RateLimitService` (déjà en place api-quality-engine) ; à propager dans api-iot-hub sprint 2 |
| API5 | BFLA | ✅ | `@PreAuthorize` méthode-level |
| API6 | Server-Side Request Forgery | ✅ | Allow-list ; cf. A10 |
| API7 | Misconfiguration | ✅ | cf. A05 |
| API8 | Lack of Inventory | ✅ | OpenAPI 3.1 publié, versioning sémantique |
| API9 | Improper Asset Management | ⏳ | À couvrir lors de l'intégration des connecteurs externes (P4) |
| API10 | Unsafe Consumption of APIs | ✅ | Feign Client interne (api-iot-hub → api-quality-engine) avec timeouts + circuit breaker stub |

## OWASP ML Top 10 (ai-vision-5s)

| ID | Risque | Statut | Note |
|----|--------|--------|------|
| ML01 | Input Manipulation Attack | ✅ | libmagic MIME check + cap 10 MB + EXIF strip avant inférence |
| ML02 | Data Poisoning | N/A | Stub déterministe — pas de fine-tuning en P3.1 |
| ML03 | Model Inversion | ⏳ | Pas applicable au stub ; à évaluer lors du wiring YOLOv8 réel (P3.2) |
| ML04 | Membership Inference | ⏳ | Idem |
| ML05 | Model Theft | ✅ | Pas de download endpoint, rate-limit slowapi |
| ML06 | AI Supply Chain Attacks | 🟡 | Modèles téléchargés via Hugging Face avec SHA pinning (sprint 2) |
| ML07 | Transfer Learning Attack | N/A | Pas de transfer learning P3.1 |
| ML08 | Model Skewing | ⏳ | Monitoring drift (Evidently AI) sprint 2 |
| ML09 | Output Integrity | ✅ | Validation Pydantic des sorties, score borné [0,1] |
| ML10 | Model Poisoning | N/A | cf. ML02 |

## Actions sprint 2 (gating release P3 complet)

1. Activer OWASP DC gate (0 Critical/0 High) sur le CI.
2. Wirer Presidio pour redaction PII dans logs JSON.
3. Livrer `libs/security-commons/crypto` (suite PQ hybride).
4. Wirer YOLOv8 réel + couvrir ML03-04, ML06, ML08.
5. Propager `RateLimitService` à api-iot-hub (API4).
