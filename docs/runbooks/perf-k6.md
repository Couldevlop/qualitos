# Runbook — Tests de performance k6 (gate SLO)

> CLAUDE.md §14.3 · SLO §2.1 / §20 : `http_req_duration` **p95 < 300 ms**,
> **p99 < 800 ms**, taux d'erreur **< 1 %**.

Tests [k6](https://k6.io) couvrant les chemins chauds (lecture) de
`api-quality-engine`. Les seuils sont des **budgets SLO bloquants** : si l'un
est dépassé, k6 sort en code != 0 et le job CI échoue.

- Scénarios : `apps/api-quality-engine/perf/` (`hot-paths.js`, `standards-drilldown.js`).
- Détails et paramétrage : `apps/api-quality-engine/perf/README.md`.
- CI : `.github/workflows/perf-k6.yml`.
- Décision : `docs/adr/0040-perf-k6-slo-ci.md`.

## 1. Démarrer la stack dev

```bash
docker compose -f docker-compose.dev.yml up -d --build \
  postgres keycloak api-quality-engine
```

Attendre que tout soit prêt (pas de sleep arbitraire) :

```bash
# Keycloak (realm importé)
until curl -fsS http://localhost:8080/realms/qualitos >/dev/null; do sleep 3; done
# Engine UP
until curl -fsS http://localhost:8082/actuator/health | grep -q '"status":"UP"'; do sleep 3; done
```

| Service            | URL                     | Accès        |
| ------------------ | ----------------------- | ------------ |
| api-quality-engine | http://localhost:8082   | Bearer JWT   |
| Keycloak           | http://localhost:8080   | qualitos     |
| Postgres           | localhost:5434          | qualitos     |

> La stack pré-charge les données de démo (tenant `…099`, 60 normes) — pas de
> seed manuel requis.

## 2. Vérifier le mint du token (pré-vol)

```bash
curl -s -o /dev/null -w '%{http_code}\n' \
  -X POST http://localhost:8080/realms/qualitos/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=qualitos-web&username=demo&password=demo&scope=openid'
# Attendu : 200
```

## 3. Lancer les tests perf

### k6 natif

```bash
k6 run apps/api-quality-engine/perf/hot-paths.js
k6 run apps/api-quality-engine/perf/standards-drilldown.js
```

### Docker — Linux (réseau hôte)

```bash
docker run --rm -i --network=host \
  -e BASE_URL=http://localhost:8082 \
  -e KEYCLOAK_URL=http://localhost:8080 \
  grafana/k6 run - < apps/api-quality-engine/perf/hot-paths.js
```

### Docker — Windows / macOS (Docker Desktop, NAT)

`--network=host` ne pointe pas sur l'hôte sous Docker Desktop : utiliser
`host.docker.internal` et monter le dossier perf.

```bash
docker run --rm -v "$PWD/apps/api-quality-engine/perf:/perf" \
  -e BASE_URL=http://host.docker.internal:8082 \
  -e KEYCLOAK_URL=http://host.docker.internal:8080 \
  grafana/k6 run /perf/hot-paths.js
```

> La NAT Docker Desktop ajoute quelques ms ; sur un poste partagé (CPU
> concurrent), le SLO peut être franchi alors qu'il est tenu en CI. La
> référence d'évaluation reste le job CI (runner dédié, réseau partagé).

## 4. Paramétrer la charge

```bash
# Charge plus forte
VUS=50 DURATION=1m k6 run apps/api-quality-engine/perf/hot-paths.js
# Smoke court
SMOKE=1 k6 run apps/api-quality-engine/perf/hot-paths.js
```

Toutes les variables : voir le README perf.

## 5. Lire le résultat

- **Vert** : `✓ http_req_duration p(95)<300`, `p(99)<800`, `http_req_failed rate<0.01`.
- **Rouge** : k6 affiche `thresholds … have been crossed` et sort en code != 0.
  Identifier l'endpoint fautif dans la ventilation `http_req_duration{name:…}`,
  puis croiser avec Grafana (runbook `observability.md`, dashboard p95 vs SLO)
  pour distinguer un problème applicatif (requête lourde) d'un bruit machine.

## 6. Déclencher en CI manuellement

Onglet Actions → workflow **« Perf (k6 SLO) »** → *Run workflow*, en réglant
`vus` et `duration`. Les résumés JSON sont archivés en artefact `k6-summaries`.

## 7. Arrêter la stack

```bash
docker compose -f docker-compose.dev.yml down -v
```
