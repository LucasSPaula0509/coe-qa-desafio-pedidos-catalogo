import { expect, type APIRequestContext } from '@playwright/test';

/**
 * Preparação de dados e oráculo via API (ADR-QA-003): cada teste cria os próprios pedidos e
 * compara o que a interface exibe com o que a API devolve, sem depender de outros testes.
 */
export const ORDER_API_URL = process.env.ORDER_API_URL ?? 'http://localhost:8080';
export const CATALOG_API_URL = process.env.CATALOG_API_URL ?? 'http://localhost:8081';

export interface OrderItem {
  productId: string;
  productName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Order {
  id: string;
  client: { name: string; document: string; email: string };
  items: OrderItem[];
  subtotal: number;
  discount: number;
  total: number;
  status: string;
  createdAt: string;
}

export interface OrderPage {
  content: Order[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TestClient {
  name: string;
  document: string;
  email: string;
}

/** Cliente sintético e único (CLIENTE_PADRAO em docs/test-cases/README.md). */
export function uniqueClient(): TestClient {
  const runId = `w${Date.now().toString(36)}${Math.floor(Math.random() * 36 ** 4).toString(36)}`;
  return { name: `Cliente Web ${runId}`, document: '12345678901', email: `qa.${runId}@example.com` };
}

export class OrdersApi {
  constructor(private readonly request: APIRequestContext) {}

  async create(items: Array<[productId: string, quantity: number]>, client = uniqueClient()): Promise<Order> {
    const response = await this.request.post(`${ORDER_API_URL}/api/v1/orders`, {
      data: { client, items: items.map(([productId, quantity]) => ({ productId, quantity })) },
    });
    expect(response.status(), `pré-condição: pedido criado — ${await response.text()}`).toBe(201);
    return (await response.json()) as Order;
  }

  async get(id: string): Promise<Order> {
    const response = await this.request.get(`${ORDER_API_URL}/api/v1/orders/${id}`);
    expect(response.status()).toBe(200);
    return (await response.json()) as Order;
  }

  async list(page: number, size: number): Promise<OrderPage> {
    const response = await this.request.get(`${ORDER_API_URL}/api/v1/orders`, { params: { page, size } });
    expect(response.status()).toBe(200);
    return (await response.json()) as OrderPage;
  }

  /** Garante ao menos {@code minimum} pedidos no ambiente (para cenários de paginação). */
  async ensureAtLeast(minimum: number): Promise<void> {
    const { totalElements } = await this.list(0, 1);
    for (let i = totalElements; i < minimum; i++) {
      await this.create([['PROD-002', 1]]);
    }
  }
}
