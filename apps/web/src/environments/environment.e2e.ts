// Config E2E (Playwright, CI) : donnees mock + auth bypassee.
// L'app tourne 100 % sans backend ni Keycloak — on valide le shell, le routing
// et le rendu des features cles, pas l'integration serveur.
export const environment = {
  production: false,
  useMockApi: true,
  apiBaseUrl: 'http://localhost:8082',
  authMode: 'dev' as 'dev' | 'oidc',
  /**
   * Palier d'authentification demandé quand le serveur exige un second
   * facteur (403 « step-up-required »). Doit correspondre à la carte
   * `acr.loa.map` du realm — cf. infra/keycloak/realm-export.json.
   */
  stepUpAcrValue: 'gold',
  keycloak: {
    issuer: 'http://localhost:8080/realms/qualitos',
    clientId: 'qualitos-web',
    redirectUri: 'http://localhost:4200/',
    scope: 'openid profile email'
  }
};
