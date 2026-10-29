import { expect, test } from '../support/fixtures';
import { t } from '../support/i18n';

/** docs/test-cases/web-e2e.md — A. Shell e navegação. */
test.describe('Shell e navegação', () => {
  test('CT-WEB-001 — idioma padrão pt-BR na primeira visita', { tag: ['@P1'] }, async ({ dashboard }) => {
    test.info().annotations.push({
      type: 'Q-11',
      description: `O PDF chama a tela de "Painel"; a UI exibe "${t('nav.dashboard')}" (observação de UX).`,
    });

    await dashboard.goto();

    await expect(dashboard.navLink('nav.dashboard')).toBeVisible();
    await expect(dashboard.navLink('nav.orders')).toBeVisible();
    await expect(dashboard.navLink('nav.catalog')).toBeVisible();
    await expect(dashboard.languageSelector()).toHaveText(t('language.pt-BR'));
    await expect(dashboard.heading()).toHaveText(t('dashboard.title'));
  });

  test('CT-WEB-002 — navegação pelo menu lateral', { tag: ['@P2'] }, async ({ dashboard, page }) => {
    await dashboard.goto();
    const destinations = [
      { link: 'nav.orders', path: '/orders', title: 'orders.title' },
      { link: 'nav.catalog', path: '/catalog', title: 'catalog.title' },
      { link: 'nav.dashboard', path: '/', title: 'dashboard.title' },
    ] as const;

    for (const { link, path, title } of destinations) {
      await dashboard.navLink(link).click();
      await expect(page).toHaveURL(path);
      await expect(dashboard.heading()).toHaveText(t(title));
      await expect(dashboard.navLink(link)).toHaveClass(/active/);
    }
  });

  test('CT-WEB-003 — rota inexistente leva à página 404', { tag: ['@P3'] }, async ({ dashboard, page }) => {
    await page.goto('/rota-inexistente');

    await expect(page).toHaveURL('/404');
    await expect(page.getByRole('heading', { name: t('notFound.title') })).toBeVisible();
    await page.getByRole('link', { name: t('notFound.home') }).click();
    await expect(dashboard.heading()).toHaveText(t('dashboard.title'));
  });
});
