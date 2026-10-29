// CT-PRF-001 — Smoke de performance (gate em todo PR).
// 2 VUs por 1 min com todas as operações do mix; 100% dos checks, 0% de erro, p95 dentro do NF-01.

import { mixedIteration } from '../lib/operations.js';
import { sloThresholds } from '../lib/config.js';
import { summaryHandler } from '../lib/summary.js';

export const options = {
  scenarios: {
    smoke: { executor: 'constant-vus', vus: 2, duration: __ENV.DURATION || '1m' },
  },
  thresholds: sloThresholds({ errorRate: 0.0001, checks: 0.9999 }),
};

export default function () {
  mixedIteration();
}

export const handleSummary = summaryHandler('smoke', 'CT-PRF-001');
