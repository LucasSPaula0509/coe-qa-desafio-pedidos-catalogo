// CT-PRF-005 — Resistência: 30 VUs por DURATION (60 min). Sem tendência de alta no p95
// (> 20% entre o primeiro e o último quarto), erro < 1%, sem erros de pool.
// As janelas "first" e "last" são cenários sequenciais com tag própria; a comparação vai no resumo.

import { mixedIteration } from '../lib/operations.js';
import { sloThresholds, SUMMARY_TREND_STATS } from '../lib/config.js';
import { p95, summaryHandler } from '../lib/summary.js';

const totalMinutes = Number((__ENV.DURATION || '60m').replace('m', ''));
const quarter = Math.max(1, Math.floor(totalMinutes / 4));
const middle = Math.max(1, totalMinutes - 2 * quarter);

export const options = {
  summaryTrendStats: SUMMARY_TREND_STATS,
  scenarios: {
    first: { executor: 'constant-vus', vus: 30, duration: `${quarter}m`, tags: { window: 'first' } },
    middle: { executor: 'constant-vus', vus: 30, duration: `${middle}m`, startTime: `${quarter}m` },
    last: {
      executor: 'constant-vus',
      vus: 30,
      duration: `${quarter}m`,
      startTime: `${quarter + middle}m`,
      tags: { window: 'last' },
    },
  },
  thresholds: {
    ...sloThresholds(),
    // Expõem as submétricas por janela no resumo (base da comparação de tendência).
    'http_req_duration{window:first}': ['p(95)>=0'],
    'http_req_duration{window:last}': ['p(95)>=0'],
  },
};

export default function () {
  mixedIteration();
}

export const handleSummary = summaryHandler('soak', 'CT-PRF-005', (data) => {
  const first = p95(data, 'http_req_duration{window:first}');
  const last = p95(data, 'http_req_duration{window:last}');
  const growth = first ? ((last - first) / first) * 100 : NaN;
  const verdict = growth <= 20 ? 'OK (sem degradação progressiva)' : 'ATENÇÃO: degradação > 20%';
  return `Tendência do p95: primeiro quarto ${first?.toFixed(1)} ms → último quarto ${last?.toFixed(1)} ms (${growth.toFixed(1)}%) — ${verdict}`;
});
