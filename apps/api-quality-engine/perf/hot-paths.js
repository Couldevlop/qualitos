// ===========================================================================
// Test de performance k6 — chemins chauds de l'API api-quality-engine.
//
// Couvre les endpoints de LECTURE les plus sollicités (listing paginé) des
// modules cœur, avec un gate SLO bloquant (CLAUDE.md §14.3 / §2.1) :
//   - GET /api/v1/standards            (Standards Hub, §8)
//   - GET /api/v1/industry-packs       (Domain Adapter Layer, §5)
//   - GET /api/v1/pdca/cycles          (PDCA, §3.1)
//   - GET /api/v1/capa/cases           (CAPA, §4.2)
//   - GET /api/v1/nc                   (Non-Conformance, §4.3)
//   - GET /api/v1/audits/plans         (Audit Management, §4.4)
//
// LECTURE SEULE — aucun POST/PUT/DELETE : sûr à exécuter contre une stack dev
// partagée (ne crée/altère pas de données).
//
// Exécution locale (Docker, stack dev qui tourne) :
//   docker run --rm -i --network=host \
//     -e BASE_URL=http://localhost:8082 \
//     -e KEYCLOAK_URL=http://localhost:8080 \
//     grafana/k6 run - < apps/api-quality-engine/perf/hot-paths.js
//
// Exécution locale (k6 natif) :
//   k6 run apps/api-quality-engine/perf/hot-paths.js
// ===========================================================================

import http from 'k6/http';
import { group, sleep } from 'k6';
import { config } from './lib/config.js';
import { fetchToken, authHeaders } from './lib/auth.js';
import { perEndpointThresholds } from './lib/thresholds.js';
import { checkPage, checkListOrPage } from './lib/checks.js';

// Endpoints applicatifs taggués (pour les seuils par endpoint + le résumé).
const ENDPOINT_TAGS = [
  'standards_list',
  'industry_packs_list',
  'pdca_cycles_list',
  'capa_cases_list',
  'nc_list',
  'audits_plans_list',
];

// Profil de charge : montée douce → palier → descente. Volontairement modéré
// (poste de dev partagé / CI runner) ; surchargeable via VUS et DURATION.
const VUS = parseInt(__ENV.VUS || '10', 10);
const DURATION = __ENV.DURATION || '30s';

export const options = {
  // Si __ENV.STAGES_DISABLED est défini, on utilise un palier constant simple
  // (utile pour un smoke run rapide en CI). Sinon, un ramping VUs réaliste.
  scenarios: {
    hot_paths: __ENV.SMOKE
      ? {
          executor: 'constant-vus',
          vus: parseInt(__ENV.VUS || '3', 10),
          duration: __ENV.DURATION || '15s',
        }
      : {
          executor: 'ramping-vus',
          startVUs: 0,
          stages: [
            { duration: '10s', target: VUS },
            { duration: DURATION, target: VUS },
            { duration: '5s', target: 0 },
          ],
          gracefulRampDown: '5s',
        },
  },
  thresholds: perEndpointThresholds(ENDPOINT_TAGS),
  // Pas de réutilisation des connexions désactivée : on veut un comportement
  // proche du navigateur (keep-alive). discardResponseBodies pour économiser
  // la RAM côté générateur (on n'a besoin que du status + d'un check léger).
  discardResponseBodies: false,
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

// setup() s'exécute une fois : on mint le token et on le partage à tous les VUs.
export function setup() {
  const token = fetchToken();
  return { token };
}

export default function (data) {
  const headers = authHeaders(data.token);
  const sz = config.pageSize;
  const base = config.baseUrl;

  group('standards', () => {
    const res = http.get(`${base}/api/v1/standards?page=0&size=${sz}`, {
      headers,
      tags: { name: 'standards_list' },
    });
    checkPage(res, 'standards');
  });

  group('industry-packs', () => {
    const res = http.get(`${base}/api/v1/industry-packs?page=0&size=${sz}`, {
      headers,
      tags: { name: 'industry_packs_list' },
    });
    checkListOrPage(res, 'industry-packs');
  });

  group('pdca-cycles', () => {
    const res = http.get(`${base}/api/v1/pdca/cycles?page=0&size=${sz}`, {
      headers,
      tags: { name: 'pdca_cycles_list' },
    });
    checkPage(res, 'pdca-cycles');
  });

  group('capa-cases', () => {
    const res = http.get(`${base}/api/v1/capa/cases?page=0&size=${sz}`, {
      headers,
      tags: { name: 'capa_cases_list' },
    });
    checkPage(res, 'capa-cases');
  });

  group('non-conformities', () => {
    const res = http.get(`${base}/api/v1/nc?page=0&size=${sz}`, {
      headers,
      tags: { name: 'nc_list' },
    });
    checkPage(res, 'non-conformities');
  });

  group('audits-plans', () => {
    const res = http.get(`${base}/api/v1/audits/plans?page=0&size=${sz}`, {
      headers,
      tags: { name: 'audits_plans_list' },
    });
    checkPage(res, 'audits-plans');
  });

  // Temps de réflexion utilisateur (think time) : évite un martèlement
  // synthétique irréaliste et laisse respirer le pool de connexions.
  sleep(Math.random() * 0.5 + 0.5);
}
