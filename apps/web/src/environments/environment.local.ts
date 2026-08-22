// Config dev INTÉGRÉE : front branché sur le vrai backend local (api-quality-engine
// sur :8082) + Keycloak local (:8080), sans mock, sans optimisation prod.
// Usage : `ng serve --configuration local`. Login Keycloak réel (ex: demo/demo).
export const environment = {
  production: false,
  useMockApi: false,
  apiBaseUrl: 'http://localhost:8082',
  authMode: 'oidc' as 'dev' | 'oidc',
  /**
   * Palier d'authentification demandé quand le serveur exige un second
   * facteur (403 « step-up-required »). Doit correspondre à la carte
   * `acr.loa.map` du realm — cf. infra/keycloak/realm-export.json.
   */
  stepUpAcrValue: 'gold',
  keycloak: {
    issuer: 'http://localhost:8080/realms/qualitos',
    clientId: 'qualitos-web',
    redirectUri: window.location.origin + '/',
    scope: 'openid profile email'
  }
};
