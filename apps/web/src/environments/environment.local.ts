// Config dev INTÉGRÉE : front branché sur le vrai backend local (api-quality-engine
// sur :8082) + Keycloak local (:8080), sans mock, sans optimisation prod.
// Usage : `ng serve --configuration local`. Login Keycloak réel (ex: demo/demo).
export const environment = {
  production: false,
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
