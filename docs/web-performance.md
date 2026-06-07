# Performance web — budgets & bundle initial

> CLAUDE.md §15.2 : LCP < 2 s, INP < 200 ms — le bundle initial conditionne le LCP.

## Budgets (angular.json, configuration production)

| Type | Warning | Error | Réel (2026-06-05) |
|---|---|---|---|
| `initial` | 900 kB | 1,2 MB | **877 kB** (198 kB transférés) |
| `anyComponentStyle` | 10 kB | 32 kB | max ~8,5 kB (main-shell) |

Historique : le bundle initial était à **1,50 MB** — ECharts + zrender (640 kB)
étaient embarqués en eager parce que `EchartComponent` (UiModule, importé par le
shell) importait `echarts/core` statiquement.

## Règle : ECharts est chargé dynamiquement

`shared/ui/echart/echart.component.ts` charge ECharts par `import()` au premier
rendu d'un graphique (singleton `loadEcharts()`, `use()` une seule fois). Le
chunk est partagé entre toutes les pages à graphiques et mis en cache.

**Ne jamais réintroduire un `import ... from 'echarts/...'` statique** dans du
code atteint par le shell (UiModule, layout, core) — seuls les `import type`
sont permis (effacés à la compilation). Même règle pour toute lib lourde
(AG Grid, JointJS, Plotly…) : import dynamique ou module Angular dédié lazy.

## Mesurer localement

Le build `production` échoue sur ce poste (inlining Google Fonts bloqué réseau).
Utiliser la configuration **`analyze`** (fonts non inlinées + `stats.json`) :

```bash
cd apps/web && npx ng build "--configuration=analyze"
# composition du bundle : dist/qualitos-web/stats.json (metafile esbuild)
```

NB : les budgets ne sont vérifiés que par la configuration `production` (CI).

## NG0100 (`ExpressionChangedAfterItHasBeenChecked`)

Les états `loading$` / `error$` des pages list/detail sont exposés via le helper
`deferredView` (`core/rx/deferred-view.ts`) pour éviter le NG0100 systémique —
voir [`web-ng0100.md`](./web-ng0100.md). Toute nouvelle page DOIT l'utiliser.
