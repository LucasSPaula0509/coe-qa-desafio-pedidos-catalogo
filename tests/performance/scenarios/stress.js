// CT-PRF-003 — Estresse (informativo): degraus de +30 VUs a cada STEP (3 min) até MAX_VUS (300).
// Objetivo: registrar a carga em que p95 > 2× SLO ou erro > 5% (capacidade máxima observada) e
// confirmar que os serviços seguem saudáveis ao final (teardown).

import { mixedIteration, servicesHealthy } from '../lib/operations.js';
import { SUMMARY_TREND_STATS } from '../lib/config.js';
import { summaryHandler } from '../lib/summary.js';

const maxVus = Number(__ENV.MAX_VUS || 300);
const step = __ENV.STEP || '3m';
const stages = [];
for (let vus = 30; vus <= maxVus; vus += 30) {
  stages.push({ duration: '30s', target: vus }, { duration: step, target: vus });
}
stages.push({ duration: '1m', target: 0 });

export const options = {
  summaryTrendStats: SUMMARY_TREND_STATS,
  scenarios: { stress: { executor: 'ramping-vus', startVUs: 0, stages } },
  // Limites de 2× o SLO: mostram onde a degradação começa (o resultado é informativo).
  thresholds: {
    'http_req_duration{name:POST /orders}': ['p(95)<1600'],
    'http_req_duration{name:GET /orders}': ['p(95)<800'],
    http_req_failed: ['rate<0.05'],
    checks: ['rate>0.95'],
  },
};

export default function () {
  mixedIteration();
}

export function teardown() {
  servicesHealthy();
}

export const handleSummary = summaryHandler('stress', 'CT-PRF-003');
