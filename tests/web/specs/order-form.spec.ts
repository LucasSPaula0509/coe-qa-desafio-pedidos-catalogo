import { uniqueClient } from '../support/api';
import { captureRequests, errorEnvelope, expect, isOrderCreation, test } from '../support/fixtures';
import { knownDefect } from '../support/known-defect';
import { expectCurrency } from '../support/assertions';

/** docs/test-cases/web-e2e.md — D. Novo pedido. Valores esperados vêm da massa (RN-22) e da API. */
test.describe('Novo pedido', () => {
  test.beforeEach(async ({ orderForm }) => {
    await orderForm.goto();
  });

  test(
    'CT-WEB-030 — criação de pedido com desconto (fluxo principal)',
    { tag: ['@P1'] },
    async ({ orderForm, orderDetail, ordersApi, page }) => {
      await orderForm.fillClient(uniqueClient());
      await orderForm.addProduct('PROD-001', 2);
      await orderForm.addProduct('PROD-002', 1);

      const notebook = orderForm.itemCells('PROD-001');
      const mouse = orderForm.itemCells('PROD-002');
      await expectCurrency(notebook.nth(2), 3500);
      await expectCurrency(notebook.nth(3), 7000);
      await expectCurrency(mouse.nth(2), 499.9);
      await expectCurrency(mouse.nth(3), 499.9);
      await expectCurrency(orderForm.subtotal(), 7499.9);

      await orderForm.submitButton().click();

      await orderForm.expectToast(orderForm.t('orders.form.createSuccess'));
      await expect(page).toHaveURL(/\/orders\/[0-9a-f-]{36}$/);
      const order = await ordersApi.get(page.url().split('/').pop()!);
      await expectCurrency(orderDetail.value('orders.list.discount'), 749.99);
      await expectCurrency(orderDetail.value('orders.list.total'), 6749.91);
      expect([order.subtotal, order.discount, order.total]).toEqual([7499.9, 749.99, 6749.91]);
    },
  );

  test(
    'CT-WEB-031 — criação abaixo do mínimo de desconto',
    { tag: ['@P1'] },
    async ({ orderForm, orderDetail, page }) => {
      await orderForm.fillClient(uniqueClient());
      await orderForm.addProduct('PROD-002', 1);

      await orderForm.submitButton().click();

      await expect(page).toHaveURL(/\/orders\/[0-9a-f-]{36}$/);
      await expectCurrency(orderDetail.value('orders.list.discount'), 0);
      await expectCurrency(orderDetail.value('orders.list.total'), 499.9);
    },
  );

  test('CT-WEB-032 — campos do cliente obrigatórios', { tag: ['@P1'] }, async ({ orderForm, page }) => {
    const creations = captureRequests(page, isOrderCreation);
    await orderForm.addProduct('PROD-001', 1);

    await orderForm.submitButton().click();

    await expect(orderForm.fieldError('form.required')).toHaveCount(3);
    await orderForm.expectToast(orderForm.t('orders.form.invalidForm'));
    expect(creations).toHaveLength(0);
  });

  test('CT-WEB-033 — e-mail inválido', { tag: ['@P2'] }, async ({ orderForm, page }) => {
    const creations = captureRequests(page, isOrderCreation);
    await orderForm.fillClient({ ...uniqueClient(), email: 'maria@' });
    await orderForm.addProduct('PROD-001', 1);

    await orderForm.submitButton().click();

    await expect(orderForm.fieldError('form.email')).toBeVisible();
    expect(creations).toHaveLength(0);
  });

  const rejectedProducts = [
    {
      id: 'CT-WEB-034',
      product: 'PROD-999',
      key: 'orders.form.productNotFound',
      label: 'inexistente',
      tag: '@P1',
    },
    {
      id: 'CT-WEB-035',
      product: 'PROD-004',
      key: 'orders.form.productInactive',
      label: 'inativo',
      tag: '@P1',
    },
  ];
  for (const { id, product, key, label, tag } of rejectedProducts) {
    test(`${id} — adicionar produto ${label} é recusado com aviso`, { tag: [tag] }, async ({ orderForm }) => {
      await orderForm.addProduct(product, 1);

      await orderForm.expectToast(orderForm.t(key, { id: product }));
      await expect(orderForm.noItemsMessage()).toBeVisible();
    });
  }

  test(
    'CT-WEB-036 — adicionar produto sem preço vigente é recusado',
    { tag: ['@P1'] },
    async ({ orderForm }) => {
      for (const product of ['PROD-005', 'PROD-006']) {
        await orderForm.addProduct(product, 1);
        await orderForm.expectToast(orderForm.t('orders.form.productNoPrice', { id: product }));
      }

      await expect(orderForm.noItemsMessage()).toBeVisible();
    },
  );

  test('CT-WEB-037 — adicionar produto já incluído', { tag: ['@P2'] }, async ({ orderForm }) => {
    await orderForm.addProduct('PROD-001', 1);
    await orderForm.addProduct('PROD-001', 1);

    await orderForm.expectToast(orderForm.t('orders.form.productDuplicated', { id: 'PROD-001' }));
    await expect(orderForm.itemRows()).toHaveCount(1);
  });

  test('CT-WEB-038 — limites de quantidade na interface', { tag: ['@P2'] }, async ({ orderForm, page }) => {
    const creations = captureRequests(page, isOrderCreation);
    for (const invalid of [0, 1000]) {
      await orderForm.productInput().fill('PROD-001');
      await orderForm.quantityInput().fill(String(invalid));
      await orderForm.addButton().click();
      await expect(orderForm.fieldError('orders.form.invalidQuantity')).toBeVisible();
    }
    await expect(orderForm.noItemsMessage()).toBeVisible();

    await orderForm.addProduct('PROD-001', 1);
    await orderForm.rowQuantity('Notebook Dell Inspiron 15').fill('1000');
    await orderForm.rowQuantity('Notebook Dell Inspiron 15').press('Tab');
    await expect(orderForm.fieldError('orders.form.invalidQuantity')).toBeVisible();
    await orderForm.fillClient(uniqueClient());
    await orderForm.submitButton().click();

    await orderForm.expectToast(orderForm.t('orders.form.invalidForm'));
    expect(creations).toHaveLength(0);
  });

  test(
    'CT-WEB-039 — edição de quantidade recalcula os valores exibidos',
    { tag: ['@P2'] },
    async ({ orderForm }) => {
      await orderForm.addProduct('PROD-002', 1);

      await orderForm.rowQuantity('Mouse Logitech MX Master 3S').fill('3');
      await orderForm.rowQuantity('Mouse Logitech MX Master 3S').press('Tab');

      await expectCurrency(orderForm.itemCells('PROD-002').nth(3), 1499.7);
      await expectCurrency(orderForm.subtotal(), 1499.7);
    },
  );

  test(
    'CT-WEB-040 — remoção de itens atualiza tabela e subtotal',
    { tag: ['@P2'] },
    async ({ orderForm }) => {
      await orderForm.addProduct('PROD-001', 1);
      await orderForm.addProduct('PROD-002', 1);

      await orderForm.removeButton('Notebook Dell Inspiron 15').click();
      await expect(orderForm.itemRows()).toHaveCount(1);
      await expectCurrency(orderForm.subtotal(), 499.9);

      await orderForm.removeButton('Mouse Logitech MX Master 3S').click();
      await expect(orderForm.noItemsMessage()).toBeVisible();
      await expectCurrency(orderForm.subtotal(), 0);
    },
  );

  test('CT-WEB-041 — criar sem itens é bloqueado', { tag: ['@P2'] }, async ({ orderForm, page }) => {
    const creations = captureRequests(page, isOrderCreation);
    await orderForm.fillClient(uniqueClient());

    await orderForm.submitButton().click();

    await orderForm.expectToast(orderForm.t('orders.form.invalidForm'));
    expect(creations).toHaveLength(0);
  });

  const apiFailures = [
    { id: 'CT-WEB-042', status: 422, code: 'PRODUCT_INACTIVE', label: 'regra de negócio (422)' },
    { id: 'CT-WEB-043', status: 503, code: 'CATALOG_UNAVAILABLE', label: 'catálogo indisponível (503)' },
  ];
  for (const { id, status, code, label } of apiFailures) {
    test(
      `${id} — erro de ${label} na criação preserva o formulário`,
      { tag: ['@P2'] },
      async ({ orderForm, page }) => {
        const requestId = `web-mock-${status}`;
        await page.route('**/api/orders', (route) =>
          route.request().method() === 'POST'
            ? route.fulfill({ status, contentType: 'application/json', body: errorEnvelope(code, requestId) })
            : route.continue(),
        );
        const client = uniqueClient();
        await orderForm.fillClient(client);
        await orderForm.addProduct('PROD-002', 1);

        await orderForm.submitButton().click();

        await orderForm.expectToast(orderForm.t(`errors.codes.${code}`), requestId);
        await expect(page).toHaveURL('/orders/new');
        await expect(orderForm.nameInput()).toHaveValue(client.name);
        await expect(orderForm.itemRows()).toHaveCount(1);
        await expect(orderForm.submitButton()).toBeEnabled();
      },
    );
  }

  test(
    'CT-WEB-044 — cancelar com e sem dados',
    knownDefect('15', 'H-14', '@P3'),
    async ({ orderForm, page }) => {
      const dialog = page.getByRole('dialog');

      await test.step('(b) com dados, fechar o diálogo mantém os dados', async () => {
        await orderForm.nameInput().fill('Cliente em edição');
        await orderForm.cancelButton().click();
        await dialog.getByRole('button', { name: orderForm.t('common.cancel'), exact: true }).click();
        await expect(dialog).toBeHidden();
        await expect(page).toHaveURL('/orders/new');
        await expect(orderForm.nameInput()).toHaveValue('Cliente em edição');
      });

      await test.step('(a) com dados, "Descartar e sair" vai para a listagem', async () => {
        await orderForm.cancelButton().click();
        await expect(dialog).toContainText(orderForm.t('orders.form.discardMessage'));
        await dialog.getByRole('button', { name: orderForm.t('orders.form.discardConfirm') }).click();
        await expect(page).toHaveURL('/orders');
      });

      await test.step('(c) sem dados, sai direto, sem diálogo', async () => {
        await orderForm.goto();
        await orderForm.cancelButton().click();
        await expect(page).toHaveURL('/orders');
        await expect(dialog).toBeHidden();
      });
    },
  );

  test(
    'CT-WEB-045 — duplo clique em "Criar pedido" cria um único pedido',
    { tag: ['@P2'] },
    async ({ orderForm, page }) => {
      const creations = captureRequests(page, isOrderCreation);
      await orderForm.fillClient(uniqueClient());
      await orderForm.addProduct('PROD-002', 1);

      await orderForm.submitButton().dblclick();

      await expect(page).toHaveURL(/\/orders\/[0-9a-f-]{36}$/);
      expect(creations).toHaveLength(1);
    },
  );
});
