// ===========================================================================
// Test de performance k6 — Standards Hub : listing → drill-down détail.
//
// Reproduit le parcours « ouvrir le catalogue normatif puis consulter une
// fiche de norme » (§8.4). Le détail (StandardDetail) est le plus lourd à
// sérialiser (sections → clauses → exigences imbriquées), c'est donc un
// chemin chaud à surveiller spécifiquement.
//
//   GET /api/v1/standards            → Page<StandardSummary>
//   GET /api/v1/standards/{id}       → StandardDetail (arborescence)
//
// LECTURE SEULE.
//
//   docker run --rm -i --network=host grafana/k6 run - \
//     < apps/api-quality-engine/perf/standards-drilldown.js
// ===========================================================================

import http from 'k6/http';
import { check, group, fail, sleep } from 'k6';
import { config } from './lib/config.js';
import { fetchToken, authHeaders } from './lib/auth.js';
import { perEndpointThresholds } from './lib/thresholds.js';
import { checkPage } from './lib/checks.js';

const VUS = parseInt(__ENV.VUS || '8', 10);
const DURATION = __ENV.DURATION || '30s';

export const options = {
  scenarios: {
    standards: {
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
  thresholds: perEndpointThresholds(['standards_list', 'standards_detail']),
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

// setup() : token + collecte des IDs de normes pour le drill-down.
export function setup() {
  const token = fetchToken();
  const headers = authHeaders(token);
  const res = http.get(
    `${config.baseUrl}/api/v1/standards?page=0&size=${config.pageSize}`,
    { headers, tags: { name: 'standards_list' } },
  );
  if (res.status !== 200) {
    fail(`setup: listing standards a échoué (status ${res.status})`);
  }
  let ids = [];
  try {
    const content = res.json('content') || [];
    ids = content.map((s) => s.id).filter((id) => !!id);
  } catch (_e) {
    ids = [];
  }
  if (ids.length === 0) {
    fail('setup: aucune norme retournée — données de démo absentes ?');
  }
  return { token, ids };
}

export default function (data) {
  const headers = authHeaders(data.token);
  const base = config.baseUrl;

  group('standards-list', () => {
    const res = http.get(`${base}/api/v1/standards?page=0&size=${config.pageSize}`, {
      headers,
      tags: { name: 'standards_list' },
    });
    checkPage(res, 'standards-list');
  });

  group('standards-detail', () => {
    // On pioche un ID aléatoire dans le catalogue pour répartir la charge.
    const id = data.ids[Math.floor(Math.random() * data.ids.length)];
    const res = http.get(`${base}/api/v1/standards/${id}`, {
      headers,
      tags: { name: 'standards_detail' },
    });
    check(res, {
      'standards-detail: status 200': (r) => r.status === 200,
      'standards-detail: id présent': (r) => {
        try {
          return r.json('id') === id;
        } catch (_e) {
          return false;
        }
      },
    });
  });

  sleep(Math.random() * 0.5 + 0.5);
}
