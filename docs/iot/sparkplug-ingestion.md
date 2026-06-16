# Ingestion Sparkplug B — api-iot-hub

> CLAUDE.md §9.4 (modèle de données industriel unifié sur MQTT) · ADR 0026.

## Ce que ça fait

Une passerelle Edge (Eclipse Tahu / codec Sparkplug d'EMQX) pousse un payload
Sparkplug B **NDATA/DDATA** vers QualitOS. Chaque métrique (température cobot,
vibration, pression…) devient un point de télémétrie : les seuils configurés
déclenchent automatiquement une **non-conformité** côté quality-engine (chaîne §9.9).

```
PLC / cobot ──MQTT Sparkplug B──▶ Edge Gateway (Tahu) ──POST /api/v1/iot/sparkplug──▶ api-iot-hub
                                   (protobuf → JSON)        (NDATA/DDATA JSON)    │
                                                                                  └─▶ TimescaleDB
```

> **Portée** : le décodage **protobuf brut** Sparkplug B (schéma Tahu) est délégué à un
> lot ultérieur (adaptateur MQTT + Tahu). Ce connecteur accepte la forme **déjà décodée
> en JSON** que produit une passerelle Edge — sans dépendance lourde, 100 % testable.

## Contrat d'API

`POST /api/v1/iot/sparkplug` — `Content-Type: application/json`

- **Auth** : OAuth2 `client_credentials`. Rôles : `DEVICE`, `GATEWAY`, `TENANT_ADMIN`,
  `QUALITY_MANAGER`. (Le tenant n'est PAS lu du JWT pour l'ingestion : voir sécurité.)
- **Corps** : un payload Sparkplug B décodé.

### Forme du payload

```json
{
  "groupId": "usine-A", "edgeNodeId": "node-1", "deviceId": "capteur-01",
  "metrics": [
    {"name": "Temperature", "value": 4.2, "unit": "degC"},
    {"name": "Vibration", "value": 1.5, "unit": "g", "timestamp": 1749722400000}
  ],
  "timestamp": 1749722400000
}
```

| Champ télémétrie | Source Sparkplug |
|---|---|
| `deviceCode` | `deviceId` → à défaut `edgeNodeId` (NDATA d'un Edge Node sans device) |
| `metric` | `metrics[].name` (obligatoire) |
| `value` | `metrics[].value` (numérique requis ; sinon métrique ignorée) |
| `unit` | `metrics[].unit` (optionnel) |
| `recordedAt` | `metrics[].timestamp` → `timestamp` du payload (epoch ms OU ISO-8601 ; absent = heure serveur) |

Le device doit être **provisionné** (`POST /api/v1/iot/devices`, protocole `SPARKPLUG_B`),
avec un **code globalement unique** (voir sécurité — résolution fail-closed).

### Réponses

| Statut | Sens |
|---|---|
| `202 Accepted` | ≥ 1 métrique ingérée — corps `{ingested, dropped, issues[]}` |
| `422 Unprocessable Entity` | tout rejeté (device inconnu/ambigu, payload pourri…) |
| `401 / 403` | token absent / rôle insuffisant |
| `404 Not Found` | connecteur désactivé (`enabled=false`, défaut) |

## Garanties de sécurité

- **Tenant** (§18.2-2) : Sparkplug B circule sur MQTT (pas d'identité tenant). Le payload
  identifie un **équipement** ; le tenant est résolu via `findUniqueByCode` et vient de
  `Device.tenantId()` — **jamais** du payload. Une **collision cross-tenant** (même code
  chez deux tenants) renvoie `empty` → **fail-closed** (anti-IDOR A01).
- **Anti-DoS** : métriques bornées à `qualitos.iot.sparkplug.max-metrics` (500) ;
  troncature signalée dans `issues[]`.
- **Robustesse** : ne lève jamais ; métriques pourries comptées ; motifs sanitizés (A09).
- **Kill-switch** : `qualitos.iot.sparkplug.enabled=false` (env `IOT_SPARKPLUG_ENABLED`,
  **défaut**) retire l'endpoint ET le handler du contexte Spring (404, surface nulle A05).

## Architecture (Clean Architecture, vérifiée ArchUnit)

| Couche | Classe | Rôle |
|---|---|---|
| infrastructure | `SparkplugBDecoder` | décodage JSON→métriques, pur, sans Spring |
| infrastructure | `SparkplugIngestionHandler` | résolution device + ingestion, ne lève jamais |
| infrastructure | `SparkplugConnectorConfig` / `SparkplugProperties` | câblage conditionnel |
| presentation | `SparkplugIngestionController` | délégation HTTP uniquement |

Le use case (`IngestTelemetryUseCase`) et le domaine sont inchangés — même chemin que
MQTT/OPC-UA/FHIR (seuils, NC, touchLastSeen, twin).
