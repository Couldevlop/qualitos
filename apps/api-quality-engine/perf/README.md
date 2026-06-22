# Tests de performance k6 — api-quality-engine

Scénarios [k6](https://k6.io) couvrant les **chemins chauds** (lecture) de
l'API `api-quality-engine`, avec un **gate SLO bloquant** (CLAUDE.md §14.3 /
§2.1 / §20) :

| Métrique            | Seuil (threshold k6)   |
| ------------------- | ---------------------- |
| `http_req_duration` | `p(95) < 300 ms`       |
| `http_req_duration` | `p(99) < 800 ms`       |
| `http_req_failed`   | `rate < 1 %`           |
| `checks`            | `rate > 99 %`          |

Si un seuil est dépassé, **k6 sort en code != 0** et le job CI échoue.

## Scénarios

| Fichier                  | Couverture                                                                                              |
| ------------------------ | ------------------------------------------------------------------------------------------------------ |
| `hot-paths.js`           | Listing paginé des 6 modules cœur : standards, industry-packs, PDCA, CAPA, NC, audits.                 |
| `standards-drilldown.js` | Standards Hub : listing → drill-down détail (`/standards/{id}`, arborescence sections/clauses lourde).  |

Tous les scénarios sont en **LECTURE SEULE** (GET) : sûrs à exécuter contre une
stack dev partagée, ils ne créent ni n'altèrent de données.

## Authentification & multi-tenant

L'authentification se fait une fois dans `setup()` via le **password grant**
Keycloak (client public `qualitos-web`, user `demo`). Le JWT porte le claim
`tenant_id` (réf. `infra/keycloak/realm-export.json`) : le multi-tenant est
respecté de bout en bout — aucune requête ne lit le tenant depuis le corps.

Aucun secret n'est codé en dur : le mot de passe par défaut `demo` est une
credential de **démo publique**. En staging, surcharger via `K6_PASSWORD`.

## Paramétrage (variables d'environnement)

| Variable              | Défaut                  | Rôle                                       |
| --------------------- | ----------------------- | ------------------------------------------ |
| `BASE_URL`            | `http://localhost:8082` | URL de l'engine                            |
| `KEYCLOAK_URL`        | `http://localhost:8080` | URL Keycloak                               |
| `KEYCLOAK_REALM`      | `qualitos`              | Realm                                      |
| `KEYCLOAK_CLIENT_ID`  | `qualitos-web`          | Client OIDC (password grant)               |
| `KEYCLOAK_CLIENT_SECRET` | *(vide)*             | Secret si client confidentiel              |
| `K6_USERNAME`         | `demo`                  | Utilisateur                                |
| `K6_PASSWORD`         | `demo`                  | Mot de passe (secret en staging)           |
| `PAGE_SIZE`           | `20`                    | Taille de page Spring Data                 |
| `VUS`                 | `10`                    | Virtual users (charge)                     |
| `DURATION`            | `30s`                   | Durée du palier                            |
| `SMOKE`               | *(non défini)*          | Si défini : run court constant-VUs         |

## Exécution locale

Voir le runbook complet : [`docs/runbooks/perf-k6.md`](../../../docs/runbooks/perf-k6.md).

### Docker (k6 non installé)

Sous Linux (réseau hôte direct) :

```bash
docker run --rm -i --network=host \
  -e BASE_URL=http://localhost:8082 \
  -e KEYCLOAK_URL=http://localhost:8080 \
  grafana/k6 run - < apps/api-quality-engine/perf/hot-paths.js
```

Sous Windows/macOS (NAT Docker Desktop — `host.docker.internal`) :

```bash
docker run --rm -v "$PWD/apps/api-quality-engine/perf:/perf" \
  -e BASE_URL=http://host.docker.internal:8082 \
  -e KEYCLOAK_URL=http://host.docker.internal:8080 \
  grafana/k6 run /perf/hot-paths.js
```

> Note : sous Docker Desktop, la traduction d'adresse `host.docker.internal`
> ajoute quelques ms de latence — les chiffres peuvent dépasser le SLO sur un
> poste partagé. La référence est la CI (réseau partagé, runner dédié).

### k6 natif

```bash
k6 run apps/api-quality-engine/perf/hot-paths.js
```

### Smoke rapide (3 VUs, 15s)

```bash
SMOKE=1 k6 run apps/api-quality-engine/perf/hot-paths.js
```

## Pourquoi k6 (et pas Gatling) ?

k6 est retenu comme outil principal : scénarios JavaScript (cohérents avec le
front Angular/TS), image officielle `grafana/k6` sans build, thresholds natifs
mappés 1:1 sur les SLO, intégration CI triviale. Gatling (Scala/JVM) imposerait
un module Maven + un plugin supplémentaires sans bénéfice ici (les chemins
chauds sont des GET REST simples). Si un besoin de tir de charge JVM-natif
(ex. corrélation avec le profiling Spring) émerge, un module Gatling pourra être
ajouté — voir ADR `docs/adr/0040-perf-k6-slo-ci.md`.
