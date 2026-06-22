# ADR 0035 — Time-travel des dashboards (état as-of réel des KPIs)

- **Statut** : Accepté
- **Date** : 2026-06-20
- **Owners** : @Couldevlop
- **Phase** : N1 1.4 (différenciateurs commerciaux, CLAUDE.md §7.3)

## Contexte

CLAUDE.md §7.3 cite le **time-travel** : « Affiche-moi l'état du dashboard au 15
mars 2025 ». L'exigence (ROADMAP § « Définition de fait — RENFORCÉE ») impose une
récupération **as-of réelle via une requête backend** sur les mesures historiques
(`kpi_measurements`), filtrée tenant — **pas** de simulation côté front. Le module
KPI (`quality.kpi`) persiste déjà `kpi_definitions` + `kpi_measurements` (couple
`kpi_id, period_start` unique), ce qui rend l'as-of calculable sans nouveau schéma.

## Décision

Sous-package hexagonal
`apps/api-quality-engine/.../quality/dashboards/timetravel`
(domain `KpiAsOfSnapshot`/`KpiAsOfRepository` purs / application `TimeTravelService`
/ infrastructure adapter + JPA projection / web), endpoint :

| Verbe | Chemin | Rôle |
|---|---|---|
| GET | `/api/v1/dashboards/time-travel/kpis?asOf=<ISO-8601>` | snapshot as-of du tenant |

L'as-of résout, **pour chaque KPI ACTIVE du tenant**, sa dernière mesure dont
`period_start ≤ asOf` (sinon snapshot « absent »). Implémenté par une requête
SQL native (LEFT JOIN LATERAL « dernière mesure ≤ asOf ») sur
`kpi_definitions`+`kpi_measurements`, **toujours filtrée `tenant_id`** (issu du JWT
via le `dashboardsTenantContextProvider` existant). Les dates futures sont bornées
à « maintenant » (le time-travel concerne le passé). Quand aucune mesure n'existe à
la date, la réponse porte `empty=true` et chaque KPI `present=false` → l'UI affiche
un **état vide soigné** (bandeau « aucune mesure à cette date » / valeurs « n/d »).

Côté front, `TimeTravelService` (HTTP) + un sélecteur de date dans l'en-tête du
dashboard exécutif ; le résultat s'affiche dans un bandeau as-of. Aucune donnée
n'est fabriquée côté client.

## Justification

- **Lecture sur l'existant** : `kpi_measurements` est déjà la source de vérité
  time-series par tenant — pas de nouveau schéma, pas de duplication d'état.
- **LATERAL « dernière mesure ≤ asOf »** : exprime exactement la sémantique as-of
  en une requête indexée (`idx_kpi_measure_tenant_kpi_period`), O(n KPIs).
- **Empty-state explicite** : `empty`/`present` rendent l'absence de donnée
  déterministe et testable, plutôt qu'une liste vide ambiguë.
- **Bornage au présent** : un time-travel vers le futur n'a pas de sens ; on
  clampe à `now` plutôt que de lever une erreur, pour une UX douce.

## Conséquences

- ✅ État historique réel, multi-tenant, sans simulation front.
- ✅ Réutilise la persistance KPI sans la modifier (couplage en lecture seule).
- ⚠ Requête native PostgreSQL (LATERAL) — portable Postgres, pas H2 ; les tests
  couvrent le service et l'adapter par projection mockée (pas d'intégration DB ici).
- ⚠ Snapshot limité aux KPIs ACTIVE (cohérent avec le catalogue affiché) ; les KPIs
  archivés sont volontairement exclus.

## Tests d'invariant

- `TimeTravelServiceTest` : valeur présente, état vide (date trop ancienne / aucun
  KPI), bornage des dates futures à `now`, rejet de `asOf` null.
- `KpiAsOfRepositoryAdapterTest` : projection présente → snapshot présent ; valeur
  nulle → snapshot absent.
- Golden-master `DashboardInteractivityGoldenPathTest` : as-of présent puis date
  ancienne → état vide.
- Couverture JaCoCo du package `dashboards/timetravel` : 100 % lignes & branches.

## Références

CLAUDE.md §7.3 (time-travel), §6.4 (KPI time-series), §18.2 #2 (tenant via JWT) ;
ADR 0001 (multi-tenant JWT), 0034 (interactivité dashboards).
