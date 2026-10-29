# ADR-QA-002: Testes de API com Java 17, JUnit 5 e RestAssured

**Status:** Aceita

## Contexto

A Parte 2 exige automatizar os testes das APIs de Pedidos e de Catálogo e a integração entre elas, com stack justificada. Os testes são **caixa-preta**, contra o ambiente Docker Compose do SUT, mas alguns cenários exigem **preparar e verificar dados diretamente nos bancos**:

- Desativar ou trocar a regra de desconto, para cobrir "sem regra ativa" e o arredondamento half-up.
- Confirmar que um pedido rejeitado não deixou registro parcial.
- Alterar o preço de um produto no MongoDB para provar o snapshot de preço.

Nada disso é possível só pelas APIs, porque o catálogo é somente leitura e a regra de desconto não tem endpoint.

O SUT é Java 17 + Spring Boot 3, com Maven e Spotless, mantido por um squad Java.

## Decisão

| Componente | Escolha | Papel |
|---|---|---|
| Linguagem/build | Java 17 + Maven 3.9 | Mesma toolchain do SUT |
| Runner | JUnit 5 (Jupiter) | Tags por prioridade/suíte (`@Tag("P1")`), `@DisplayName` com o ID do caso (`CT-ORD-001`) |
| Cliente HTTP / DSL | RestAssured 5 | DSL given/when/then legível; logs de request/response em caso de falha |
| Asserções | AssertJ + RestAssured JSON Schema Validator | Asserções fluentes; contrato do envelope de erro validado por JSON Schema |
| Dados | Driver JDBC PostgreSQL + MongoDB Java Driver | Preparação e verificação de dados nos bancos (sem tocar no código do SUT) |
| Relatório | Allure (JUnit 5) + Surefire XML | Relatório navegável com evidências; XML para o gate do CI |
| Qualidade do código | Spotless (mesma configuração do SUT) | Formatação como gate, igual ao padrão do SUT |

## Alternativas consideradas

- **Playwright (TypeScript) também para API:** uma linguagem só nas suítes funcionais. Foi descartado como principal porque o acesso a PostgreSQL e MongoDB e a aritmética monetária com `BigDecimal` (ADR-015 do SUT) ficam mais naturais e seguros em Java. Também afastaria a suíte do squad que mantém o backend.
- **Postman/Newman:** rápido para começar, mas fraco em preparação de dados, reuso e revisão de código em PR.
- **Karate:** DSL própria, com curva de aprendizado e debugging menos familiares para um squad Java.

## Consequências

- **Positivas:**
  - O squad do backend lê, mantém e revisa a suíte com as mesmas ferramentas do SUT. Isso conta para shift-left e para a governança do COE.
  - Comparações monetárias exatas com `BigDecimal`, sem erro de ponto flutuante.
  - Os cenários que dependem de dados ficam automatizáveis de forma determinística, com setup e teardown explícitos.
- **Negativas:**
  - Duas linguagens nas suítes funcionais (Java na API, TypeScript na UI). Fica mitigado pela separação clara por diretório e por READMEs por suíte.
  - Os testes que mexem no banco ficam acoplados ao schema (`discount_rule`, `orders`, `products`). O schema é documentado em `sut/docs/database.md`, e qualquer mudança dele quebra o teste de forma explícita.
