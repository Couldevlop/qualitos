# qualitos-web — Frontend Angular

Angular 18 + Material 3, NgModules, HTML/SCSS séparés.

## Démarrage

```bash
cd apps/web
nvm use            # 20.18.0 (cf. .nvmrc)
npm ci
npm start          # → http://localhost:4200
```

Au démarrage, l'app tourne en **mode démo** :
- `useMockApi = true` → données fictives, pas de backend requis
- `authMode = 'dev'` → utilisateur fictif, JWT de dev pour transporter `tenant_id`

## Branchement sur le vrai backend (api-quality-engine)

Éditer `src/environments/environment.ts` :

```ts
useMockApi: false,            // bascule sur les vrais endpoints
apiBaseUrl: 'http://localhost:8082',
```

Le service `api-quality-engine` doit tourner en parallèle (`mvn spring-boot:run` côté `apps/api-quality-engine`).

## Branchement Keycloak (production)

```ts
authMode: 'oidc',
keycloak: {
  issuer: 'https://auth.qualitos.io/realms/qualitos',
  clientId: 'qualitos-web',
  ...
}
```

Le wiring `angular-oauth2-oidc` est prévu dans `AuthService.getAccessToken()` (TODO).

## Conventions Angular

- **Pas de standalone components** — tout est déclaré dans un `@NgModule`.
- **HTML et SCSS séparés** — toujours `templateUrl` / `styleUrls`, jamais inline.
- **Modules** :
  - `CoreModule` : singleton (auth, interceptor) — importé une seule fois par `AppModule`.
  - `SharedModule` : re-exports Material + RxJS utilitaires — importé par chaque feature.
  - `features/<name>/<Name>Module` : lazy-loaded depuis `AppRoutingModule`.
- **Selector prefix** : `qos-`.

## Build & tests

```bash
npm run build            # production build dans dist/qualitos-web
npm test                 # unit tests Karma + Jasmine (Chrome headless en CI)
```

Le pipeline GitHub Actions (`.github/workflows/ci.yml`) builde + teste le front sur chaque PR.
