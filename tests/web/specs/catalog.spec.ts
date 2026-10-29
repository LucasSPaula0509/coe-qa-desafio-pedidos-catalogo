import { captureRequests, expect, isCatalogCall, test } from '../support/fixtures';
import { expectCurrency } from '../support/assertions';

/** docs/test-cases/web-e2e.md — C. Catálogo. Oráculo: massa inicial (RN-22) e traduções do SUT. */
test.describe('Catálogo', () => {
  test.beforeEach(async ({ catalog }) => {
    await catalog.goto();
  });

  test(
    'CT-WEB-020 — consulta individual exibe os dados do produto',
    { tag: ['@P1'] },
    async ({ catalog }) => {
      await catalog.search('PROD-001');

      const cells = catalog.cells('PROD-001');
      await expect(catalog.rows()).toHaveCount(1);
      await expect(cells.nth(1)).toHaveText('Notebook Dell Inspiron 15');
      await expectCurrency(cells.nth(2), 3500);
      await expect(cells.nth(3)).toHaveText('BRL');
      await expect(cells.nth(4)).toHaveText(catalog.t('status.ATIVO'));
    },
  );

  test(
    'CT-WEB-021 — lote com situações diferentes (inativo, sem preço)',
    { tag: ['@P1'] },
    async ({ catalog }) => {
      await catalog.search('PROD-001, PROD-004, PROD-005');

      await expect(catalog.rows()).toHaveCount(3);
      await expect(catalog.cells('PROD-004').nth(4)).toHaveText(catalog.t('status.INATIVO'));
      await expect(catalog.cells('PROD-005').nth(2)).toHaveText('—');
      await expect(catalog.cells('PROD-005').nth(3)).toHaveText('—');
      await expect(catalog.cells('PROD-001').nth(4)).toHaveText(catalog.t('status.ATIVO'));
    },
  );

  test(
    'CT-WEB-022 — lote com ID inexistente sinaliza os não encontrados',
    { tag: ['@P1'] },
    async ({ catalog }) => {
      await catalog.search('PROD-001, PROD-999');

      await expect(catalog.rows()).toHaveCount(1);
      await expect(catalog.cells('PROD-001').first()).toBeVisible();
      await expect(catalog.notFoundAlert('PROD-999')).toBeVisible();
    },
  );

  test('CT-WEB-023 — consulta individual de ID inexistente', { tag: ['@P2'] }, async ({ catalog, page }) => {
    const response = page.waitForResponse((r) => r.url().endsWith('/api/catalog/PROD-999'));
    await catalog.search('PROD-999');
    const requestId = (await (await response).json()).requestId as string;

    await expect(catalog.errorState(catalog.t('errors.codes.PRODUCT_NOT_FOUND'))).toContainText(
      catalog.t('errors.requestId', { id: requestId }),
    );
  });

  test(
    'CT-WEB-024 — campo obrigatório, sem requisição ao catálogo',
    { tag: ['@P2'] },
    async ({ catalog, page }) => {
      const calls = captureRequests(page, isCatalogCall);

      await catalog.searchButton().click();

      await expect(catalog.requiredMessage()).toBeVisible();
      expect(calls).toHaveLength(0);
    },
  );

  test('CT-WEB-025 — separadores variados e IDs repetidos', { tag: ['@P3'] }, async ({ catalog, page }) => {
    const request = page.waitForRequest((r) => isCatalogCall(r) && r.method() === 'POST');

    await catalog.search('PROD-001;PROD-002  PROD-001');

    expect((await request).postDataJSON()).toEqual({ productIds: ['PROD-001', 'PROD-002'] });
    await expect(catalog.rows()).toHaveCount(2);
  });

  test('CT-WEB-026 — limite de 50 IDs na interface', { tag: ['@P2'] }, async ({ catalog, page }) => {
    const ids = Array.from({ length: 51 }, (_, i) => `QA-NX-${String(i + 1).padStart(3, '0')}`);
    const calls = captureRequests(page, isCatalogCall);

    await catalog.search(ids.join(', '));
    await catalog.expectToast(catalog.t('catalog.maxIds'));
    expect(calls).toHaveLength(0);

    await catalog.search(ids.slice(0, 50).join(', '));
    await expect(catalog.notFoundAlert(ids.slice(0, 50).join(', '))).toBeVisible();
    expect(calls).toHaveLength(1);
  });
});
