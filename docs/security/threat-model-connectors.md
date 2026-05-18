# Threat Model STRIDE — api-connectors (P4)

> Cf. CLAUDE.md §9.8 (Sécurité IoT zero-trust), §11.1 (OWASP ASVS L3), §13.3 (Connecteurs).

## Périmètre

- Service `apps/api-connectors/` (port 8084).
- Stockage Postgres : `connector_configs` (BLOBs AES-256-GCM), `sync_jobs`, `sync_history`.
- Frontières externes : SAP REST, ServiceNow REST, HL7 FHIR R5, OPC-UA endpoints, Vault Transit.

## Risques principaux (top 5)

1. **Vol de credentials tiers** (API keys SAP/ServiceNow, OAuth refresh tokens FHIR) — impact : accès cross-tenant aux systèmes externes.
2. **Cross-tenant contamination** — un connecteur tenant A leak des données vers tenant B.
3. **SSRF** depuis un adapter vers infrastructure interne (cloud metadata, services privés).
4. **Replay/Tampering** sur les sync jobs déclenchés.
5. **Disclosure** des configs sensibles via logs ou erreurs RFC 7807.

## STRIDE

### S — Spoofing
| Menace | Mitigation |
|--------|------------|
| Faux JWT trigger sync | Validation Keycloak JWKS (aud/iss/exp/sig) |
| Webhook entrant non signé (futur) | HMAC SHA-256 + horodatage anti-replay (sprint 2) |
| Adapter cible (FHIR) spoofé via DNS | TLS pinning sur certificats serveurs (sprint 2) |

### T — Tampering
| Menace | Mitigation |
|--------|------------|
| Modification config connecteur sans audit | Audit log immutable + ABAC (`super_admin` ou `admin_tenant`) |
| Tampering payload SAP en transit | TLS 1.3 mutuel + signature HMAC payload (option SAP) |
| Empoisonnement de la `sync_history` | Table append-only + chain SHA-256 |

### R — Repudiation
| Menace | Mitigation |
|--------|------------|
| Operator nie avoir activé un sync | Audit log chained + ancrage blockchain Fabric (§11.3) |
| FHIR endpoint nie avoir reçu un PUT | Réception ack signée stockée dans `sync_history.evidence_blob` |

### I — Information Disclosure
| Menace | Mitigation |
|--------|------------|
| Listing connecteurs cross-tenant | Hibernate `@Filter(tenantId)` + RLS Postgres |
| Logs avec API keys en clair | Filter Logback redactor sur patterns secret + Presidio (sprint 2) |
| OpenAPI exposé en prod révèle endpoints internes | `/v3/api-docs` permitAll en dev, gated `super_admin` en prod |
| RFC 7807 `instance` URI fuite token query string | Stripping des query params dans le ProblemDetail builder |
| Snapshot Postgres exporté | pgcrypto + restriction `pg_dump` à un rôle dédié |

### D — Denial of Service
| Menace | Mitigation |
|--------|------------|
| Trigger sync en boucle (DoS contre SAP) | Rate-limit per (tenant, connector) — `RateLimitService` propagé |
| Réponse FHIR gigantesque | Cap response size 50 MB + streaming en chunks |
| Adapter externe latency timeout-of-die | Circuit breaker (Resilience4j) + bulkhead par adapter |

### E — Elevation of Privilege
| Menace | Mitigation |
|--------|------------|
| User non-admin déclenche sync write | `@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_TENANT')")` au use case |
| Adapter compromis exécute code arbitraire | Adaptateurs sandboxés (allow-list HTTP, pas de shell exec, pas de file I/O sortant non-prévu) |
| Pack marketplace charge un adapter malveillant | Validation Cosign + revue manuelle avant publication |

## Tests de sécurité

| Suite | Objet | Cible |
|-------|-------|-------|
| Unit | `@PreAuthorize`, RBAC, allow-list URL | 100 % paths |
| Integration | Cross-tenant config isolation (Testcontainers) | Top 3 connecteurs |
| DAST | OWASP ZAP scan automatisé | Pre-deploy CI |
| Pentest | Annuel — accès tenant→tenant via connecteur | — |

## Suivi

- Owner : RSSI plateforme.
- Revue trimestrielle ou à chaque ajout d'adapter.
- Lien : `docs/security/owasp-checklist-p4.md`.
