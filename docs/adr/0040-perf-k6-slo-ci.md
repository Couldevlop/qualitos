# ADR 0040 — Tests de performance k6 avec budgets SLO en CI

- **Statut** : Accepté
- **Date** : 2026-06-22
- **Owners** : @Couldevlop
- **Phase** : Prod-readiness (reliquat CLAUDE.md §14.3 — « Performance : Gatling, k6 »)

## Contexte

CLAUDE.md §14.3 impose des tests de performance et fixe les SLO API : **p95 < 300 ms,
p99 < 800 ms** (repris au §2.1 et dans la Definition of Done §20). Jusqu'ici, ces
budgets n'étaient vérifiés que par les histogrammes Prometheus en runtime
(buckets SLO 100/300/800 ms, runbook `observability.md`) — **aucun garde-fou
préventif** n'empêchait une régression de latence d'atteindre `develop`.

Il manquait :

1. Des scénarios de charge reproductibles ciblant les **chemins chauds** de
   `api-quality-engine` (les endpoints de listing paginé, les plus sollicités).
2. Un **gate bloquant** mappant les SLO sur des seuils qui font échouer la CI.
3. Une authentification réaliste (token Keycloak, claim `tenant_id`) sans secret
   en clair (§18.2.3) et respectant le multi-tenant (§18.2.2).

## Décision

### Outil : k6 (et non Gatling)

On retient **k6** comme outil principal de test de charge :

- Scénarios en **JavaScript** (cohérence avec l'écosystème front Angular/TS,
  pas de module Maven/Scala supplémentaire).
- Image officielle **`grafana/k6`** : aucun build, exécutable en CI et en local
  via Docker.
- **Thresholds natifs** mappés 1:1 sur les SLO ; le code de sortie de k6 propage
  l'échec → intégration CI triviale.

Gatling (JVM/Scala) est **écarté pour l'instant** : il imposerait un module et un
plugin Maven sans bénéfice — les chemins chauds sont des GET REST simples. Un
module Gatling pourra être ajouté si un besoin de tir JVM-natif (corrélation avec
le profiling Spring) émerge ; cette ADR n'y ferme pas la porte.

### Périmètre — chemins chauds (lecture seule)

`apps/api-quality-engine/perf/` :

- `hot-paths.js` — listing paginé des 6 modules cœur :
  `/api/v1/standards`, `/api/v1/industry-packs`, `/api/v1/pdca/cycles`,
  `/api/v1/capa/cases`, `/api/v1/nc`, `/api/v1/audits/plans`.
- `standards-drilldown.js` — Standards Hub : listing → drill-down
  `/api/v1/standards/{id}` (sérialisation la plus lourde : sections → clauses →
  exigences imbriquées).

Bibliothèque partagée `perf/lib/` : `config.js` (env + défauts dev),
`auth.js` (password grant Keycloak), `thresholds.js` (budgets SLO),
`checks.js` (assertions de forme Spring `Page<T>`).

Tous les scénarios sont **GET only** : sûrs sur une stack partagée, ils ne
créent/altèrent aucune donnée.

### Budgets SLO (thresholds bloquants)

| Threshold k6                              | Valeur            | Source           |
| ----------------------------------------- | ----------------- | ---------------- |
| `http_req_duration` p(95)                 | `< 300 ms`        | §14.3 / §2.1     |
| `http_req_duration` p(99)                 | `< 800 ms`        | §14.3            |
| `http_req_failed` rate                    | `< 1 %`           | fiabilité §2.1   |
| `checks` rate                             | `> 99 %`          | validité fonct.  |

Des seuils **par endpoint** (`http_req_duration{name:…}`) isolent l'endpoint
fautif dans le résumé. La latence du mint de token Keycloak est suivie **sans
seuil bloquant** : ce n'est pas un chemin chaud applicatif.

### Authentification & multi-tenant

- `setup()` mint **une fois** un token via le **password grant** du client public
  `qualitos-web` (user `demo`) ; tous les VUs le partagent (on mesure l'API
  métier, pas le débit Keycloak).
- Le JWT porte le claim `tenant_id` (`infra/keycloak/realm-export.json`) : le
  filtrage tenant s'applique de bout en bout, aucune requête ne lit le tenant
  depuis le corps.
- **Pas de secret en clair** : le mot de passe `demo` est une credential de démo
  publique (déjà allowlistée gitleaks) ; en staging, surcharge via `K6_PASSWORD`
  (secret repo).

### CI

`.github/workflows/perf-k6.yml` : démarre la stack minimale
(`postgres + keycloak + api-quality-engine` via `docker-compose.dev.yml`),
attend la santé (Keycloak realm + engine `/actuator/health` UP, pas de sleep
arbitraire), effectue un pré-vol token, exécute les deux scénarios k6 et archive
les résumés JSON. Déclenché sur push/PR `main`/`develop` (chemins filtrés) et en
`workflow_dispatch` (VUs/durée paramétrables).

## Conséquences

- **Régression de latence détectée tôt** : tout dépassement de SLO sur un chemin
  chaud fait échouer la CI avant le merge.
- Périmètre volontairement restreint aux **GET** : sûr, déterministe, rapide.
  Les chemins d'écriture (POST/PUT) ne sont pas couverts par cette première
  itération (ils nécessiteraient un seed/cleanup transactionnel).
- Sensibilité au matériel : sur un poste de dev partagé (NAT Docker Desktop, CPU
  concurrent), le SLO peut être franchi alors qu'il est tenu en CI. La référence
  d'évaluation est le job CI (runner dédié, réseau partagé). Cette nuance est
  documentée dans le runbook et le README perf.

## Alternatives écartées

- **Gatling** : surcoût d'outillage (module + plugin Maven, Scala) sans bénéfice
  pour des GET REST simples — voir ci-dessus.
- **JMeter** : XML verbeux, intégration CI moins fluide, pas de thresholds aussi
  expressifs.
- **Vérification SLO uniquement en runtime (Prometheus/Grafana)** : détecte mais
  ne **prévient** pas une régression ; conservée en complément (observabilité),
  pas en remplacement du gate CI.

## Références

- Scénarios : `apps/api-quality-engine/perf/` (+ `README.md`)
- CI : `.github/workflows/perf-k6.yml`
- Runbook : `docs/runbooks/perf-k6.md`
- SLO runtime : `docs/runbooks/observability.md`
