// Helpers d'authentification Keycloak pour les tests de performance k6.
//
// Le JWT porte le claim `tenant_id` (réf. realm-export.json, protocolMapper
// du client qualitos-web) : le multi-tenant est donc respecté de bout en bout —
// aucune requête perf ne lit le tenant depuis le corps (§18.2.2).

import http from 'k6/http';
import { check, fail } from 'k6';
import { config, tokenUrl } from './config.js';

// Mint un access_token via le password grant Keycloak.
// Appelé dans setup() pour que TOUS les VUs partagent le même token
// (le token est dérivé une fois et propagé via les data de setup) — on ne
// veut pas mesurer le débit de Keycloak ici, mais bien celui de l'API métier.
export function fetchToken() {
  const body = {
    grant_type: 'password',
    client_id: config.keycloak.clientId,
    username: config.keycloak.username,
    password: config.keycloak.password,
    scope: 'openid',
  };
  if (config.keycloak.clientSecret) {
    body.client_secret = config.keycloak.clientSecret;
  }

  const res = http.post(tokenUrl(), body, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    tags: { name: 'keycloak_token' },
  });

  const ok = check(res, {
    'keycloak token: status 200': (r) => r.status === 200,
    'keycloak token: access_token présent': (r) => {
      try {
        return !!r.json('access_token');
      } catch (_e) {
        return false;
      }
    },
  });

  if (!ok) {
    fail(
      `Échec d'obtention du token Keycloak (status ${res.status}). ` +
        `Vérifier KEYCLOAK_URL/realm/client/credentials. Corps: ${res.body}`,
    );
  }

  return res.json('access_token');
}

// En-têtes authentifiés à passer à chaque requête API.
export function authHeaders(token) {
  return {
    Authorization: `Bearer ${token}`,
    Accept: 'application/json',
  };
}
