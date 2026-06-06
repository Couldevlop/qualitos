# PWA & mode hors-ligne terrain — apps/web

> CLAUDE.md §15.2-15.3 : offline-first terrain (audits 5S, audits, non-conformités) en zone blanche.

## Ce qui est livré

### PWA installable
- `manifest.webmanifest` (icônes 192/512, standalone, theme-color DS) + meta dans `index.html`.
- Service Worker Angular (`@angular/service-worker`) **actif en build production
  uniquement** (`angular.json` → `"serviceWorker": "ngsw-config.json"`).
- `ngsw-config.json` : app shell en prefetch, assets lazy, données API 5S/KPI/
  standards en stratégie *freshness* (réseau d'abord, cache 1 j en secours).

### File d'attente hors-ligne (écritures 5S)
```
saisie terrain ──offline──▶ OfflineQueueService ──▶ IndexedDB (qualitos-offline)
                                   │ retour réseau ('online')
                                   └──▶ rejeu ordonné via HttpClient
                                         (ApiInterceptor ⇒ token FRAIS au rejeu)
```
- `core/offline/` : `ConnectivityService` (état réseau injectable),
  `OfflineQueueStore` (port + adaptateurs IndexedDB/mémoire, auto-fourni root),
  `OfflineQueueService` (enqueue / replay / `pendingCount$` / `events$`).
- `FivesService` : `createAudit` et `scorePillar` basculent en file quand
  hors-ligne **ou** sur coupure pendant l'envoi (status 0) ; réponse optimiste
  marquée `pendingSync: true`.
- `AuditsService` : mêmes garanties pour les **écritures terrain** d'un audit —
  `respondChecklistItem` (réponse à un item de checklist en zone blanche) et
  `addFinding` (constat soulevé sur le terrain). Le back-office reste online-only
  (planification, checklist préparée, transitions, validation manager). Labels de
  file non-PII (`plan <id>` / `item <id>`, jamais de nom d'auditeur ou d'audité).
- `NcService` : `createNc` (déclaration d'une non-conformité sur le terrain, §4.3)
  bascule en file quand hors-ligne **ou** sur coupure pendant l'envoi (status 0) ;
  réponse optimiste `pendingSync: true` (id `offline-<opId>`, référence
  `NC-EN-ATTENTE`). Les transitions de workflow (analyse, action, résolution,
  clôture, annulation) et l'escalade CAPA restent **online-only** (back-office).
  Label de file non-PII (`catégorie (sévérité)`, jamais le titre saisi).
- Shell : chip topbar `cloud_off` / `cloud_sync n` (hors-ligne / n actions en
  attente), `role="status"` accessible — **cliquable → page `/offline-queue`**.

### Page « File d'attente » (`/offline-queue`)
- Feature lazy `features/offline-queue` : liste des opérations en attente
  (label non-PII, méthode, URL, horodatage), bandeau d'état réseau.
- **Synchroniser maintenant** : relance `replay()` manuellement (utile si le
  navigateur n'a pas émis l'événement `online`).
- **Abandonner** (par opération, dialog de confirmation destructive) :
  `OfflineQueueService.discard(id)` retire l'op définitivement et émet
  l'événement `discarded`.
- La liste se rafraîchit sur chaque événement de la file (`events$`) — pas de
  polling. i18n complet (`offline.queue.*`, 6 langues).

## Sémantique de rejeu
| Situation au rejeu | Comportement |
|---|---|
| Succès API | op retirée, événement `replayed` |
| Erreur réseau (status 0) | rejeu STOPPÉ, ops conservées (retentera au prochain `online`) |
| Erreur applicative 4xx/5xx | op retirée + `replay-failed` (pas de boucle infinie) |

## Sécurité
- **Aucun token stocké** dans la file : l'Authorization est posée par
  l'intercepteur au moment du rejeu (OWASP A02).
- `label` (affiché/journalisé) ne contient jamais de PII ; le `body` reste
  confiné à IndexedDB du poste.
- Le SW ne met en cache que des GET ; les écritures passent toujours réseau.

## Vérifier à la main
1. `ng build` (production) puis servir `dist/` — DevTools → Application :
   manifest + SW `ngsw-worker.js` actifs.
2. DevTools → Network → Offline : saisir un score 5S → chip `cloud_off 1`,
   réponse optimiste immédiate.
3. Repasser Online : la requête part (onglet Network), le chip disparaît.

## Limites connues / suite
- Icônes = placeholder monogramme « Q » (générées) — à remplacer par le logo DS.
- Couverture offline : flux 5S terrain (create + score), audits terrain
  (réponse checklist + constat) et non-conformités terrain (déclaration). L'upload
  binaire des photos (saisie d'URLs pour l'instant) viendra ensuite.
