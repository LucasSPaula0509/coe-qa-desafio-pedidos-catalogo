// CT-PRF-006 — Criação independente do número de itens (anti-N+1, RN-09).
// 10 VUs por 3 min: metade cria pedidos de 1 item, metade de 3 itens (máximo de produtos válidos).
// Critério: diferença de p95 entre os grupos < 20% (uma única chamada em lote por pedido).

import { sleep } from 'k6';
import { createOrder } from '../lib/operations.js';
import { p95, summaryHandler } from '../lib/summary.js';

const duration = __ENV.DURATION || '3m';

export const options = {
  scenarios: {
    one_item: { executor: 'constant-vus', vus: 5, duration, exec: 'oneItem' },
    three_items: { executor: 'constant-vus', vus: 5, duration, exec: 'threeItems' },
  },
  thresholds: {
    'http_req_duration{items:1}': ['p(95)<800'],
    'http_req_duration{items:3}': ['p(95)<800'],
    checks: ['rate>0.99'],
  },
};

export function oneItem() {
  createOrder(1, { items: '1' });
  sleep(0.5);
}

export function threeItems() {
  createOrder(3, { items: '3' });
  sleep(0.5);
}

export const handleSummary = summaryHandler('items-scaling', 'CT-PRF-006', (data) => {
  const one = p95(data, 'http_req_duration{items:1}');
  const three = p95(data, 'http_req_duration{items:3}');
  const diff = one ? ((three - one) / one) * 100 : NaN;
  const verdict = Math.abs(diff) < 20 ? 'OK (< 20%)' : 'ATENÇÃO: diferença ≥ 20%';
  return `p95 1 item ${one?.toFixed(1)} ms × 3 itens ${three?.toFixed(1)} ms → diferença ${diff.toFixed(1)}% — ${verdict}`;
});
