import type { TestDetails } from '@playwright/test';

const ISSUES_URL = 'https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/issues';

/**
 * Marca um teste que reproduz um defeito confirmado (specs/open-questions.md). Política da
 * estratégia (§12): o teste continua executando e falhando; nunca é desativado. A tag
 * @defeito-conhecido permite ao gate avaliar a falha pela severidade, e a anotação liga o relatório
 * à Issue. Uso: test('CT-…', knownDefect('14', 'H-14', '@P3'), async () => …).
 */
export function knownDefect(issue: string, suspicion: string, ...tags: string[]): TestDetails {
  return {
    tag: [...tags, '@defeito-conhecido'],
    annotation: [
      { type: 'issue', description: `${ISSUES_URL}/${issue}` },
      { type: 'suspeita', description: suspicion },
    ],
  };
}
