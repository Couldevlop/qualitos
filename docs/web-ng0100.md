# NG0100 — `deferredView` pour les états `loading$` / `error$`

> `ExpressionChangedAfterItHasBeenChecked` (NG0100) : éradication systémique sur
> les ~70 pages list/detail des features.

## Symptôme

Un composant expose `loading$ = new BehaviorSubject<boolean>(false)` consommé par
`loading$ | async` **en haut** du template, mais l'écrit dans `tap` / `finalize` /
`catchError` du flux de données consommé **plus bas** (`data$ | async`). Pendant
une passe de détection, l'évaluation du binding `data` déclenche de façon
**synchrone** `finalize(() => loading$.next(false))` *après* que le binding
`loading` a déjà été vérifié → NG0100. Idem pour `error$`. Le vieux hack
`queueMicrotask(() => this.loading$.next(true))` ne couvre pas le chemin
`finalize`/`catchError`.

## Correctif canonique — `deferredView`

`apps/web/src/app/core/rx/deferred-view.ts` :

```ts
export function deferredView<T>(state: BehaviorSubject<T>): Observable<T> {
  return state.pipe(observeOn(asyncScheduler), distinctUntilChanged());
}
```

La **consommation** (l'async pipe) est livrée en **macrotâche** (`asyncScheduler`),
donc dans un cycle de détection séparé — jamais pendant la passe courante. L'ordre
des transitions est préservé.

## Pattern à appliquer (toute nouvelle page list/detail)

- État privé écrit par les opérateurs du flux + vue publique différée :

  ```ts
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);
  ```

- Écrire **directement** dans `*State$` (`this.loadingState$.next(true)`), sans
  `queueMicrotask`. Le nom **public** `loading$` / `error$` ne change pas → aucun
  changement de template.
- Ne **pas** différer les subjects écrits uniquement depuis des handlers
  d'événements (clicks) — ex. `acting$`, `uploading$`, `photos$`,
  `storageDisabled$`. Ils ne provoquent pas de NG0100.

## Tests `fakeAsync`

`loading$` / `error$` émettant via `asyncScheduler`, les assertions sur le DOM
piloté par leur valeur doivent **drainer la macrotâche** :

- `tick()` (ou `flush()`) après l'action, **avant** `fixture.detectChanges()` qui
  rend le gate `(loading$ | async) === false`.
- `discardPeriodicTasks()` en fin de test (l'`asyncScheduler` laisse un timer
  périodique dans la file fakeAsync).

Exemple : `apps/web/src/app/features/offline-queue/pages/offline-queue/offline-queue.component.spec.ts`.
