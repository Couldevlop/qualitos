# Rollups de télémétrie + TimescaleDB — api-iot-hub

> CLAUDE.md §9.3 (TimescaleDB) · §6.4 (KPI temps réel) · ADR 0026.

## Ce que ça fait

Agrège la télémétrie d'un équipement par tranche temporelle (avg/min/max/count) pour
alimenter dashboards et KPI. Deux niveaux complémentaires :

1. **Endpoint de rollup** — calcul **SQL standard portable** (`date_trunc` + agrégats),
   fonctionne sur PostgreSQL simple **ET** H2 (tests), sans dépendre de TimescaleDB.
2. **Continuous aggregate TimescaleDB** (`iot_telemetry_hourly`) — **accélérateur** sur
   le runtime de production : rafraîchissement automatique, rétention 90j, compression 7j.

## Contrat d'API

`GET /api/v1/iot/devices/{id}/telemetry/rollup?metric=&bucket=hour&limit=`

- **Auth** : authentifié (claim `tenant_id` requis).
- **Params** :
  - `metric` (obligatoire) — métrique exacte à agréger.
  - `bucket` — `hour` (défaut) | `day` | `minute` (allow-list ; valeur hors liste → 400).
  - `limit` — nombre max de buckets (défaut 168 = 7 j horaires ; borné à 1000).
- **Réponse** `200` — tableau de buckets, **tri DESC** (plus récent d'abord) :

```json
[
  {"bucketStart": "2026-06-12T11:00:00Z", "metric": "temp", "avg": 5.0, "min": 5.0, "max": 5.0, "count": 1},
  {"bucketStart": "2026-06-12T10:00:00Z", "metric": "temp", "avg": 6.0, "min": 4.0, "max": 8.0, "count": 2}
]
```

| Statut | Sens |
|---|---|
| `200 OK` | buckets agrégés |
| `400 Bad Request` | `bucket` hors allow-list / `metric` manquant |
| `404 Not Found` | device inconnu **pour ce tenant** (fail-closed) |
| `401` | non authentifié |

## Garanties de sécurité

- **Tenant** (§18.2-2) : dérivé du JWT (`TenantContext`) ; device résolu **tenant-scopé**
  (un device d'un autre tenant = 404, anti-IDOR A01).
- **Injection** (A03) : l'unité `date_trunc` provient d'une **allow-list enum**
  (`RollupBucket`), jamais du wire ; tous les autres champs sont des paramètres liés.
- **Anti-DoS** (A04) : `limit` borné à 1000 buckets.

## Migration V2 — neutralité H2 / PostgreSQL simple

`V2__iot_timescale_rollups.sql` crée la continuous aggregate + les policies. **Tout** vit
dans un bloc gardé :

```sql
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'timescaledb') THEN
    -- CREATE MATERIALIZED VIEW ... WITH (timescaledb.continuous),
    -- add_continuous_aggregate_policy, add_retention_policy, add_compression_policy
  END IF;
END $$;
```

- Sur **PostgreSQL simple** (sans l'extension) : le bloc ne s'exécute pas → aucune
  fonction Timescale référencée. Inoffensif.
- En **CI/H2** : Flyway est désactivé en test (comme pour V1, qui utilise déjà des blocs
  `DO $$`) → la migration n'est jamais jouée. L'endpoint de rollup, lui, est prouvé en
  intégration H2 (`DeviceControllerIntegrationTest`).
- Sur **TimescaleDB** : la continuous aggregate accélère les lectures ; le résultat
  fonctionnel de l'endpoint reste identique (mêmes avg/min/max/count).

## Architecture (Clean Architecture, vérifiée ArchUnit)

| Couche | Classe | Rôle |
|---|---|---|
| domain | `RollupBucket`, `TelemetryRollup` | granularité (allow-list) + bucket agrégé |
| domain (port) | `TelemetryRepository.rollupByDevice` | contrat d'agrégation |
| application | `RollupTelemetryUseCase` | portée tenant + bornes + défauts |
| infrastructure | `JpaTelemetryRepository.rollupByDevice` | SQL natif portable PG/H2 |
| presentation | `DeviceController#rollup` | délégation HTTP uniquement |
