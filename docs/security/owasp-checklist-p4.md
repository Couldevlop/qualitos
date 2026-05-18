# OWASP Checklist — P4 (Standards Hub extension + Industry Packs + api-connectors + IMS)

> Cf. CLAUDE.md §11.1 (OWASP Top 10), OWASP API Top 10 (Connectors), OWASP YAML safety (Industry Packs).

Légende : ✅ done | 🟡 partial | ⏳ deferred (sprint 2+) | N/A non applicable.

## OWASP Top 10 (2021)

| ID | Risque | Statut P4 | Implémentation |
|----|--------|-----------|----------------|
| A01 | Broken Access Control | ✅ | `@PreAuthorize` par endpoint (`super_admin` pour register norm/connector, tenant_id JWT-filter pour reads). RLS + Hibernate filter. |
| A02 | Cryptographic Failures | ✅ | Configs connecteurs chiffrées AES-256-GCM at rest (clés Vault Transit). TLS 1.3 outbound. Pack YAML : SHA-256 fingerprint. |
| A03 | Injection | ✅ | JPA Criteria + named params. Jakarta Validation sur DTOs. SnakeYAML `SafeConstructor` pour les packs (rejet `!!java/`). Pas de concat string SQL. |
| A04 | Insecure Design | ✅ | STRIDE rédigé (`threat-model-connectors.md`). ADR 0006 (standards) + 0007 (connectors-spi). |
| A05 | Security Misconfiguration | ✅ | Distroless. Actuator restreint `/health` + `/info`. CSP via OWASP Secure Headers. Whitelabel désactivée. |
| A06 | Vulnerable Components | 🟡 | CycloneDX SBOM au build. OWASP DC gate à activer (`vars.OWASP_DC_ENABLED`). |
| A07 | Authentication Failures | ✅ | Keycloak JWKS (aud + iss + exp + sig). Pas de JWT en URL. Adapters externes : OAuth2 client_credentials par tenant (jamais de clé globale). |
| A08 | Data Integrity | ✅ | Pack YAML : SHA-256 fingerprint en DB + Cosign stub pour packs marketplace. Audit log chaîné SHA-256. |
| A09 | Logging & Monitoring | ✅ | JSON structuré, `tenant_id` + `correlation_id` MDC. Audit log immutable. Redaction secrets via Logback filter ; Presidio PII sprint 2. |
| A10 | SSRF | ✅ | Allow-list hosts par adapter (`api.servicenow.com`, `hapi.fhir.org/baseR5`, etc.). `WebClient.Builder` global rejette les hosts non listés. |

## OWASP API Top 10 (2023)

| ID | Risque | Statut | Note |
|----|--------|--------|------|
| API1 | BOLA | ✅ | tenant_id JWT-filter sur tous les reads `connectors`, `standards`, `industry-packs` |
| API2 | Broken Authentication | ✅ | Keycloak ; refresh-token rotation pour adapters OAuth2 |
| API3 | Property-level Authorization | ✅ | DTOs explicites — pas de `BeanUtils.copyProperties` |
| API4 | Unrestricted Resource Consumption | ✅ | Rate-limit per (tenant, connector) via `RateLimitService` |
| API5 | BFLA | ✅ | `@PreAuthorize` méthode-level (register/sync = super_admin) |
| API6 | SSRF | ✅ | cf. A10 |
| API7 | Misconfiguration | ✅ | cf. A05 |
| API8 | Inventory | ✅ | OpenAPI 3.1 publié, sémantique versioning |
| API9 | Improper Asset Management | 🟡 | Inventaire connecteurs actifs par tenant exposé via `/api/v1/connectors` ; dashboard inventaire ⏳ sprint 2 |
| API10 | Unsafe Consumption of APIs | ✅ | Circuit breaker + timeouts + validation Pydantic-style sur réponses |

## YAML Safety (Industry Packs)

| Risque | Mitigation |
|--------|------------|
| YAML deserialization → RCE | SnakeYAML `SafeConstructor` (rejet `!!java/`, `!!javax/`, etc.) |
| Pack non signé chargé | SHA-256 fingerprint requis ; Cosign signature optionnelle stockée + vérifiée |
| Pack avec champs inconnus | Schéma strict YAML 1.2 ; champs non déclarés → exception loader |
| Pack > 10 MB | Cap explicite, rejet avec RFC 7807 |

## Standards Hub seeds

| Risque | Mitigation |
|--------|------------|
| Clauses inventées (non conformes à la norme officielle) | Source : copies textuelles des normes publiées (références dans `standards/templates/<id>/`). Revue éditoriale obligatoire. |
| Templates documentaires non à jour | Veille trimestrielle ; ADR éditorial dédié par révision majeure (§8.10) |

## Actions sprint 2 (gating release P4 complet)

1. Activer OWASP DC gate (0 Critical/0 High) sur le CI.
2. Wirer Presidio pour redaction PII dans logs JSON.
3. Adaptateurs externes réels (SAP OAuth2, ServiceNow Basic Auth, FHIR mutual TLS).
4. Cosign signature obligatoire pour les packs marketplace.
5. Dashboard inventaire connecteurs par tenant (API9).
