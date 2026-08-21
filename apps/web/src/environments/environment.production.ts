// Cible: stack docker-compose locale (postgres + keycloak + api-core + api-quality-engine + web).
// En prod publique, remplacer apiBaseUrl et keycloak.issuer par les URLs publiques + reactiver TLS.
export const environment = {
  production: true,
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
