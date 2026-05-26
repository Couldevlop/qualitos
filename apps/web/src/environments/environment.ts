/**
 * Environnement par défaut (development).
 * - useMockApi: true → la couche service renvoie des données fictives (démo sans backend).
 *   Mettre à false pour pointer le vrai api-quality-engine.
 * - apiBaseUrl: utilisé quand useMockApi=false.
 * - keycloak: utilisé quand authMode='oidc'. En 'dev' on bypass l'auth pour tester l'UI.
 */
export const environment = {
  production: false,
  useMockApi: false,
  apiBaseUrl: 'http://localhost:8082',
  authMode: 'oidc' as 'dev' | 'oidc',
  keycloak: {
    issuer: 'http://localhost:8080/realms/qualitos',
    clientId: 'qualitos-web',
    redirectUri: 'http://localhost:4200/',
    scope: 'openid profile email'
  }
};
