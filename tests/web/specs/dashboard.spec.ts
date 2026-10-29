import { errorEnvelope, expect, test } from '../support/fixtures';
import { expectCurrency } from '../support/assertions';
import { sumAmounts } from '../support/format';

/** docs/test-cases/web-e2e.md — B. Painel (Dashboard). */
test.describe('Painel', () => {
  test('CT-WEB-010 — indicadores coerentes com a API', { tag: ['@P1'] }, async ({ dashboard, ordersApi }) => {
    await ordersApi.create([['PROD-002', 1]]);
    await dashboard.goto();
    await expect(dashboard.indicator('dashboard.ordersTotal')).toBeVisible();

    // Oráculo: a mesma consulta que a tela faz (página 0, até 100 pedidos), no mesmo instante.
    const oracle = await ordersApi.list(0, 100);
    const loaded = oracle.content.length;
    const created = oracle.content.filter((order) => order.status === 'CRIADO').length;

    await expect(dashboard.indicator('dashboard.ordersTotal')).toHaveText(String(oracle.totalElements));
    await expectCurrency(
      dashboard.indicator('dashboard.valueLoaded'),
      sumAmounts(oracle.content.map((order) => order.total)),
    );
    await expect(dashboard.statusCard()).toContainText(`${dashboard.t('dashboard.ordersLoaded')}: ${loaded}`);
    await expect(dashboard.statusCount(dashboard.t('status.CRIADO'))).toHaveText(String(created));
  });

  test('CT-WEB-011 — atalhos do painel', { tag: ['@P2'] }, async ({ dashboard, page }) => {
    const shortcuts = [
      { key: 'dashboard.newOrder', path: '/orders/new' },
      { key: 'dashboard.viewOrders', path: '/orders' },
      { key: 'dashboard.viewCatalog', path: '/catalog' },
    ] as const;

    for (const { key, path } of shortcuts) {
      await dashboard.goto();
      await dashboard.shortcut(key).click();
      await expect(page).toHaveURL(path);
    }
  });

  test('CT-WEB-012 — aviso de indicadores indisponíveis', { tag: ['@P3'] }, async ({ dashboard }) => {
    await dashboard.goto();

    await expect(dashboard.gapNotice()).toContainText(dashboard.t('dashboard.gapMessage'));
  });

  test('CT-WEB-013 — painel sem pedidos', { tag: ['@P3'] }, async ({ dashboard, page }) => {
    await page.route('**/api/orders?*', (route) =>
      route.fulfill({ json: { content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 } }),
    );

    await dashboard.goto();

    await expect(page.getByText(dashboard.t('dashboard.empty'), { exact: true })).toBeVisible();
    await expect(dashboard.shortcut('dashboard.newOrder')).toBeVisible();
  });

  test(
    'CT-WEB-014 — erro de API no painel e nova tentativa',
    { tag: ['@P2'] },
    async ({ dashboard, page }) => {
      // Código desconhecido força o fallback por status HTTP (código → status → genérica, RN-34).
      await page.route('**/api/orders?*', (route) =>
        route.fulfill({
          status: 503,
          contentType: 'application/json',
          body: errorEnvelope('SIMULATED_UNAVAILABLE', 'web-mock-503'),
        }),
      );
      await dashboard.goto();

      const error = dashboard.errorState(dashboard.t('errors.status.503'));
      await expect(error).toContainText(dashboard.t('errors.requestId', { id: 'web-mock-503' }));

      await page.unrouteAll();
      await dashboard.retryButton().click();
      await expect(dashboard.indicator('dashboard.ordersTotal')).toBeVisible();
    },
  );
});
