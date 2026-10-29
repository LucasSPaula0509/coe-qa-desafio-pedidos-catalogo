import { expect, type Locator } from '@playwright/test';
import { AppShell } from './shell';

/** Listagem de pedidos — sut/frontend/src/app/features/orders/pages/order-list. */
export class OrderListPage extends AppShell {
  async goto(): Promise<void> {
    await this.page.goto('/orders');
    await expect(this.heading()).toHaveText(this.t('orders.title'));
  }

  table(): Locator {
    return this.page.getByRole('table', { name: this.t('orders.title') });
  }

  rows(): Locator {
    return this.table().locator('tbody tr');
  }

  /** Link "Visualizar" do pedido, identificado pelo id completo no aria-label. */
  viewLink(orderId: string): Locator {
    return this.page.getByRole('link', { name: this.t('orders.list.viewAria', { id: orderId }) });
  }

  /** Ids completos dos pedidos exibidos, na ordem (extraídos dos aria-labels dos links). */
  async displayedIds(): Promise<string[]> {
    const prefix = this.t('orders.list.viewAria', { id: '' });
    const labels = await this.rows()
      .getByRole('link')
      .evaluateAll((links) => links.map((link) => link.getAttribute('aria-label') ?? ''));
    return labels.map((label) => label.replace(prefix, '').trim());
  }

  pagination(): Locator {
    return this.page.getByRole('navigation', { name: this.t('orders.list.pagination') });
  }

  /**
   * Controle "próxima página". O rótulo acessível vem do ng-bootstrap e não é traduzido pelo SUT
   * ("Next" em todos os idiomas — observação registrada no relatório de qualidade).
   */
  nextPageLink(): Locator {
    return this.pagination().getByRole('link', { name: 'Next' });
  }
}
