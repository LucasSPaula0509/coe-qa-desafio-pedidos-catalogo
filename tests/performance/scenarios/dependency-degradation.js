// CT-PRF-007 — Degradação da dependência: carga nominal com +1 s de latência no catálogo (proxy).
// Criação: p95 ≈ SLO + 1 s, sem erros. Leituras de pedidos não dependem do catálogo: p95 no NF-01.
// Requer o override de teste (proxy WireMock). O stub é criado no setup e removido no teardown.

import http from 'k6/http';
import { mixedIteration } from '../lib/operations.js';
import { PROXY_ADMIN_URL, sloThresholds, SUMMARY_TREND_STATS } from '../lib/config.js';
import { summaryHandler } from '../lib/summary.js';

const DELAY_MS = 1000;
const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

export const options = {
  summaryTrendStats: SUMMARY_TREND_STATS,
  scenarios: {
    degraded: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 30 },
        { duration: __ENV.HOLD || '4m', target: 30 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: sloThresholds({ extraLatency: { 'POST /orders': DELAY_MS } }),
};

export function setup() {
  const stub = {
    priority: 1,
    request: { method: 'POST', urlPath: '/api/v1/products/prices' },
    response: { proxyBaseUrl: 'http://catalog-service:8081', fixedDelayMilliseconds: DELAY_MS },
    metadata: { qa: 'fault' },
  };
  const response = http.post(`${PROXY_ADMIN_URL}/__admin/mappings`, JSON.stringify(stub), JSON_HEADERS);
  if (response.status !== 201) {
    throw new Error(`Não foi possível injetar a latência no proxy: ${response.status}`);
  }
}

export default function () {
  mixedIteration();
}

export function teardown() {
  http.post(
    `${PROXY_ADMIN_URL}/__admin/mappings/remove-by-metadata`,
    JSON.stringify({ matchesJsonPath: { expression: '$.qa', equalTo: 'fault' } }),
    JSON_HEADERS,
  );
}

export const handleSummary = summaryHandler('dependency-degradation', 'CT-PRF-007');
