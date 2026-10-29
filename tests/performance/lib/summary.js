// Saída padronizada de cada cenário: resumo em texto no console e arquivos JSON/texto em /results
// (artefatos do CI). O resumo JSON é a evidência para o registro de execução.

import { textSummary } from 'https://jslib.k6.io/k6-summary/0.1.0/index.js';

/**
 * @param {string} scenario nome do cenário (ex.: "smoke")
 * @param {string} caseId caso de teste (ex.: "CT-PRF-001")
 * @param {(data: object) => string} [extra] linhas adicionais (ex.: comparação entre grupos)
 */
export function summaryHandler(scenario, caseId, extra) {
  return (data) => {
    const header = `\n=== ${caseId} — ${scenario} ===\n`;
    const additional = extra ? `\n${extra(data)}\n` : '';
    const text = header + textSummary(data, { indent: ' ', enableColors: false }) + additional;
    return {
      stdout: header + textSummary(data, { indent: ' ', enableColors: true }) + additional,
      [`/results/${scenario}-summary.json`]: JSON.stringify(data, null, 2),
      [`/results/${scenario}-summary.txt`]: text,
    };
  };
}

/** p95 (ms) de uma métrica de duração filtrada por tag, ou null se ausente. */
export function p95(data, metric) {
  const values = data.metrics[metric] && data.metrics[metric].values;
  return values ? values['p(95)'] : null;
}
