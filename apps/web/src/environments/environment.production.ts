export const environment = {
  production: true,
  useMockApi: false,
  apiBaseUrl: '/api',
  authMode: 'oidc' as 'dev' | 'oidc',
  keycloak: {
    issuer: 'https://auth.qualitos.io/realms/qualitos',
    clientId: 'qualitos-web',
    redirectUri: window.location.origin + '/',
    scope: 'openid profile email'
  }
};
