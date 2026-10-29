import { chromium } from '@playwright/test';
import { expect, test } from '../support/fixtures';
import { t } from '../support/i18n';
import { knownDefect } from '../support/known-defect';

const WEB_BASE_URL = new URL(process.env.WEB_BASE_URL ?? 'http://localhost:4200');
const QA_HOST = 'qa-pedidos.test';

/**
 * docs/test-cases/web-e2e.md — A. Shell: a aplicação servida por HTTP num nome de host comum (fora
 * de localhost), como em homologação ou intranet. Um navegador próprio mapeia QA_HOST para o
 * front-end (--host-resolver-rules): a origem deixa de ser "localhost" e o contexto passa a ser
 * inseguro, sem alterar o SUT nem a rede.
 */
test.describe('Implantação', () => {
  test(
    'CT-WEB-057 — aplicação operante quando acessada por nome de host via HTTP',
    knownDefect('20', 'H-18', '@P2'),
    async () => {
      const target = WEB_BASE_URL.hostname === 'localhost' ? '127.0.0.1' : WEB_BASE_URL.hostname;
      const browser = await chromium.launch({ args: [`--host-resolver-rules=MAP ${QA_HOST} ${target}`] });
      try {
        const page = await browser.newPage();
        await page.goto(`http://${QA_HOST}:${WEB_BASE_URL.port || 80}/`);

        expect(
          await page.evaluate(() => window.isSecureContext),
          'pré-condição: origem não-local via HTTP',
        ).toBe(false);
        await expect(page.getByRole('heading', { level: 1 })).toHaveText(t('dashboard.title'));
        await expect(page.getByRole('navigation', { name: t('nav.menu') })).toBeVisible();
      } finally {
        await browser.close();
      }
    },
  );
});
