import { randomUUID } from 'node:crypto';
import { expectCurrency } from '../support/assertions';
import { expect, test } from '../support/fixtures';
import { knownDefect } from '../support/known-defect';
import { expectedShortDateNumbers, numbersIn } from '../support/format';

/** docs/test-cases/web-e2e.md — E. Pedidos (listagem e detalhe). Oráculo: a API, no mesmo instante. */
test.describe('Pedidos', () => {
  test(
    'CT-WEB-050 — pedido recém-criado no topo da lista',
    { tag: ['@P1'] },
    async ({ orderList, ordersApi, page }) => {
      const order = await ordersApi.create([
        ['PROD-001', 1],
        ['PROD-002', 2],
      ]);

      await orderList.goto();

      const cells = orderList.rows().first().getByRole('cell');
      await expect(cells.nth(0)).toHaveText(`${order.id.slice(0, 13)}…`);
      await expect(cells.nth(1)).toHaveText(order.client.name);
      await expect(cells.nth(2)).toHaveText(String(order.items.length));
      await expectCurrency(cells.nth(3), order.subtotal);
      await expectCurrency(cells.nth(4), order.discount);
      await expectCurrency(cells.nth(5), order.total);
      await expect(cells.nth(6)).toHaveText(orderList.t('status.CRIADO'));
      expect(numbersIn(await cells.nth(7).innerText())).toEqual(
        await expectedShortDateNumbers(page, order.createdAt, 'pt-BR'),
      );
      await cells.nth(0).locator('span').hover();
      await expect(page.getByRole('tooltip')).toHaveText(order.id);
    },
  );

  test('CT-WEB-051 — paginação da listagem', { tag: ['@P2'] }, async ({ orderList, ordersApi }) => {
    await ordersApi.ensureAtLeast(21);
    await orderList.goto();
    await expect(orderList.pagination()).toBeVisible();

    // Controle "próxima": navegação válida em qualquer volume (o alcance dos links numéricos com
    // muitas páginas é verificado no CT-WEB-058).
    await orderList.nextPageLink().click();

    const secondPage = await ordersApi.list(1, 20);
    await expect.poll(() => orderList.displayedIds()).toEqual(secondPage.content.map((order) => order.id));
  });

  test(
    'CT-WEB-058 — com volume, todos os links de página ficam ao alcance',
    knownDefect('16', 'H-15', '@P2'),
    async ({ orderList, ordersApi }) => {
      test.setTimeout(240_000);
      await ordersApi.ensureAtLeast(1000);
      const { totalPages } = await ordersApi.list(0, 20);
      await orderList.goto();
      await expect(orderList.pagination()).toBeVisible();

      for (const pageNumber of ['1', '2', String(totalPages)]) {
        const link = orderList.pagination().getByText(pageNumber, { exact: true });
        await link.scrollIntoViewIfNeeded();
        await expect(link, `link da página ${pageNumber} de ${totalPages} visível na tela`).toBeInViewport();
      }
      await orderList.pagination().getByRole('link', { name: '2', exact: true }).click();
      const secondPage = await ordersApi.list(1, 20);
      await expect.poll(() => orderList.displayedIds()).toEqual(secondPage.content.map((order) => order.id));
    },
  );

  test(
    'CT-WEB-052 — detalhe a partir da lista',
    { tag: ['@P1'] },
    async ({ orderList, orderDetail, ordersApi, page }) => {
      const order = await ordersApi.create([
        ['PROD-001', 1],
        ['PROD-003', 2],
      ]);
      await orderList.goto();

      await orderList.viewLink(order.id).click();

      await expect(page).toHaveURL(`/orders/${order.id}`);
      await expect(orderDetail.value('orders.detail.name')).toHaveText(order.client.name);
      await expect(orderDetail.value('orders.detail.document')).toHaveText(order.client.document);
      await expect(orderDetail.value('orders.detail.email')).toHaveText(order.client.email);
      await expect(orderDetail.value('orders.list.id')).toHaveText(order.id);
      await expect(orderDetail.value('orders.list.status')).toHaveText(orderDetail.t('status.CRIADO'));
      await expectCurrency(orderDetail.value('orders.list.subtotal'), order.subtotal);
      await expectCurrency(orderDetail.value('orders.list.discount'), order.discount);
      await expectCurrency(orderDetail.value('orders.list.total'), order.total);
      for (const item of order.items) {
        const cells = orderDetail.itemCells(item.productId);
        await expect(cells.nth(0)).toContainText(item.productName);
        await expect(cells.nth(1)).toHaveText(String(item.quantity));
        await expectCurrency(cells.nth(2), item.unitPrice);
        await expectCurrency(cells.nth(3), item.lineTotal);
      }
    },
  );

  test('CT-WEB-053 — detalhe de pedido inexistente', { tag: ['@P2'] }, async ({ orderDetail, page }) => {
    const id = randomUUID();
    const response = page.waitForResponse((r) => r.url().endsWith(`/api/orders/${id}`));

    await page.goto(`/orders/${id}`);
    const requestId = (await (await response).json()).requestId as string;

    await expect(orderDetail.errorState(orderDetail.t('errors.codes.ORDER_NOT_FOUND'))).toContainText(
      orderDetail.t('errors.requestId', { id: requestId }),
    );
  });

  test('CT-WEB-054 — lista vazia com ação de novo pedido', { tag: ['@P3'] }, async ({ orderList, page }) => {
    await page.route('**/api/orders?*', (route) =>
      route.fulfill({ json: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 } }),
    );
    await orderList.goto();

    await expect(page.getByText(orderList.t('orders.empty'), { exact: true })).toBeVisible();
    await page.getByRole('button', { name: orderList.t('orders.new'), exact: true }).click();

    await expect(page).toHaveURL('/orders/new');
  });

  test(
    'CT-WEB-055 — voltar do detalhe para a lista',
    { tag: ['@P3'] },
    async ({ orderDetail, ordersApi, page }) => {
      const order = await ordersApi.create([['PROD-002', 1]]);
      await orderDetail.goto(order.id);

      await orderDetail.backLink().click();

      await expect(page).toHaveURL('/orders');
    },
  );
});
