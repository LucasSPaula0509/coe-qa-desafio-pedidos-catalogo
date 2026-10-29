# Desafio Técnico — COE Leader Desenvolvimento (Stefanini)

Sistema composto por dois microsserviços Java/Spring Boot desenvolvidos com Clean Architecture, testes automatizados, Docker e CI/CD:

| Serviço | Porta | Banco | Responsabilidade |
|---|---|---|---|
| **order-service** | 8080 | PostgreSQL 16 | Criação, listagem e consulta de pedidos, com regra de desconto configurável |
| **catalog-service** | 8081 | MongoDB 7 | Catálogo de produtos com histórico de preços e consulta de preço vigente |

O order-service valida itens e preços no momento da criação do pedido por meio de **uma única chamada em lote** ao catalog-service (padrão anti-N+1), com retry, timeout classificado e snapshot de preço/nome persistido no pedido.

## Arquitetura

```
Client ──> order-service (8080) ──> PostgreSQL (orders, order_items, discount_rule)
                    │
                    └── 1 chamada em lote ──> catalog-service (8081) ──> MongoDB (products)
```

Ambos seguem Clean Architecture em pacotes `domain` / `application` / `infrastructure` / `presentation`. Detalhes em [docs/architecture.md](docs/architecture.md) e decisões registradas em [docs/adr/](docs/adr/).

## Requisitos

- Java 17 e Maven 3.9 (para build local)
- Docker + Docker Compose (para execução integrada e testes de integração)

## Execução (Docker Compose)

```bash
docker compose up --build
```

Sobe PostgreSQL, MongoDB (com seed de produtos), catalog-service e order-service. O schema do PostgreSQL é criado pelo Flyway, incluindo a regra de desconto inicial (pedidos a partir de R$ 1.000,00 recebem 10%).

### Criar um pedido

```bash
curl -i -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "client": {"name": "Maria Silva", "document": "12345678901", "email": "maria@example.com"},
    "items": [
      {"productId": "PROD-001", "quantity": 2},
      {"productId": "PROD-002", "quantity": 1}
    ]
  }'
```

Resposta `201 Created` com `Location` do pedido criado:

```json
{
  "id": "…",
  "client": {"name": "Maria Silva", "document": "12345678901", "email": "maria@example.com"},
  "items": [
    {"productId": "PROD-001", "productName": "Notebook Dell Inspiron 15", "unitPrice": 3500.00, "quantity": 2, "lineTotal": 7000.00},
    {"productId": "PROD-002", "productName": "Mouse Logitech MX Master 3S", "unitPrice": 499.90, "quantity": 1, "lineTotal": 499.90}
  ],
  "subtotal": 7499.90,
  "discount": 749.99,
  "total": 6749.91,
  "status": "CRIADO",
  "createdAt": "2026-09-25T17:00:00Z"
}
```

### Listar e consultar pedidos

```bash
curl 'http://localhost:8080/api/v1/orders?page=0&size=20&status=CRIADO'
curl 'http://localhost:8080/api/v1/orders/{id}'
```

### Consultar o catálogo

```bash
curl http://localhost:8081/api/v1/products/PROD-001
curl -X POST http://localhost:8081/api/v1/products/prices \
  -H "Content-Type: application/json" \
  -d '{"productIds": ["PROD-001", "PROD-002"]}'
```

Swagger UI: `http://localhost:8080/swagger-ui.html` (orders) e `http://localhost:8081/swagger-ui.html` (catalog).

## Desenvolvimento local

Cada serviço é um Maven module independente:

```bash
# unit tests (sem Docker)
mvn test

# unit + integration tests (requer Docker para Testcontainers)
mvn verify

# formatação (Spotless roda check no phase validate)
mvn spotless:apply
```

Variáveis de ambiente suportadas pelo order-service: `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `CATALOG_BASE_URL`, `CATALOG_CONNECT_TIMEOUT_MS`, `CATALOG_READ_TIMEOUT_MS`. Pelo catalog-service: `MONGODB_URI`.



## Testes

- **Unitários** (Surefire, `*Test`): regras de domínio, cálculo de desconto, use cases com mocks — 25 testes no order-service e 14 no catalog-service.
- **Integração** (Failsafe, `*IT`, executados no CI com Docker/Testcontainers): API completa com MockWebServer simulando o catálogo (incluindo asserção de **uma única chamada HTTP por pedido**) e persistência real em PostgreSQL (incluindo asserção de **exatamente 3 prepared statements** na listagem paginada, comprovando a ausência de N+1).

## Documentação

- [Levantamento de requisitos](docs/requirements.md)
- [Arquitetura](docs/architecture.md)
- [Contratos de API](docs/api.md)
- [Modelagem de dados](docs/database.md)
- [Uso de IA no desenvolvimento](docs/ai-development.md)
- [Guia de code review](docs/code-review-guide.md)
- [Dimensionamento técnico da proposta](docs/technical-estimation.md)
- [Decisões de arquitetura (ADRs)](docs/adr/)

## Estrutura do repositório

```
catalog-service/     Serviço de catálogo (MongoDB)
order-service/       Serviço de pedidos (PostgreSQL)
docker/              Scripts de inicialização (seed do MongoDB)
.github/             CI (GitHub Actions) e Dependabot
docs/                Documentação e ADRs
docker-compose.yml   Orquestração local
```
