// CT-PRF-008 — Listagem com volume: base com ≥ MIN_ORDERS pedidos (10.000), 20 VUs por 5 min,
// páginas aleatórias com size 20 e 100. Critério: size=20 dentro do NF-01; size=100 < 2× SLO.
// O setup completa a massa via API (lotes paralelos), de forma idempotente.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { ORDER_URL } from '../lib/config.js';
import { summaryHandler } from '../lib/summary.js';

const MIN_ORDERS = Number(__ENV.MIN_ORDERS || 10000);
const BATCH = 50;
const JSON_HEADERS = { 'Content-Type': 'application/json' };

export const options = {
  setupTimeout: '30m',
  scenarios: {
    volume: { executor: 'constant-vus', vus: 20, duration: __ENV.DURATION || '5m' },
  },
  thresholds: {
    'http_req_duration{name:GET /orders size=20}': ['p(95)<400'],
    'http_req_duration{name:GET /orders size=100}': ['p(95)<800'],
    'http_req_failed{scenario:volume}': ['rate<0.01'],
    'checks{scenario:volume}': ['rate>0.99'],
  },
};

function totalElements() {
  return http.get(`${ORDER_URL}/api/v1/orders?page=0&size=1`, { tags: { name: 'setup' } }).json('totalElements');
}

export function setup() {
  let current = totalElements();
  while (current < MIN_ORDERS) {
    const requests = [];
    for (let i = 0; i < Math.min(BATCH, MIN_ORDERS - current); i++) {
      const runId = `vol.${current + i}.${Date.now()}`;
      const body = {
        client: { name: `Volume ${runId}`, document: '12345678901', email: `${runId}@example.com` },
        items: [{ productId: 'PROD-002', quantity: 1 }],
      };
      requests.push(['POST', `${ORDER_URL}/api/v1/orders`, JSON.stringify(body), { headers: JSON_HEADERS, tags: { name: 'setup' } }]);
    }
    http.batch(requests);
    current = totalElements();
  }
  return { totalElements: current };
}

export default function (data) {
  const size = Math.random() < 0.5 ? 20 : 100;
  const lastPage = Math.max(0, Math.floor((data.totalElements - 1) / size));
  const page = Math.floor(Math.random() * (lastPage + 1));
  const name = `GET /orders size=${size}`;
  const response = http.get(`${ORDER_URL}/api/v1/orders?page=${page}&size=${size}`, { tags: { name } });
  check(response, {
    [`${name}: 200`]: (r) => r.status === 200,
    [`${name}: página cheia ou última`]: (r) => r.json('content').length === size || page === lastPage,
  });
  sleep(0.5 + Math.random());
}

export const handleSummary = summaryHandler('volume', 'CT-PRF-008');
