// CT-PRF-004 — Pico: 5 → 150 VUs em 10 s, 1 min de pico, volta a 5 VUs.
// Durante o pico: nenhum 5xx além das classificações documentadas (503/504).
// Recuperação: no último minuto (cenário "recovery"), p95 de cada operação de volta ao NF-01.

import { mixedIteration, servicesHealthy } from '../lib/operations.js';
import { sloThresholds } from '../lib/config.js';
import { summaryHandler } from '../lib/summary.js';

const recoveryThresholds = {};
for (const [metric, rules] of Object.entries(sloThresholds())) {
  if (metric.startsWith('http_req_duration{name:')) {
    recoveryThresholds[metric.replace('{name:', '{scenario:recovery,name:')] = rules;
  }
}

export const options = {
  scenarios: {
    spike: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '30s', target: 5 },
        { duration: '10s', target: 150 },
        { duration: '1m', target: 150 },
        { duration: '10s', target: 5 },
        { duration: '2m', target: 5 },
      ],
    },
    recovery: { executor: 'constant-vus', vus: 5, duration: '1m', startTime: '4m' },
  },
  thresholds: {
    unclassified_5xx: ['count==0'],
    ...recoveryThresholds,
  },
};

export default function () {
  mixedIteration();
}

export function teardown() {
  servicesHealthy();
}

export const handleSummary = summaryHandler('spike', 'CT-PRF-004');
