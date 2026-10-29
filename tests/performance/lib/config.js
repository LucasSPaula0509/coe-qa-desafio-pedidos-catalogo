// Configuração comum dos testes de performance (ADR-QA-004).
//
// Os endereços padrão são os nomes dos serviços na rede do Docker Compose (o k6 roda em container
// na mesma rede — local e CI se comportam igual). Sobrescreva com -e ORDER_URL=... etc.

export const ORDER_URL = __ENV.ORDER_URL || 'http://order-service:8080';
export const CATALOG_URL = __ENV.CATALOG_URL || 'http://catalog-service:8081';
export const PROXY_ADMIN_URL = __ENV.PROXY_ADMIN_URL || 'http://catalog-proxy:8080';

/** Estatísticas exibidas no resumo (o padrão do k6 omite o p99, que faz parte do SLO). */
export const SUMMARY_TREND_STATS = ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'];

/**
 * SLOs propostos (NF-01, docs/test-strategy.md §7) — premissa a homologar (Q-06).
 * Chave = tag "name" das requisições.
 */
export const SLO = {
  'POST /orders': { p95: 800, p99: 1500 },
  'GET /orders': { p95: 400 },
  'GET /orders/{id}': { p95: 300 },
  'GET /products/{id}': { p95: 200 },
  'POST /products/prices': { p95: 300 },
};

/**
 * Thresholds derivados do SLO. Um threshold violado faz o k6 terminar com código ≠ 0: é o gate.
 *
 * @param {object} options
 * @param {number} [options.errorRate=0.01] taxa máxima de requisições com falha
 * @param {number} [options.checks=0.99] taxa mínima de checks funcionais aprovados
 * @param {object} [options.extraLatency] ms adicionais permitidos por operação (ex.: latência injetada)
 * @param {string[]} [options.only] limita às operações exercitadas pelo cenário
 */
export function sloThresholds({ errorRate = 0.01, checks = 0.99, extraLatency = {}, only } = {}) {
  const thresholds = {
    http_req_failed: [`rate<${errorRate}`],
    checks: [`rate>${checks}`],
  };
  for (const [name, slo] of Object.entries(SLO)) {
    if (only && !only.includes(name)) {
      continue;
    }
    const extra = extraLatency[name] || 0;
    const rules = [`p(95)<${slo.p95 + extra}`];
    if (slo.p99) {
      rules.push(`p(99)<${slo.p99 + extra}`);
    }
    thresholds[`http_req_duration{name:${name}}`] = rules;
  }
  return thresholds;
}
