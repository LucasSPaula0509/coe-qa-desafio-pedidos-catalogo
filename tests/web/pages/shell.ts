import { expect, type Locator, type Page } from '@playwright/test';
import { t, type Lang } from '../support/i18n';

/**
 * Elementos comuns a todas as telas (layout/shell do SUT): menu lateral, seletor de idioma, toasts,
 * estados de erro. Os localizadores são semânticos (papel + nome acessível vindo das traduções),
 * pois o SUT não tem data-testid e não pode ser alterado (ADR-QA-003).
 */
export class AppShell {
  lang: Lang = 'pt-BR';

  constructor(readonly page: Page) {}

  t(key: string, params: Record<string, string | number> = {}): string {
    return t(key, this.lang, params);
  }

  heading(): Locator {
    return this.page.getByRole('heading', { level: 1 });
  }

  navigation(): Locator {
    return this.page.getByRole('navigation', { name: this.t('nav.menu') });
  }

  navLink(key: 'nav.dashboard' | 'nav.orders' | 'nav.catalog'): Locator {
    return this.navigation().getByRole('link', { name: this.t(key) });
  }

  languageSelector(): Locator {
    return this.page.getByRole('button', { name: this.t('language.label') });
  }

  async switchLanguage(lang: Lang): Promise<void> {
    await this.languageSelector().click();
    await this.page.getByRole('button', { name: t(`language.${lang}`, lang), exact: true }).click();
    this.lang = lang;
    await expect(this.languageSelector()).toHaveText(t(`language.${lang}`, lang));
  }

  /** Toast (role=alert) com o texto informado. */
  toast(text: string): Locator {
    return this.page.locator('ngb-toast').filter({ hasText: text });
  }

  async expectToast(text: string, requestId?: string): Promise<void> {
    const toast = this.toast(text);
    await expect(toast).toBeVisible();
    if (requestId) {
      await expect(toast).toContainText(this.t('errors.requestId', { id: requestId }));
    }
  }

  /** Estado de erro de página (componente error-state, role=alert). */
  errorState(message: string): Locator {
    return this.page.locator('app-error-state').getByRole('alert').filter({ hasText: message });
  }

  retryButton(): Locator {
    return this.page.getByRole('button', { name: this.t('common.retry') });
  }
}
