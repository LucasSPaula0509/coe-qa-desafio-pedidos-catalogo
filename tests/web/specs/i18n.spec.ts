import type { Page, TestDetails } from '@playwright/test';
import { uniqueClient } from '../support/api';
import { expectCurrency } from '../support/assertions';
import { expect, test } from '../support/fixtures';
import { knownDefect } from '../support/known-defect';
import { expectedShortDateNumbers, numbersIn } from '../support/format';
import { flatten, LANGS, t, type Lang } from '../support/i18n';
import type { AppShell } from '../pages/shell';

/** docs/test-cases/web-e2e.md — F. Idiomas. Oráculo: arquivos de tradução do SUT e Intl. */

/** Abre a aplicação já no idioma (preferência gravada antes do carregamento, como a do usuário). */
async function useLanguage(page: Page, lang: Lang, ...pages: AppShell[]): Promise<void> {
  await page.addInitScript((value) => localStorage.setItem('app-language', value), lang);
  pages.forEach((p) => (p.lang = lang));
}

test.describe('Idiomas', () => {
  test(
    'CT-WEB-060 — troca de idioma em tempo de execução, sem recarregar',
    { tag: ['@P1'] },
    async ({ dashboard, page }) => {
      await dashboard.goto();
      await expect(dashboard.heading()).toHaveText(t('dashboard.title'));
      let loads = 0;
      page.on('load', () => loads++);
      await page.evaluate(() => ((window as unknown as { qaMarker: boolean }).qaMarker = true));

      for (const lang of ['en', 'es'] as const) {
        await dashboard.switchLanguage(lang);
        await expect(dashboard.heading()).toHaveText(t('dashboard.title', lang));
        await expect(dashboard.navLink('nav.orders')).toBeVisible();
        await expect(dashboard.navLink('nav.catalog')).toBeVisible();
        await expect(dashboard.shortcut('dashboard.newOrder')).toBeVisible();
      }

      expect(loads, 'nenhum recarregamento de página').toBe(0);
      expect(await page.evaluate(() => (window as unknown as { qaMarker?: boolean }).qaMarker)).toBe(true);
    },
  );

  /** H-17: só o espanhol diverge (agrupamento de valores com 4 dígitos). */
  const formattingDetails: Record<Lang, TestDetails> = {
    'pt-BR': { tag: ['@P2'] },
    en: { tag: ['@P2'] },
    es: knownDefect('18', 'H-17', '@P2'),
  };
  for (const lang of LANGS) {
    test(
      `CT-WEB-061 — moeda e data formatadas no idioma: ${lang}`,
      formattingDetails[lang],
      async ({ orderDetail, orderList, ordersApi, page }) => {
        const order = await ordersApi.create([['PROD-001', 1]]);
        await useLanguage(page, lang, orderDetail, orderList);

        await orderDetail.goto(order.id);
        await expectCurrency(orderDetail.value('orders.list.subtotal'), order.subtotal, lang);
        await expectCurrency(orderDetail.value('orders.list.discount'), order.discount, lang);
        await expectCurrency(orderDetail.value('orders.list.total'), order.total, lang);

        await orderList.goto();
        const dateCell = orderList.rows().first().getByRole('cell').nth(7);
        expect(numbersIn(await dateCell.innerText())).toEqual(
          await expectedShortDateNumbers(page, order.createdAt, lang),
        );
      },
    );
  }

  test(
    'CT-WEB-062 — preferência de idioma persistida após recarregar',
    { tag: ['@P2'] },
    async ({ dashboard, page }) => {
      await dashboard.goto();
      await dashboard.switchLanguage('es');

      await page.reload();

      await expect(dashboard.languageSelector()).toHaveText(t('language.es', 'es'));
      await expect(dashboard.heading()).toHaveText(t('dashboard.title', 'es'));
      expect(await page.evaluate(() => localStorage.getItem('app-language'))).toBe('es');
    },
  );

  for (const lang of LANGS) {
    test(
      `CT-WEB-063 — fluxos principais no idioma: ${lang}`,
      { tag: ['@P2'] },
      async ({ catalog, orderForm, page }) => {
        await useLanguage(page, lang, catalog, orderForm);

        await catalog.goto();
        await catalog.search('PROD-001');
        await expect(catalog.cells('PROD-001').nth(4)).toHaveText(t('status.ATIVO', lang));

        await orderForm.goto();
        await orderForm.fillClient(uniqueClient());
        await orderForm.addProduct('PROD-002', 1);
        await orderForm.submitButton().click();
        await orderForm.expectToast(t('orders.form.createSuccess', lang));
        await expect(page).toHaveURL(/\/orders\/[0-9a-f-]{36}$/);
      },
    );
  }

  test('CT-WEB-064 — paridade dos arquivos de tradução', { tag: ['@P3'] }, () => {
    const reference = flatten('pt-BR');
    const placeholders = (text: string) => [...text.matchAll(/\{\{\s*(\w+)\s*\}\}/g)].map((m) => m[1]).sort();

    for (const lang of LANGS) {
      const entries = flatten(lang);
      expect([...entries.keys()].sort(), `chaves de ${lang}`).toEqual([...reference.keys()].sort());
      for (const [key, value] of entries) {
        expect(value.trim(), `${lang}: ${key} não pode ser vazio`).not.toBe('');
        expect(placeholders(value), `${lang}: placeholders de ${key}`).toEqual(
          placeholders(reference.get(key)!),
        );
      }
    }
  });

  for (const lang of ['en', 'es'] as const) {
    test(
      `CT-WEB-065 — mensagens de erro traduzidas: ${lang}`,
      { tag: ['@P3'] },
      async ({ orderForm, page }) => {
        await useLanguage(page, lang, orderForm);
        await orderForm.goto();

        await orderForm.addProduct('PROD-999', 1);

        await orderForm.expectToast(t('orders.form.productNotFound', lang, { id: 'PROD-999' }));
      },
    );
  }
});
