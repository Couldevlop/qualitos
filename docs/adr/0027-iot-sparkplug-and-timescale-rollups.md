# ADR 0026 — Connecteur Sparkplug B + rollups TimescaleDB (IoT Hub)

- **Statut** : Accepté
- **Date** : 2026-06-16
- **Owners** : @Couldevlop

## Contexte

Deux écarts §9 restaient ouverts sur `api-iot-hub` (cf. `docs/AUDIT-stub-vs-reel.md`) :

1. **Sparkplug B** (§9.4) — protocole de données industriel unifié sur MQTT — annoncé
   mais absent. Le « vrai » Sparkplug B sérialise ses payloads en **protobuf** (schéma
   Eclipse Tahu) : embarquer Tahu + protobuf alourdit le module et rend les tests
   dépendants d'un broker MQTT.
2. **TimescaleDB** (§9.3, §6.4) — déclaré (hypertable V1) mais **inactif** : aucune
   continuous aggregate, aucune rétention/compression, et aucun endpoint de rollup
   pour alimenter dashboards/KPI temps réel.

Contraintes structurantes :
- **Clean Architecture** (domain pur / application / infrastructure / presentation) +
  ArchUnit, gate JaCoCo 85 %/75 %.
- **OWASP** §18.2 #2 : le `tenant_id` ne vient JAMAIS du payload.
- **CI sans Docker** : tests sur **H2** (Flyway désactivé en test — V1 utilise déjà des
  blocs `DO $$` que H2 ne sait pas parser).

## Décision

### 1. Sparkplug B — décodage JSON, protobuf délégué

On calque le connecteur sur les patterns MQTT/FHIR : **décodeur pur** + **handler pur**
(`Outcome(ingested, dropped, issues)`) + `Properties` (flag `enabled=false`) +
`ConnectorConfig` gardé + `presentation/rest/SparkplugIngestionController`
(`POST /api/v1/iot/sparkplug`).

- Le décodeur accepte un payload Sparkplug B **NDATA/DDATA déjà décodé en JSON**
  (`{groupId, edgeNodeId, deviceId, metrics:[{name,value,unit,timestamp}], timestamp}`).
  Une passerelle Edge (Tahu / EMQX Sparkplug codec) produit cette forme. Le **décodage
  protobuf brut → JSON est délégué** à un adaptateur ultérieur (Eclipse Tahu), sans
  toucher à la logique métier.
- **Code device** = `deviceId`, à défaut `edgeNodeId` (cas NDATA d'un Edge Node sans
  device fils).
- **Résolution tenant** : Sparkplug circule sur MQTT (pas de JWT), donc — comme le
  connecteur MQTT — le tenant est résolu via `DeviceRepository.findUniqueByCode`,
  **fail-closed** sur collision cross-tenant (anti-IDOR A01), jamais depuis le payload.
- **Robustesse** : ne lève jamais ; champs pourris comptés, borne `maxMetrics` (A04),
  une métrique en échec n'interrompt pas le lot, motifs sanitizés (A09).
- Désactivé par défaut (`enabled=false`) → endpoint **404** sans le flag (surface nulle,
  A05), même garantie que MQTT.

### 2. Rollups — calcul SQL standard + TimescaleDB en accélérateur

- **Endpoint** `GET /api/v1/iot/devices/{id}/telemetry/rollup?metric=&bucket=hour&limit=`
  (use case `RollupTelemetryUseCase` + port `TelemetryRepository.rollupByDevice` + adapter
  `JpaTelemetryRepository`). Calcul **SQL standard portable** :
  `date_trunc(unit, recorded_at)` + AVG/MIN/MAX/COUNT — fonctionne sur PostgreSQL simple
  **ET** H2 (mode PostgreSQL). L'unité `date_trunc` provient d'une **allow-list enum**
  (`RollupBucket`), jamais du wire (A03) ; tenant dérivé du JWT, device tenant-scopé (A01),
  bucket hors liste → 400.
- **Migration V2** `V2__iot_timescale_rollups.sql` : crée la continuous aggregate horaire
  `iot_telemetry_hourly` (time_bucket 1h, avg/min/max/count par tenant+device+metric) +
  policies de rafraîchissement, **rétention** (drop chunks > 90j) et **compression**
  (> 7j). **TOUT** est encapsulé dans `DO $$ IF EXISTS(timescaledb) ... END IF; $$` : sur
  PostgreSQL simple (sans l'extension) le bloc ne s'exécute pas → aucune fonction Timescale
  référencée ; en CI (H2, Flyway off) la migration n'est jamais jouée. La continuous
  aggregate n'est qu'un **accélérateur** : le résultat fonctionnel de l'endpoint est
  identique avec ou sans elle.

## Conséquences

- §9.4 gagne un 4e protocole réel (Sparkplug B) ; reste STUB : LoRaWAN.
- §9.3/§6.4 : TimescaleDB activé sans casser CI ni PostgreSQL simple ; rollups exposés
  pour dashboards/KPI.
- **Neutralité H2 prouvée** : `clean verify` vert (150 tests, JaCoCo + ArchUnit) ; la V2
  ne s'exécute pas en test (Flyway off, comme V1), et son contenu Timescale est garanti
  inerte hors runtime TimescaleDB par la garde `IF EXISTS`.
- **Reste à faire** : adaptateur protobuf Sparkplug (Eclipse Tahu) + consommateur MQTT
  Sparkplug ; rafraîchissement réel de la continuous aggregate à valider sur un cluster
  TimescaleDB de prod (Testcontainers Timescale en CI).
