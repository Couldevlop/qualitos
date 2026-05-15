# ADR 0003 — Services frontend en mode "mock-first" par défaut

- **Statut** : Accepté
- **Date** : 2026-05-14
- **Owners** : @Couldevlop

## Contexte

Le frontend Angular doit pouvoir être démontré et développé **sans dépendre**
d'un backend tournant. À l'inverse, en production il doit consommer le
vrai backend `api-quality-engine` via JWT/Keycloak.

Options :

1. Service avec **deux implémentations** (`MockXxxService`,
   `HttpXxxService`) et un token DI qui choisit selon l'environnement.
2. Un **seul service** avec une branche conditionnelle interne sur
   `environment.useMockApi`.
3. **Intercepteur HTTP de mock** (passthrough vers le vrai backend si
   `useMockApi=false`, sinon mock).

## Décision

**Option 2 retenue** pour le MVP : un seul service par module, avec dans
chaque méthode :

```ts
if (environment.useMockApi) {
  return of(this.mockX()).pipe(delay(150));
}
return this.http.get<...>(this.endpoint, { params });
```

Le toggle se fait dans `src/environments/environment.ts` :

```ts
useMockApi: true,            // démo offline
apiBaseUrl: 'http://localhost:8082',
```

## Justification

- ✅ **Démo immédiate** : `npm start` suffit pour montrer l'UI complète, sans
  Docker compose, sans Keycloak, sans Postgres. Idéal pour les revues UX
  et la démo client.
- ✅ **Une seule abstraction** : le composant ne sait pas qu'il y a un mock,
  donc les tests E2E (futurs Cypress) marchent dans les deux modes.
- ✅ **Bascule en une ligne** : changer `useMockApi: false` pointe sur le
  vrai backend sans redémarrer.
- ⚠ Le code mock vit dans le service même : alourdit les fichiers. Acceptable
  tant qu'on a ≤ 10 modules.

## Évolution prévue

À mesure que les modules se complexifient (filtres avancés, création,
écriture), on basculera vers l'option 1 (deux implémentations) avec un
token DI :

```ts
export const PDCA_SERVICE = new InjectionToken<PdcaService>('PdcaService');

providers: [
  environment.useMockApi
    ? { provide: PDCA_SERVICE, useClass: MockPdcaService }
    : { provide: PDCA_SERVICE, useClass: HttpPdcaService }
]
```

C'est aussi le moment où la couche état (NGRX ou signaux) sera introduite.

## Tests d'invariant

- En revue : refuser tout composant qui injecte `HttpClient` directement —
  toujours passer par le service du module.
- Les fonctions `mockX()` doivent rester pures (pas d'effet de bord) et
  retourner des `Observable` (pour matcher la signature du mode réel).

## Références

- CLAUDE.md §15 (UI/UX).
- ADR 0002 (architecture Angular en NgModules).
