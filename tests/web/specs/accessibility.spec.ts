import AxeBuilder from '@axe-core/playwright';
import { uniqueClient } from '../support/api';
import { expect, test } from '../support/fixtures';
import { knownDefect } from '../support/known-defect';

/** docs/test-cases/web-e2e.md — G. Acessibilidade (NF-04). */
test.describe('Acessibilidade', () => {
  test(
    'CT-WEB-070 — varredura WCAG 2.1 A/AA sem violações críticas ou sérias',
    knownDefect('17', 'H-16', '@P3'),
    async ({ catalog, orderForm, orderList, orderDetail, dashboard, ordersApi, page }, testInfo) => {
      const order = await ordersApi.create([['PROD-001', 1]]);
      const screens: Array<{ name: string; open: () => Promise<void> }> = [
        {
          name: 'Painel',
          open: async () => {
            await dashboard.goto();
            await expect(dashboard.indicator('dashboard.ordersTotal')).toBeVisible();
          },
        },
        {
          name: 'Catálogo com resultados',
          open: async () => {
            await catalog.goto();
            await catalog.search('PROD-001, PROD-005');
            await expect(catalog.rows()).toHaveCount(2);
          },
        },
        {
          name: 'Novo pedido com item',
          open: async () => {
            await orderForm.goto();
            await orderForm.addProduct('PROD-002', 1);
          },
        },
        {
          name: 'Pedidos',
          open: async () => {
            await orderList.goto();
            await expect(orderList.rows().first()).toBeVisible();
          },
        },
        {
          name: 'Detalhe do pedido',
          open: async () => {
            await orderDetail.goto(order.id);
            await expect(orderDetail.itemsTable()).toBeVisible();
          },
        },
      ];

      const blocking: string[] = [];
      for (const screen of screens) {
        await screen.open();
        const results = await new AxeBuilder({ page })
          .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
          .analyze();
        await testInfo.attach(`axe — ${screen.name}`, {
          body: JSON.stringify(results.violations, null, 2),
          contentType: 'application/json',
        });
        for (const violation of results.violations.filter(
          (v) => v.impact === 'critical' || v.impact === 'serious',
        )) {
          blocking.push(
            `${screen.name}: [${violation.impact}] ${violation.id} — ${violation.help} (${violation.nodes.length} elemento(s))`,
          );
        }
      }

      expect(blocking, 'violações críticas/sérias').toEqual([]);
    },
  );

  test(
    'CT-WEB-071 — criação de pedido operada só por teclado',
    { tag: ['@P3'] },
    async ({ orderForm, page }) => {
      const client = uniqueClient();
      await orderForm.goto();

      await orderForm.nameInput().focus();
      await page.keyboard.type(client.name);
      await page.keyboard.press('Tab');
      await page.keyboard.type(client.document);
      await page.keyboard.press('Tab');
      await page.keyboard.type(client.email);
      await page.keyboard.press('Tab');
      await expect(orderForm.productInput()).toBeFocused();
      expect(await page.evaluate(() => document.activeElement?.matches(':focus-visible'))).toBe(true);
      await page.keyboard.type('PROD-002');
      await page.keyboard.press('Enter');
      await expect(orderForm.itemRows()).toHaveCount(1);

      await orderForm.submitButton().focus();
      await page.keyboard.press('Enter');

      await expect(page).toHaveURL(/\/orders\/[0-9a-f-]{36}$/);
    },
  );
});
