# ADR 0002 — Angular en NgModules (pas de composants standalone)

- **Statut** : Accepté
- **Date** : 2026-05-14
- **Owners** : @Couldevlop

## Contexte

Angular 14+ a introduit les **standalone components** (composants qui se
déclarent indépendamment, sans `@NgModule`). En Angular 17+, c'est devenu
le mode par défaut pour `ng new` et la documentation officielle.

QualitOS doit choisir entre :

1. **Standalone components partout** (mode moderne, recommandé par l'équipe Angular).
2. **NgModules classiques** (`AppModule`, `CoreModule`, `SharedModule`, feature
   modules) avec `templateUrl` / `styleUrls` séparés.

## Décision

**Option 2 retenue** : NgModules classiques, HTML et SCSS toujours séparés du TS.

Politique appliquée :

- `main.ts` utilise `bootstrapModule(AppModule)`, jamais `bootstrapApplication`.
- Chaque composant a `templateUrl: './x.component.html'` et
  `styleUrls: ['./x.component.scss']`. Pas d'inline `template:` ou `styles:`.
- `standalone: false` explicite sur chaque `@Component`.
- Modules : `CoreModule` (singleton — interceptors, guards), `SharedModule`
  (re-exports Material), feature modules lazy-loaded via
  `loadChildren: () => import('...').then(m => m.XModule)`.

## Justification

Préférence durable de l'équipe (cf. mémoire utilisateur
`feedback-angular-no-standalone`). Raisons :

- **Séparation des préoccupations** : la logique TS, le template HTML et les
  styles SCSS sont 3 disciplines distinctes (typage, accessibilité,
  responsive). Les fichiers séparés rendent les responsabilités lisibles.
- **Tooling IDE** : autocomplétion, refactor et lint des templates sont
  plus mûrs sur des fichiers `.html` que sur des template-strings.
- **Lisibilité des modules** : un `@NgModule` documente explicitement les
  dépendances de la feature (imports, exports, declarations).
- **Cohérence projet** : un seul style architectural simplifie l'onboarding
  et le code review.

## Conséquences

- ✅ Build, tests et SSR fonctionnent normalement (Angular supporte les deux
  approches au moins jusqu'à v20).
- ✅ Le `SharedModule` factorise les imports Material — chaque feature en a
  besoin d'une seule ligne.
- ⚠ Plus de boilerplate : chaque feature exige son module + ses routes +
  ses composants déclarés. Compensé par les générateurs Angular CLI
  configurés dans `angular.json` (`"standalone": false` par défaut).
- ⚠ La doc Angular officielle pousse vers standalone : il faudra mentalement
  traduire les exemples.

## Tests d'invariant

- ESLint rule à ajouter dans une PR suivante : `@angular-eslint/no-standalone`
  (n'existe pas tel quel — règle custom à créer ou check via revue).
- En revue de code : refuser tout `standalone: true`, tout `template:` ou
  `styles:` inline.

## Références

- CLAUDE.md §18 (conventions de développement).
- Mémoire utilisateur : `feedback_angular_no_standalone.md`.
