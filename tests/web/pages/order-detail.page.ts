import { expect, type Locator } from '@playwright/test';
import { AppShell } from './shell';

/** Detalhe do pedido — sut/frontend/src/app/features/orders/pages/order-detail. */
export class OrderDetailPage extends AppShell {
  async goto(orderId: string): Promise<void> {
    await this.page.goto(`/orders/${orderId}`);
    await expect(this.heading()).toHaveText(this.t('orders.detail.title'));
  }

  /** Valor (dd) associado ao rótulo (dt) informado, nos cards de cliente e de resumo. */
  value(labelKey: string): Locator {
    const label = this.t(labelKey);
    return this.page
      .locator('dt')
      .filter({ hasText: new RegExp(`^\\s*${label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*$`) })
      .locator('xpath=following-sibling::dd[1]');
  }

  itemsTable(): Locator {
    return this.page.getByRole('table', { name: this.t('orders.detail.itemsCard') });
  }

  /** Células da linha do item: [produto, quantidade, preço unitário, total da linha]. */
  itemCells(productId: string): Locator {
    return this.itemsTable().locator('tbody tr').filter({ hasText: productId }).getByRole('cell');
  }

  backLink(): Locator {
    return this.page.getByRole('link', { name: this.t('common.back') });
  }
}
