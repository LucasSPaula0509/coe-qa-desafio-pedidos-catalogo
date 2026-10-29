import type { Locator } from '@playwright/test';
import { AppShell } from './shell';

/** Painel (Dashboard) — sut/frontend/src/app/pages/dashboard. */
export class DashboardPage extends AppShell {
  async goto(): Promise<void> {
    await this.page.goto('/');
  }

  /** Valor em destaque do card cujo rótulo é a chave informada. */
  indicator(labelKey: 'dashboard.ordersTotal' | 'dashboard.valueLoaded'): Locator {
    return this.page
      .locator('.card-body')
      .filter({ has: this.page.getByText(this.t(labelKey), { exact: true }) })
      .locator('p.h2');
  }

  /** Card "Pedidos por status" (contagens e "Pedidos carregados: N"). */
  statusCard(): Locator {
    return this.page
      .locator('.card-body')
      .filter({ has: this.page.getByText(this.t('dashboard.byStatus'), { exact: true }) });
  }

  /** Contagem exibida ao lado do badge do status (rótulo traduzido). */
  statusCount(statusLabel: string): Locator {
    return this.statusCard()
      .locator('span.d-inline-flex')
      .filter({ has: this.page.locator('.badge', { hasText: statusLabel }) })
      .locator('span.fw-semibold');
  }

  shortcut(key: 'dashboard.newOrder' | 'dashboard.viewOrders' | 'dashboard.viewCatalog'): Locator {
    return this.page.getByRole('link', { name: this.t(key), exact: true });
  }

  gapNotice(): Locator {
    return this.page.getByRole('note').filter({ hasText: this.t('dashboard.gapTitle') });
  }
}
