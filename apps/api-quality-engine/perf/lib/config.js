// Configuration partagée des tests de performance k6 — api-quality-engine.
//
// Tout est paramétrable par variable d'environnement (k6 -e KEY=VALUE ou env du
// shell), avec des valeurs par défaut alignées sur la stack de DEV locale
// (engine :8082, Keycloak :8080, user demo/demo, realm qualitos).
//
// AUCUN secret en clair : le mot de passe par défaut `demo` est une crédential
// de DÉMO publique (réf. infra/keycloak/realm-export.json, allowlistée gitleaks).
// En CI/staging, fournir K6_PASSWORD via un secret GitHub Actions.

// __ENV est l'objet d'environnement injecté par k6.
const E = __ENV;

export const config = {
  // URL de base de l'API api-quality-engine.
  baseUrl: E.BASE_URL || 'http://localhost:8082',

  // Keycloak (émetteur du JWT). On compose le token endpoint à partir de
  // l'URL + realm pour rester DRY.
  keycloak: {
    url: E.KEYCLOAK_URL || 'http://localhost:8080',
    realm: E.KEYCLOAK_REALM || 'qualitos',
    // Client public avec directAccessGrantsEnabled=true (password grant).
    clientId: E.KEYCLOAK_CLIENT_ID || 'qualitos-web',
    // Client confidentiel : laisser vide pour un client public.
    clientSecret: E.KEYCLOAK_CLIENT_SECRET || '',
    username: E.K6_USERNAME || 'demo',
    password: E.K6_PASSWORD || 'demo',
  },

  // Taille de page demandée sur les endpoints paginés (Spring Data).
  pageSize: parseInt(E.PAGE_SIZE || '20', 10),
};

// Token endpoint OIDC (password grant).
export function tokenUrl() {
  return `${config.keycloak.url}/realms/${config.keycloak.realm}/protocol/openid-connect/token`;
}
