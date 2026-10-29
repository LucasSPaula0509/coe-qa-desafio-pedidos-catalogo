import { test as base, type Page, type Request } from '@playwright/test';
import { CatalogPage } from '../pages/catalog.page';
import { DashboardPage } from '../pages/dashboard.page';
import { OrderDetailPage } from '../pages/order-detail.page';
import { OrderFormPage } from '../pages/order-form.page';
import { OrderListPage } from '../pages/order-list.page';
import { OrdersApi } from './api';

/**
 * Fixtures da suíte: páginas (Page Object Model) e API para preparar dados. Cada teste roda num
 * contexto de navegador novo (localStorage vazio → idioma padrão pt-BR).
 */
type Fixtures = {
  ordersApi: OrdersApi;
  dashboard: DashboardPage;
  catalog: CatalogPage;
  orderForm: OrderFormPage;
  orderList: OrderListPage;
  orderDetail: OrderDetailPage;
};

export const test = base.extend<Fixtures>({
  ordersApi: async ({ request }, use) => use(new OrdersApi(request)),
  dashboard: async ({ page }, use) => use(new DashboardPage(page)),
  catalog: async ({ page }, use) => use(new CatalogPage(page)),
  orderForm: async ({ page }, use) => use(new OrderFormPage(page)),
  orderList: async ({ page }, use) => use(new OrderListPage(page)),
  orderDetail: async ({ page }, use) => use(new OrderDetailPage(page)),
});

export { expect } from '@playwright/test';

/** Coleta as requisições que casam com o filtro, para provar que algo foi (ou não) chamado. */
export function captureRequests(page: Page, matches: (request: Request) => boolean): Request[] {
  const captured: Request[] = [];
  page.on('request', (request) => {
    if (matches(request)) {
      captured.push(request);
    }
  });
  return captured;
}

export const isOrderCreation = (request: Request) =>
  request.method() === 'POST' && new URL(request.url()).pathname === '/api/orders';

export const isCatalogCall = (request: Request) => new URL(request.url()).pathname.startsWith('/api/catalog');

/** Envelope de erro do SUT (ADR-008) para respostas simuladas. */
export function errorEnvelope(code: string, requestId: string, message = 'simulated') {
  return JSON.stringify({ error: { code, message, details: [] }, requestId });
}
