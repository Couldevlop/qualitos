# ADR 0007 — Connectors SPI (P4)

**Statut** : Accepted | **Date** : 2026-05-17 | **Phase** : P4

## Contexte

QualitOS doit s'intégrer sans friction aux systèmes tiers (ERP SAP/Oracle, ITSM ServiceNow/Jira, EHR HL7 FHIR, MES OPC-UA, GED SharePoint, etc.). CLAUDE.md §9.4 et §13.3 listent 15+ connecteurs cibles. P4 sprint 1 livre la fondation SPI extensible plus 4 adaptateurs stub démontrables.

## Décision

Nouveau service `apps/api-connectors/` (Spring Boot 3.5, Java 21, port 8084, distroless) en **Clean Architecture hexagonale** :

```
apps/api-connectors/
├── domain/connector/
│   ├── ConnectorDefinition       (record — id, supportedProtocols, configSchema)
│   ├── ConnectorConfig           (VO encryptedByDesign)
│   ├── SyncJob, SyncResult       (records)
│   └── port/
│       ├── ConnectorProvider     (SPI implémenté par chaque adapter)
│       ├── ConnectorRepository   (JPA)
│       └── SecretCipher          (Vault-backed)
├── application/connector/usecase/
│   ├── RegisterConnectorUseCase
│   ├── TriggerSyncUseCase
│   └── ListConnectorsUseCase
├── infrastructure/connector/
│   ├── external/
│   │   ├── SapErpConnectorProvider       (REST stub)
│   │   ├── ServiceNowConnectorProvider   (REST avec mocks)
│   │   ├── FhirR5ConnectorProvider       (REST vers hapi.fhir.org/baseR5/)
│   │   └── OpcUaConnectorProvider        (placeholder Eclipse Milo)
│   ├── persistence/                       (JPA entities + AES-256-GCM at rest)
│   └── config/                            (Spring Security, Beans)
└── presentation/connector/rest/
    ├── ConnectorController
    └── ConnectorDto
```

### Points clés

1. **SPI ouvert** : `ConnectorProvider` exposé en `META-INF/services` permet à des packs marketplace tiers d'ajouter de nouveaux adaptateurs sans modifier le core.
2. **Allow-list outbound HTTP** : chaque adapter déclare son host autorisé. Le `WebClient.Builder` global refuse tout host non-listé (anti-SSRF, OWASP A10).
3. **Secrets chiffrés** : les configs adapter (API keys, OAuth refresh tokens, certs) sont chiffrées AES-256-GCM avec clés stockées dans HashiCorp Vault Transit (ou clé locale en dev).
4. **Multi-tenancy** : chaque adapter est instancié par tenant ; le `tenant_id` est extrait du JWT (cf. ADR 0001), jamais du body.
5. **Tracing** : chaque sync émet un span OpenTelemetry avec `tenant_id`, `connector_id`, `result_status`.

## Conséquences

### Positives
- Architecture découplée : ajouter un nouveau connecteur = créer une classe + un fichier `META-INF/services`. Pas de modification du core.
- Sécurité : pas de fuite secrets (chiffrement at-rest + Vault), pas de SSRF (allow-list), pas de cross-tenant (JWT-only).
- Testabilité : interfaces de domain permettent de mocker tout adapter pour les tests E2E.

### Négatives
- Couche d'abstraction supplémentaire (vs. appel HTTP direct depuis un service métier). Justifié par la centralisation des règles de sécurité.
- Adaptateurs externes (FHIR, SAP) nécessitent maintenance versionnée — risque de cassure quand l'API tiers évolue.

### Alternatives rejetées
- **Camel / Spring Integration** : trop lourd pour le périmètre V1 (15 connecteurs). Camel re-évalué à 50+ connecteurs.
- **Tous les connecteurs dans api-quality-engine** : violerait Bounded Context (DDD) et complexifierait la rotation des credentials.

## Suivi

- Sprint P4.2 : adaptateurs réels SAP (OAuth2 client_credentials), ServiceNow (Basic Auth + table-scoped queries), HL7 FHIR (TLS mutuel hors test).
- Sprint P5 : OPC-UA réel via Eclipse Milo + intégration avec api-iot-hub.
- Sprint P5+ : marketplace pour packs de connecteurs tiers (validés Cosign).
