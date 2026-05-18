// Cible: stack docker-compose locale (postgres + keycloak + api-core + api-quality-engine + web).
// En prod publique, remplacer apiBaseUrl et keycloak.issuer par les URLs publiques + reactiver TLS.
export const environment = {
  production: true,
  useMockApi: false,
  apiBaseUrl: 'http://localhost:8082',
  authMode: 'oidc' as 'dev' | 'oidc',
  keycloak: {
    issuer: 'http://localhost:8080/realms/qualitos',
    clientId: 'qualitos-web',
    redirectUri: window.location.origin + '/',
    scope: 'openid profile email'
  }
};
