# Threat Model STRIDE — api-iot-hub (P3)

> Cf. CLAUDE.md §9 (IoT & Edge), §11.1 (OWASP ASVS L3).

## Périmètre

- Service `apps/api-iot-hub/` (port 8083).
- Stockage : TimescaleDB hypertable `iot_telemetry`, Postgres `iot_devices` (JSONB digital twin).
- Frontières : Keycloak (auth), api-quality-engine (auto-NC sur breach), futurs Edge Gateways (MQTT/OPC-UA).

## Acteurs

| Acteur | Niveau | Notes |
|--------|--------|-------|
| Device légitime | Authentifié X.509 mutual TLS | Provisionné par superadmin |
| Edge Gateway compromise | Élevé | Mutual TLS + cert revocation |
| Tenant cross-contamination | Critique | Filter JWT tenant_id, RLS Postgres |
| Operator interne malveillant | Élevé | Audit log immutable + ABAC |
| Attaquant externe sans auth | Faible (filtre périmètre) | TLS 1.3, Keycloak gating |

## STRIDE

### S — Spoofing
| Menace | Impact | Mitigation |
|--------|--------|------------|
| Device usurpé (faux cert) | Injection télémétrie frauduleuse, corruption KPIs | mTLS + cert pinning (fingerprint stocké en DB), revocation CRL |
| JWT volé | Lecture données d'un autre tenant | Validation aud + iss + exp + signature ; rotation 15 min ; refresh-rotation |
| Replay d'un payload télémétrie | Cumul de mesures, fausse alerte | Champ `ts` strictement croissant par device + window de 5 min |

### T — Tampering
| Menace | Impact | Mitigation |
|--------|--------|------------|
| Modification d'une row `iot_telemetry` | Falsification historique | Table append-only (REVOKE UPDATE/DELETE), audit log chained SHA-256 |
| Modification d'une règle stream | Désactivation alertes critiques | RBAC `quality_manager` minimum + audit log |
| Tampering pendant transit | Données corrompues | TLS 1.3 + signatures HMAC payload (optionnel sprint 2) |

### R — Repudiation
| Menace | Impact | Mitigation |
|--------|--------|------------|
| Operateur nie avoir desactive un device | Litige contractuel | Audit log immutable + ancrage blockchain Fabric (§11.3) |
| Device nie avoir emis une mesure | Litige fournisseur | Signature device-side (clé privée Edge Gateway) sur batch télémétrie |

### I — Information Disclosure
| Menace | Impact | Mitigation |
|--------|--------|------------|
| Listing devices cross-tenant | RGPD + concurrentiel | Hibernate `@Filter(tenantId)` + RLS Postgres |
| Lecture digital twin JSONB sensible | Fuite secrets | Champs sensibles chiffrés AES-256-GCM (Vault Transit) |
| Logs avec PII (IP, GPS device) | RGPD | Redaction Presidio + rétention 90 j |
| OpenAPI exposé en prod | Fingerprinting | `/v3/api-docs` permitAll en dev, gated par rôle en prod |

### D — Denial of Service
| Menace | Impact | Mitigation |
|--------|--------|------------|
| Flood télémétrie depuis 1 device | TimescaleDB rempli, ressources épuisées | Rate-limit par device (`/api/v1/iot/telemetry` 1 000 msg/min default) |
| Slow-loris sur ingestion HTTP | Worker tomcat bloqués | timeouts httpx + circuit breaker |
| Cardinality explosion sur TimescaleDB | Index dégradés | Limite `device_id` par tenant + alerte SOC > 10 000 |

### E — Elevation of Privilege
| Menace | Impact | Mitigation |
|--------|--------|------------|
| Use case `activateDevice` exécuté sans `super_admin` | Provisionnement non autorisé | `@PreAuthorize("hasRole('SUPER_ADMIN')")` au niveau use case |
| Injection SQL via filtre `?metric=` | Lecture cross-tenant / RCE | JPA Criteria + named params (jamais de concat string) |
| YAML deserialization (industry packs) | RCE | SnakeYAML SafeConstructor (rejet `!!java/`) |

## Tests de sécurité

| Suite | Objet | Couverture cible |
|-------|-------|------------------|
| Unit | TenantJwtFilter, RBAC `@PreAuthorize` | 100 % paths |
| Integration | Cross-tenant data leakage (Testcontainers) | Top 5 endpoints |
| DAST | OWASP ZAP scan automatisé sur OpenAPI | Pre-deploy CI |
| Pentest | Annuel — focus iot-hub + connectors externes | — |

## Suivi

- Owner sécurité : RSSI tenant + RSSI plateforme (escalade).
- Revue : trimestrielle ou à chaque modification du périmètre.
- Lien : `docs/security/owasp-checklist-p3.md`.
