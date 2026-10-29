// CT-PRF-002 — Carga nominal (gate na develop e no release).
// Rampa 0 → 30 VUs em 2 min; 30 VUs por 10 min; descida em 1 min. Erro < 1%, checks > 99%,
// p95/p99 por operação dentro do NF-01. HOLD permite encurtar a sustentação (ex.: HOLD=3m).

import { mixedIteration } from '../lib/operations.js';
import { sloThresholds, SUMMARY_TREND_STATS } from '../lib/config.js';
import { summaryHandler } from '../lib/summary.js';

export const options = {
  summaryTrendStats: SUMMARY_TREND_STATS,
  scenarios: {
    load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 30 },
        { duration: __ENV.HOLD || '10m', target: 30 },
        { duration: '1m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: sloThresholds(),
};

export default function () {
  mixedIteration();
}

export const handleSummary = summaryHandler('load', 'CT-PRF-002');
