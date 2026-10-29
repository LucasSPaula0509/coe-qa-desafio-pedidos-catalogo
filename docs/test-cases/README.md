# Casos de Teste

Casos derivados das regras de [specs/business-rules.md](../../specs/business-rules.md) por técnicas formais de projeto de teste (ISTQB CTFL). O **resultado esperado vem da especificação** (RN, ou Q com a interpretação adotada), nunca do comportamento observado (ADR-QA-005).

| Arquivo | Área | Prefixo |
|---|---|---|
| [api-orders.md](api-orders.md) | API de Pedidos: criação, cálculo, validações, consulta e listagem | `CT-ORD` |
| [api-catalog.md](api-catalog.md) | API de Catálogo: consulta individual e em lote | `CT-CAT` |
| [integration.md](integration.md) | Integração Pedidos ↔ Catálogo, resiliência e consistência | `CT-INT` |
| [api-cross-cutting.md](api-cross-cutting.md) | Regras transversais: envelope de erro, `X-Request-Id`, semântica HTTP, saúde | `CT-TRV` |
| [web-e2e.md](web-e2e.md) | Interface web: Painel, Catálogo, Novo pedido, Pedidos, Idiomas, acessibilidade | `CT-WEB` |
| [performance.md](performance.md) | Performance e carga | `CT-PRF` |
| [security.md](security.md) | Segurança (OWASP API Security Top 10 2023 e Top 10 web) | `CT-SEC` |

## Formato

| Coluna | Conteúdo |
|---|---|
| ID | `CT-<ÁREA>-nnn`. Lacunas na numeração separam grupos e permitem inserções futuras sem renumerar. |
| Cenário | O que se verifica, em uma frase |
| Entrada / pré-condição | Dados e estado necessários (massa da RN-22, salvo indicação) |
| Resultado esperado | Status HTTP, código de erro, valores e efeitos colaterais (persistência) |
| Ref. | Regras (RN/NF) cobertas e, quando houver, a questão (Q) ou suspeita (H) relacionada |
| Técnica | Ver legenda |
| Prio | Prioridade derivada do risco (ver `docs/test-strategy.md` §3) |
| Nível | Onde o caso é executado (ver legenda) |

### Técnicas

| Sigla | Técnica |
|---|---|
| PE | Partição de equivalência |
| VL | Análise de valor-limite (2 valores: no limite e imediatamente fora) |
| TD | Tabela de decisão (combinação de condições) |
| CU | Caso de uso / fluxo ponta a ponta |
| CO | Teste de contrato (schema, headers, formato) |
| SE | Suposição de erro (*error guessing*) e ataques baseados em experiência |
| CA | Carga / perfil operacional (performance) |

### Prioridade

| Prio | Critério | Execução |
|---|---|---|
| P1 | Cobre risco **alto** (cálculo financeiro, preço, integridade de dados, integração crítica, fluxo principal) | Em todo PR (gate bloqueante) |
| P2 | Cobre risco **médio** (validações secundárias, contratos de erro, resiliência, UX relevante) | Em todo PR (gate bloqueante) |
| P3 | Cobre risco **baixo** (cosmético, bordas improváveis, informativo) | Na `develop` e no fechamento de release |

### Níveis de execução

| Nível | Descrição |
|---|---|
| API | Requisição HTTP caixa-preta contra o ambiente Docker Compose |
| API+DB | API com preparação e verificação direta em PostgreSQL/MongoDB (sempre com restauração do estado) |
| API+FI | API com injeção de falha na dependência (catálogo parado, lento ou com erro) |
| WEB | Navegador real (Playwright) contra o ambiente completo |
| WEB+MOCK | Navegador com respostas de API interceptadas (estados difíceis de produzir: vazio, erro) |
| PERF | Script de carga (k6) |
| SEC | Teste de segurança automatizado (API ou WEB) |
| DAST | Varredura dinâmica (OWASP ZAP) |
| ESTÁTICO | Verificação estática de artefatos (ex.: arquivos de tradução) |

## Dados de teste padrão

| Nome | Valor | Uso |
|---|---|---|
| `CLIENTE_PADRAO` | `{"name": "Cliente QA <runId>", "document": "12345678901", "email": "qa.<runId>@example.com"}` | Cliente válido em todos os casos que não testam o cliente |
| `<runId>` | Identificador único por execução e por teste (timestamp + aleatório) | Isola os dados de cada teste em um ambiente com pedidos acumulados |
| Produtos | Massa inicial da RN-22 (PROD-001 a PROD-006) e `PROD-999` (inexistente) | — |
| Regra de desconto | Inicial: mínimo 1000,00, 10%, ativa | Casos `API+DB` alteram a regra e **restauram** o estado no teardown |

Os dados são sintéticos. O documento `12345678901` é o mesmo do exemplo da documentação do SUT, e os e-mails usam o domínio reservado `example.com` (RFC 2606).
