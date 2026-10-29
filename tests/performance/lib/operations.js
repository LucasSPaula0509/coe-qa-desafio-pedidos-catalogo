// Operações do modelo de carga (docs/test-cases/performance.md), cada uma com checks funcionais:
// uma resposta rápida e errada conta como falha (checks + http_req_failed).

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { CATALOG_URL, ORDER_URL } from './config.js';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

/** Produtos da massa inicial (RN-22). Válidos para pedido: 001, 002, 003. */
export const ORDERABLE = ['PROD-001', 'PROD-002', 'PROD-003'];
export const ALL_SEED = ['PROD-001', 'PROD-002', 'PROD-003', 'PROD-004', 'PROD-005', 'PROD-006'];

/** 5xx que não são classificações documentadas de dependência (503/504) — CT-PRF-004. */
export const unclassified5xx = new Counter('unclassified_5xx');

/** Pedidos criados por este VU, para alimentar as consultas de detalhe. */
const createdIds = [];

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function pick(list, count) {
  const copy = [...list];
  const chosen = [];
  while (chosen.length < count && copy.length > 0) {
    chosen.push(copy.splice(randomInt(0, copy.length - 1), 1)[0]);
  }
  return chosen;
}

function cents(value) {
  return Math.round(value * 100);
}

function track(response) {
  if (response.status >= 500 && response.status !== 503 && response.status !== 504) {
    unclassified5xx.add(1, { name: response.request ? response.tags.name : 'unknown' });
  }
  return response;
}

function json(response) {
  try {
    return response.json();
  } catch (e) {
    return null;
  }
}

/** POST /api/v1/orders com 1 a {maxItems} produtos válidos distintos (quantidade 1 a 5). */
export function createOrder(itemCount = randomInt(1, 3), extraTags = {}) {
  const items = pick(ORDERABLE, itemCount).map((productId) => ({ productId, quantity: randomInt(1, 5) }));
  const runId = `${__VU}.${__ITER}.${Date.now()}`;
  const payload = {
    client: { name: `Carga ${runId}`, document: '12345678901', email: `perf.${runId}@example.com` },
    items,
  };
  const response = track(
    http.post(`${ORDER_URL}/api/v1/orders`, JSON.stringify(payload), {
      headers: JSON_HEADERS,
      tags: { name: 'POST /orders', ...extraTags },
    }),
  );
  const order = json(response);
  const ok = check(response, {
    'POST /orders: 201': (r) => r.status === 201,
    'POST /orders: status CRIADO': () => order !== null && order.status === 'CRIADO',
    'POST /orders: total = subtotal - desconto': () =>
      order !== null && cents(order.total) === cents(order.subtotal) - cents(order.discount),
    'POST /orders: itens com snapshot': () => order !== null && order.items.length === items.length,
  });
  if (ok && order) {
    createdIds.push(order.id);
    if (createdIds.length > 50) {
      createdIds.shift();
    }
  }
  return order;
}

/** GET /api/v1/orders (página 0 a 2). */
export function listOrders(size = 20, name = 'GET /orders') {
  const page = randomInt(0, 2);
  const response = track(
    http.get(`${ORDER_URL}/api/v1/orders?page=${page}&size=${size}`, { tags: { name } }),
  );
  const body = json(response);
  check(response, {
    [`${name}: 200`]: (r) => r.status === 200,
    [`${name}: página com até ${size} itens`]: () =>
      body !== null && Array.isArray(body.content) && body.content.length <= size,
  });
  return body;
}

/** GET /api/v1/orders/{id} de um pedido criado por este VU. */
export function getOrder() {
  if (createdIds.length === 0) {
    createOrder();
  }
  const id = createdIds[randomInt(0, createdIds.length - 1)];
  if (!id) {
    return;
  }
  const response = track(http.get(`${ORDER_URL}/api/v1/orders/${id}`, { tags: { name: 'GET /orders/{id}' } }));
  const body = json(response);
  check(response, {
    'GET /orders/{id}: 200': (r) => r.status === 200,
    'GET /orders/{id}: mesmo pedido': () => body !== null && body.id === id,
  });
}

/** GET /api/v1/products/{id} de um produto da massa. */
export function getProduct() {
  const id = ALL_SEED[randomInt(0, ALL_SEED.length - 1)];
  const response = track(http.get(`${CATALOG_URL}/api/v1/products/${id}`, { tags: { name: 'GET /products/{id}' } }));
  const body = json(response);
  check(response, {
    'GET /products/{id}: 200': (r) => r.status === 200,
    'GET /products/{id}: produto correto': () => body !== null && body.id === id,
  });
}

/** POST /api/v1/products/prices com 2 a 6 IDs da massa. */
export function batchPrices() {
  const ids = pick(ALL_SEED, randomInt(2, 6));
  const response = track(
    http.post(`${CATALOG_URL}/api/v1/products/prices`, JSON.stringify({ productIds: ids }), {
      headers: JSON_HEADERS,
      tags: { name: 'POST /products/prices' },
    }),
  );
  const body = json(response);
  check(response, {
    'POST /products/prices: 200': (r) => r.status === 200,
    'POST /products/prices: todos os IDs respondidos': () =>
      body !== null && body.prices.length + body.notFound.length === ids.length,
  });
}

/**
 * Uma iteração do mix nominal (docs/test-cases/performance.md): 30% listagem, 20% detalhe,
 * 15% produto, 15% lote, 20% criação, com think time de 1 a 3 s (comportamento de usuário).
 */
export function mixedIteration() {
  const roll = Math.random();
  if (roll < 0.3) {
    listOrders();
  } else if (roll < 0.5) {
    getOrder();
  } else if (roll < 0.65) {
    getProduct();
  } else if (roll < 0.8) {
    batchPrices();
  } else {
    createOrder();
  }
  sleep(1 + Math.random() * 2);
}

/** Saúde dos dois serviços (usada no teardown de estresse/pico). */
export function servicesHealthy() {
  const order = http.get(`${ORDER_URL}/actuator/health`, { tags: { name: 'health' } });
  const catalog = http.get(`${CATALOG_URL}/actuator/health`, { tags: { name: 'health' } });
  return check(null, {
    'order-service UP ao final': () => order.status === 200 && json(order)?.status === 'UP',
    'catalog-service UP ao final': () => catalog.status === 200 && json(catalog)?.status === 'UP',
  });
}
