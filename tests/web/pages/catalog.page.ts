import { expect, type Locator } from '@playwright/test';
import { AppShell } from './shell';

/** Consulta de catálogo — sut/frontend/src/app/features/catalog/pages/catalog-lookup. */
export class CatalogPage extends AppShell {
  async goto(): Promise<void> {
    await this.page.goto('/catalog');
    await expect(this.heading()).toHaveText(this.t('catalog.title'));
  }

  idsInput(): Locator {
    return this.page.getByLabel(this.t('catalog.searchLabel'));
  }

  searchButton(): Locator {
    return this.page.getByRole('button', { name: this.t('catalog.search'), exact: true });
  }

  async search(ids: string): Promise<void> {
    await this.idsInput().fill(ids);
    await this.searchButton().click();
  }

  resultsTable(): Locator {
    return this.page.getByRole('table', { name: this.t('catalog.results') });
  }

  rows(): Locator {
    return this.resultsTable().locator('tbody tr');
  }

  /** Células da linha do produto: [id, nome, preço, moeda, situação]. */
  cells(productId: string): Locator {
    return this.rows()
      .filter({ has: this.page.getByRole('cell', { name: productId, exact: true }) })
      .getByRole('cell');
  }

  notFoundAlert(ids: string): Locator {
    return this.page.getByRole('alert').filter({ hasText: this.t('catalog.notFoundIds', { ids }) });
  }

  requiredMessage(): Locator {
    return this.page.getByText(this.t('form.required'), { exact: true });
  }
}
