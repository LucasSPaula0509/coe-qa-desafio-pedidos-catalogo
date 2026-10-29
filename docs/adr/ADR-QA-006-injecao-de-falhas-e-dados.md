# ADR-QA-006: Injeção de falhas por proxy e preparação de dados direto nos bancos

**Status:** Aceita

## Contexto

Parte das regras só pode ser verificada com condições que a API não produz sozinha:

- **Resiliência** (RN-10): catálogo lento, indisponível, derrubando a conexão ou respondendo erro.
- **Contrato da integração** (RN-09): uma única chamada em lote por pedido, com propagação do `X-Request-Id`.
- **Regra de desconto** (RN-13): fronteira exata do mínimo, HALF_UP e ausência de regra ativa. A massa inicial não atinge esses pontos (Q-09).
- **Snapshot** (RN-11): mudar o preço ou o nome de um produto. O catálogo não tem escrita (RN-21).
- **All-or-nothing** (RN-07): provar que nada foi persistido, o que a API só mostra indiretamente.

A restrição é **não alterar o SUT** (ADR-QA-001).

## Decisão

1. **Proxy WireMock entre `order-service` e `catalog-service`** (`tests/infra/docker-compose.qa.yml`):
   - O *override* do Compose aponta `CATALOG_BASE_URL` para o proxy. É uma variável de ambiente já suportada pelo SUT, sem alteração de código.
   - Sem stubs ativos, o proxy é transparente (`--proxy-all`). Os testes criam stubs de prioridade alta para atraso, erro HTTP e conexão interrompida, sempre marcados com metadata `qa=fault`. Eles são removidos depois de cada caso, sem tocar no mapping de proxy padrão.
   - O registro de requisições do proxy é o oráculo para "1 chamada em lote" e para a propagação do `X-Request-Id`.
   - "Destino inacessível" é simulado parando o container do proxy. A recuperação é medida pelo caminho real do pedido (sonda com produto inexistente → 422, sem persistência), porque o container religado pode trocar de IP e a JVM do `order-service` mantém o DNS em cache por alguns segundos.
2. **Acesso direto a PostgreSQL e MongoDB** para preparar e verificar dados:
   - Consultas parametrizadas e escopo mínimo: a tabela `discount_rule` e o documento do produto alterado.
   - **Snapshot e restauração** em `@BeforeEach`/`@AfterEach`, com verificação de que a restauração aconteceu, mesmo quando o teste falha.
   - A persistência é verificada por e-mail único do cliente de cada teste.
3. **Isolamento:** as classes que mudam estado global têm a tag `serial`. A suíte roda sem paralelismo entre classes, e o CI começa de um ambiente limpo (`docker compose down -v`).

## Alternativas consideradas

- **Toxiproxy:** excelente para falhas de rede (latência, banda, conexão recusada), mas não registra requisições nem permite responder com corpo ou status específicos. O WireMock cobre os dois papéis com uma ferramenta só.
- **Parar o `catalog-service` real:** testa a indisponibilidade, mas não permite atraso, erro HTTP nem contagem de chamadas, e afetaria também as suítes do catálogo que estiverem rodando.
- **Endpoints de teste no SUT:** proibido pela restrição de não alterar o código.

## Consequências

- **Positivas:**
  - Cenários de falha **determinísticos e reproduzíveis**, que viraram evidência objetiva. A H-11 (timeout tratado como 500) foi confirmada por injeção, e não por observação ocasional.
  - Os casos de dados atingem fronteiras impossíveis com a massa padrão, sem mexer na massa original.
- **Negativas:**
  - O ambiente de teste tem um salto de rede a mais, desprezível para os testes funcionais. Na performance, o proxy fica transparente ou é removido do *override*.
  - Os testes que acessam os bancos dependem do schema documentado (`sut/docs/database.md`). Uma mudança de schema quebra o teste de forma explícita, o que é o desejado.
  - Parar e religar o container do proxy custa alguns segundos de recuperação (DNS). O custo é medido e anexado ao relatório.
