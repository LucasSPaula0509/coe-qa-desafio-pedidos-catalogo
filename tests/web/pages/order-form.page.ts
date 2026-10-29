import { expect, type Locator } from '@playwright/test';
import type { TestClient } from '../support/api';
import { AppShell } from './shell';

/** Novo pedido — sut/frontend/src/app/features/orders/pages/order-form. */
export class OrderFormPage extends AppShell {
  async goto(): Promise<void> {
    await this.page.goto('/orders/new');
    await expect(this.heading()).toHaveText(this.t('orders.form.title'));
  }

  nameInput(): Locator {
    return this.page.getByLabel(this.t('orders.form.name'), { exact: true });
  }

  documentInput(): Locator {
    return this.page.getByLabel(this.t('orders.form.document'), { exact: true });
  }

  emailInput(): Locator {
    return this.page.getByLabel(this.t('orders.form.email'), { exact: true });
  }

  productInput(): Locator {
    return this.page.getByLabel(this.t('orders.form.productId'), { exact: true });
  }

  quantityInput(): Locator {
    return this.page.getByLabel(this.t('orders.form.quantity'), { exact: true });
  }

  addButton(): Locator {
    return this.page.getByRole('button', { name: this.t('orders.form.addProduct'), exact: true });
  }

  submitButton(): Locator {
    return this.page.getByRole('button', { name: this.t('orders.form.submit'), exact: true });
  }

  cancelButton(): Locator {
    return this.page.getByRole('button', { name: this.t('orders.form.cancel'), exact: true });
  }

  async fillClient(client: TestClient): Promise<void> {
    await this.nameInput().fill(client.name);
    await this.documentInput().fill(client.document);
    await this.emailInput().fill(client.email);
  }

  /** Adiciona um produto e aguarda o fim da validação no catálogo (botão volta ao estado normal). */
  async addProduct(productId: string, quantity = 1): Promise<void> {
    await this.productInput().fill(productId);
    await this.quantityInput().fill(String(quantity));
    await this.addButton().click();
    await expect(this.addButton()).toBeEnabled();
  }

  itemsTable(): Locator {
    return this.page.getByRole('table', { name: this.t('orders.form.selectedItems') });
  }

  itemRows(): Locator {
    return this.itemsTable().locator('tbody tr');
  }

  /** Células da linha do item: [produto, quantidade, preço unitário, total da linha, ação]. */
  itemCells(productId: string): Locator {
    return this.itemRows().filter({ hasText: productId }).getByRole('cell');
  }

  rowQuantity(productName: string): Locator {
    return this.page.getByLabel(`${this.t('orders.detail.quantity')}: ${productName}`, { exact: true });
  }

  removeButton(productName: string): Locator {
    return this.page.getByRole('button', { name: this.t('orders.form.removeAria', { name: productName }) });
  }

  noItemsMessage(): Locator {
    return this.page.getByText(this.t('orders.form.noItems'), { exact: true });
  }

  /** Valor do subtotal informativo no card "Resumo". */
  subtotal(): Locator {
    return this.page
      .locator('.card')
      .filter({ hasText: this.t('orders.form.summary') })
      .locator('span.fw-semibold');
  }

  fieldError(key: string): Locator {
    return this.page.locator('.invalid-feedback').filter({ hasText: this.t(key) });
  }
}
