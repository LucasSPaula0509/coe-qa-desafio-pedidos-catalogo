# Suíte de Testes de API

Automação dos casos de API, integração e regras transversais (`CT-ORD`, `CT-CAT`, `CT-INT`, `CT-TRV`): **111 casos, 190 execuções** (casos parametrizados geram uma execução por variação). Stack: Java 17, JUnit 5, RestAssured, AssertJ, JSON Schema, Allure ([ADR-QA-002](../../docs/adr/ADR-QA-002-stack-testes-api.md)). Injeção de falhas e dados: [ADR-QA-006](../../docs/adr/ADR-QA-006-injecao-de-falhas-e-dados.md).

## Pré-requisitos

- Java 17 e Maven 3.9
- Docker com Compose v2. A suíte usa o CLI `docker` no CT-INT-015.
- Ambiente do SUT com o *override* de teste, que insere o proxy de falhas entre `order-service` e catálogo:

```bash
# na raiz do repositório
docker compose -f sut/docker-compose.yml -f tests/infra/docker-compose.qa.yml up --build -d
```

| Serviço | Endereço | Variável para sobrescrever |
|---|---|---|
| order-service | http://localhost:8080 | `ORDER_BASE_URL` |
| catalog-service | http://localhost:8081 | `CATALOG_BASE_URL` |
| Proxy de falhas (admin WireMock) | http://localhost:8089 | `CATALOG_PROXY_ADMIN_URL` |
| Container do proxy | `sut-catalog-proxy-1` | `CATALOG_PROXY_CONTAINER` |
| PostgreSQL | `jdbc:postgresql://localhost:5432/orders` (orders/orders) | `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD` |
| MongoDB | `mongodb://localhost:27017`, base `catalog` | `MONGODB_URI`, `MONGODB_DATABASE` |

As variáveis podem ser passadas como ambiente ou como `-D` no Maven.

## Execução

```bash
cd tests/api

mvn test                                         # suíte completa (inclui defeitos conhecidos, que falham)
mvn test -DexcludedGroups=defeito-conhecido      # visão do gate: tudo deve passar
mvn test -Dgroups="P1"                           # somente prioridade P1
mvn test -Dgroups="fault"                        # somente injeção de falhas
mvn test -Dtest=DiscountRuleTest                 # uma classe
mvn spotless:apply                               # formatação (o check roda na fase validate)
```

Para restaurar a massa original do ambiente, use `docker compose -f sut/docker-compose.yml -f tests/infra/docker-compose.qa.yml down -v` e suba de novo.

### Tags JUnit

| Tag | Significado |
|---|---|
| `P1`, `P2`, `P3` | Prioridade por risco (`docs/test-strategy.md` §3) |
| `api`, `integration` | Camada |
| `db` | Prepara ou verifica dados direto no PostgreSQL/MongoDB (sempre com restauração) |
| `fault` | Usa o proxy de injeção de falhas |
| `serial` | Altera estado global (regra de desconto, produto, proxy). Não paralelizar |
| `defeito-conhecido` | Reproduz um defeito registrado (`@KnownDefect(value = "<nº da Issue>", suspicion = "H-xx")`). Continua executando e falhando (estratégia §12) |

## Relatórios

| Saída | Local | Uso |
|---|---|---|
| JUnit XML | `target/surefire-reports/` | Gate do CI e resumo (`node scripts/summarize-junit.mjs tests/api/target/surefire-reports`) |
| Allure | `target/allure-results/` | Relatório navegável: `npx allure-commandline serve target/allure-results`. Cada requisição e resposta vira anexo, e os defeitos conhecidos têm link direto para a Issue |

## Estrutura

```
src/test/java/com/stefanini/qa/api/
  support/      Api (clientes HTTP), ApiAssertions (envelope, X-Request-Id, persistência),
                Money (BigDecimal), Orders, Ids, TestConfig, KnownDefect
  data/         Products (massa da RN-22 como oráculo), TestClient, OrderPayload (payloads válidos e inválidos)
  db/           OrdersDatabase (PostgreSQL), CatalogDatabase (MongoDB): snapshot e restauração
  fault/        CatalogProxy: stubs de falha, registro de chamadas, parar/religar o destino
  orders/       CT-ORD — criação, cálculo, desconto, validações, 422, consulta, listagem
  catalog/      CT-CAT — consulta individual e em lote
  integration/  CT-INT — integração real, snapshot, resiliência
  crosscutting/ CT-TRV — envelope, X-Request-Id, semântica HTTP, saúde, OpenAPI
src/test/resources/schemas/  JSON Schemas derivados de sut/docs/api.md (contrato documentado, não o observado)
```

## Convenções

- **O ID do caso no nome do teste** (`@DisplayName("CT-ORD-007 — …")` ou o `name` do `@ParameterizedTest`). A matriz de rastreabilidade detecta a automação por esse ID.
- **O esperado vem do caso de teste** (`docs/test-cases/`), que vem da regra (`specs/`). Os valores da massa estão em `data/Products` como constantes, e não são lidos do próprio sistema.
- **Valores monetários comparados como `BigDecimal`** (`Money.assertAmount`), com o JSON lido em `BigDecimal`.
- **Isolamento:** cada teste cria o próprio cliente (`TestClient.unique()`). As asserções de listagem são relativas, nunca contagens absolutas.
- **Sem espera fixa:** as esperas são por condição (proxy pronto, caminho do pedido recuperado), com prazo máximo e mensagem de erro.
- **Erro de entrada nunca pode ser 5xx:** `assertClientError` aceita apenas os 4xx documentados e exige envelope.

## Resultado de referência (2026-10-29, ambiente local)

| Execução | Casos | Aprovados | Falhas | Duração |
|---|---|---|---|---|
| Completa (3 execuções seguidas) | 190 | 146 | 44, **todas** com `@KnownDefect`, conjunto idêntico nas 3 | ~65 s |
| Visão do gate (`-DexcludedGroups=defeito-conhecido`) | 129 | 129 | 0 | ~57 s |

O detalhamento dos defeitos está em [docs/test-execution/api-2026-10-29.md](../../docs/test-execution/api-2026-10-29.md).
