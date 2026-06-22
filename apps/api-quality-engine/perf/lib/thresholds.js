// Budgets SLO partagés (CLAUDE.md §14.3 / §2.1 / §20).
//
//   - http_req_duration  p(95) < 300 ms   (SLO API)
//   - http_req_duration  p(99) < 800 ms   (SLO API)
//   - http_req_failed    < 1 %             (taux d'erreur)
//   - checks             > 99 %            (assertions fonctionnelles)
//
// `abortOnFail: true` n'est PAS activé : on laisse le scénario aller au bout
// pour obtenir des percentiles statistiquement valides, mais un dépassement de
// seuil fait sortir k6 en code != 0 → le job CI ÉCHOUE (gate SLO bloquant).
//
// NB : les seuils s'appliquent aux requêtes API (taguées) et EXCLUENT le mint
// de token Keycloak, qui n'est pas un chemin chaud applicatif. On filtre via
// un sous-ensemble tagué `{ name: 'keycloak_token' }` neutralisé.

export const sloThresholds = {
  // Latence globale des requêtes (toutes URLs confondues hors token, voir
  // le filtre ci-dessous appliqué à la métrique applicative).
  http_req_duration: ['p(95)<300', 'p(99)<800'],

  // Taux d'erreur HTTP (5xx, timeouts, connexions refusées) < 1 %.
  http_req_failed: ['rate<0.01'],

  // Les assertions fonctionnelles (status 200, forme Page Spring) doivent
  // passer à > 99 %.
  checks: ['rate>0.99'],
};

// Variante avec seuils ventilés par endpoint applicatif : permet d'isoler
// un endpoint lent dans le résumé. La latence du token Keycloak est suivie
// mais SANS seuil bloquant (elle ne fait pas partie du SLO API).
export function perEndpointThresholds(endpointTags) {
  const t = { ...sloThresholds };
  for (const name of endpointTags) {
    t[`http_req_duration{name:${name}}`] = ['p(95)<300', 'p(99)<800'];
  }
  // Token : observabilité uniquement (seuil large, non bloquant en pratique).
  t['http_req_duration{name:keycloak_token}'] = ['p(95)<2000'];
  return t;
}
