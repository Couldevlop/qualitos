# Runbook — Observabilité QualitOS

> CLAUDE.md §14 · SLO §20 : p95 API < 300 ms, disponibilité 99,95 %.

## Démarrer la stack en dev

```bash
docker-compose -f docker-compose.dev.yml --profile observability up -d prometheus grafana
```

| Service | URL | Accès |
|---|---|---|
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3000 | admin / admin |

Le dashboard **« QualitOS — Vue d'ensemble services »** (dossier QualitOS) est
provisionné automatiquement : trafic, p95 vs SLO, 5xx, heap JVM, pool Hikari, up.

## Ce qui est exposé

- Chaque service Spring Boot expose `/actuator/prometheus`
  (api-core :8081, api-quality-engine :8082, api-iot-hub :8083).
- Histogrammes `http_server_requests_seconds` avec buckets SLO 100/300/800 ms.
- Keycloak expose `/metrics` sur son port management 9000 (interne).
- Cibles et règles : `infra/observability/prometheus.yml` + `rules/qualitos-alerts.yml`.

## Alertes fournies

| Alerte | Condition | Gravité |
|---|---|---|
| `ApiP95LatencyHigh` | p95 > 300 ms pendant 10 min (SLO §20) | warning |
| `ApiErrorRateHigh` | 5xx > 5 % pendant 5 min | critical |
| `ServiceDown` | cible injoignable 2 min | critical |

Brancher l'Alertmanager du cluster en prod (non inclus en dev).

## Logs JSON structurés

- Profils Spring `prod` ou `json-logs` → sortie JSON une-ligne (LogstashEncoder),
  MDC inclus (`tenantId`), parsable Loki/OpenSearch sans regex.
- Dev/test/CI : pattern console lisible inchangé.
- Activer ponctuellement en local : `SPRING_PROFILES_ACTIVE=dev,json-logs`.
- Règle §22.9 : jamais de PII dans les messages de log.

## En production (K8s)

- Le chart Helm (`infra/k8s/qualitos`) pose les annotations `prometheus.io/*`
  sur chaque pod ; avec Prometheus Operator :
  `--set prometheus.serviceMonitor.enabled=true`.
- Profil Spring `prod` actif par défaut dans le chart → logs JSON d'office.

## Diagnostic rapide

| Symptôme | Vérifier |
|---|---|
| Cible DOWN dans Prometheus | `curl http://<svc>:<port>/actuator/prometheus` ; le service expose-t-il bien `management.endpoints…include: prometheus` ? |
| p95 dégradé | dashboard panel « Latence p95 » → drill par `application` ; corréler avec « Pool JDBC Hikari » (saturation connexions) |
| Pas de logs JSON en prod | le pod a-t-il `SPRING_PROFILES_ACTIVE=prod` ? (values.yaml `global.springProfiles`) |
| Grafana vide | datasource Prometheus provisionnée ? (`infra/observability/grafana/provisioning/`) |
