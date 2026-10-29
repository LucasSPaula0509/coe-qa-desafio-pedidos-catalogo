import { uniqueClient } from '../support/api';
import { expect, test } from '../support/fixtures';
import { knownDefect } from '../support/known-defect';

/** docs/test-cases/security.md — segurança da interface (OWASP Top 10 2021: A03, A05). */
test.describe('Segurança — interface', () => {
  test(
    'CT-SEC-004 — XSS armazenado nos dados do cliente é exibido como texto',
    { tag: ['@P1', '@security'] },
    async ({ orderList, orderDetail, ordersApi, page }) => {
      const base = uniqueClient();
      const payload = `<img src=x onerror="window.__xss=1"><script>window.__xss=2</script> ${base.name}`;
      const order = await ordersApi.create([['PROD-002', 1]], { ...base, name: payload });
      const dialogs: string[] = [];
      page.on('dialog', (dialog) => {
        dialogs.push(dialog.message());
        void dialog.dismiss();
      });

      for (const open of [() => orderList.goto(), () => orderDetail.goto(order.id)]) {
        await open();
        await expect(page.getByText(payload, { exact: true }).first()).toBeVisible();
        expect(await page.locator('img[src="x"]').count(), 'elemento <img> injetado no DOM').toBe(0);
        expect(
          await page.evaluate(() => (window as unknown as { __xss?: number }).__xss),
          'script executado',
        ).toBe(undefined);
      }
      expect(dialogs, 'nenhum diálogo disparado').toEqual([]);
    },
  );

  test(
    'CT-SEC-008 — front-end envia os cabeçalhos de segurança',
    knownDefect('26', 'H-08', '@P2', '@security'),
    async ({ request }) => {
      const response = await request.get('/');
      const headers = response.headers();
      const csp = headers['content-security-policy'] ?? '';

      const missing = [
        ['Content-Security-Policy', csp !== ''],
        ['X-Content-Type-Options: nosniff', headers['x-content-type-options'] === 'nosniff'],
        [
          'X-Frame-Options ou CSP frame-ancestors',
          !!headers['x-frame-options'] || csp.includes('frame-ancestors'),
        ],
        ['Referrer-Policy', !!headers['referrer-policy']],
      ]
        .filter(([, present]) => !present)
        .map(([name]) => name);

      expect(missing, 'cabeçalhos de segurança ausentes').toEqual([]);
    },
  );
});
