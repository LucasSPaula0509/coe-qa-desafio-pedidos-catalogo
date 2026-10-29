# Estratégia de Testes — Gestão de Pedidos e Catálogo

| Item | Valor |
|---|---|
| Sistema sob teste | Gestão de Pedidos e Catálogo (`sut/`, integridade em `sut.sha256`) |
| Base de teste | `sut/docs/` + PDF do desafio §2.2, consolidados em [specs/](../specs/README.md) |
| Documentos relacionados | [Plano de testes](test-plan.md) · [Casos de teste](test-cases/README.md) · [Rastreabilidade](traceability-matrix.md) · [ADRs](adr/) |
| Versão | 1.0 |

## Resumo executivo

O sistema registra pedidos e calcula o valor a cobrar a partir de preços que **não** estão sob seu controle: ele consulta o catálogo a cada pedido. Os maiores riscos de negócio são:

- **Cobrar o valor errado:** preço, desconto ou arredondamento.
- **Aceitar pedidos que deveriam ser recusados:** produto inativo ou sem preço.
- **Falhar quando o catálogo falha.**

A estratégia concentra o esforço nesses pontos:

1. **A maior parte dos testes (≈ 60%) roda na camada de API**, que é rápida, estável e cobre todas as regras de cálculo e validação com valores exatos.
2. **A interface é testada nas jornadas que o usuário executa** (consultar catálogo, criar pedido, acompanhar pedidos, trocar idioma), confirmando que mostra o que o sistema calculou.
3. **A integração com o catálogo é testada com falhas simuladas** (fora do ar, lento, com erro), porque é o ponto mais frágil da arquitetura.
4. **Performance e segurança têm limites objetivos**, que bloqueiam a esteira automaticamente.
5. **Nenhum deploy acontece sem a esteira verde.** Defeitos críticos ou altos abertos impedem o release, salvo exceção formal.

Toda a especificação segue **Spec-Driven Development**: o comportamento esperado vem da documentação, e não do que o sistema faz hoje. Por isso, divergências aparecem como defeitos e não ficam escondidas nos testes. A IA é usada em todas as etapas, sempre com validação humana registrada.

## 1. Objetivos

| Objetivo | Medida |
|---|---|
| Verificar que o sistema atende à documentação oficial | 100% das regras (RN/NF) cobertas por ao menos um caso ([matriz](traceability-matrix.md)) |
| Detectar defeitos o mais cedo possível (shift-left) | Divergências identificadas já na análise da especificação (Q/H) e confirmadas por teste automatizado |
| Fornecer confiança para a decisão de release | Quality gates objetivos no CI/CD e resumo de qualidade por versão |
| Manter suítes confiáveis e sustentáveis | Taxa de instabilidade (*flakiness*) < 2%; esteira de PR < 15 min; código de teste revisado como código de produção |

## 2. Escopo

**Dentro do escopo:** APIs do `order-service` e do `catalog-service`; integração entre eles; interface web (Painel, Catálogo, Novo pedido, Pedidos, Idiomas); performance e carga; segurança da superfície exposta; consistência da documentação com o comportamento.

**Fora do escopo:**

| Item | Motivo |
|---|---|
| Testes unitários e de componente dos serviços | Já existem no SUT (39 no backend, 53 no front-end) e rodam no CI dele. Não são duplicados (ver §4). |
| Autenticação e autorização | Não existem por decisão documentada (ADR-013 do SUT). O risco é registrado no resumo de qualidade. |
| Ciclo de vida do pedido além de `CRIADO` | Não existe no sistema (ADR-012 do SUT) |
| Cadastro de produtos | Não há endpoints (catálogo somente leitura, RN-21) |
| Certificação de capacidade de produção | Exige ambiente dedicado e equivalente à produção. No CI, a performance é medida como regressão (ADR-QA-004). |
| Navegadores móveis e responsividade fina | Fora do enunciado. Chromium é obrigatório; Firefox e WebKit rodam em job estendido. |

## 3. Análise de risco de produto

O risco é avaliado como **impacto × probabilidade**, e o nível define a **prioridade** dos casos (P1 = alto, P2 = médio, P3 = baixo) e a ordem de automação.

| ID | Risco | Impacto | Probab. | Nível | Mitigação (tipos de teste e casos) |
|---|---|---|---|---|---|
| R-01 | Cálculo financeiro incorreto (linha, subtotal, desconto, arredondamento, total) | Alto | Média | **Alto** | Valores-limite e tabela de decisão na API, com valores exatos em `BigDecimal` (CT-ORD-001..013); regra de desconto manipulada no banco para atingir fronteiras e HALF_UP |
| R-02 | Preço errado no pedido (janela não vigente, valor forjado pelo cliente) | Alto | Média | **Alto** | CT-ORD-014/015, CT-INT-001, CT-CAT-002/004/005, CT-SEC-001 |
| R-03 | Pedido inválido aceito ou persistido parcialmente | Alto | Baixa | **Alto** | Verificação direta no banco após cada rejeição (CT-ORD-050..056) |
| R-04 | Falha na integração com o catálogo (indisponível, lento, limites do lote) | Alto | Média | **Alto** | Injeção de falhas e limites (CT-INT-006..012); suspeita H-01 |
| R-05 | Snapshot violado (pedidos antigos alterados por mudança no catálogo) | Alto | Baixa | **Médio** | CT-INT-002/003 |
| R-06 | Degradação de desempenho sob carga (criação síncrona dependente do catálogo) | Médio | Média | **Médio** | CT-PRF-001..008 com SLOs propostos |
| R-07 | Vulnerabilidades (injeção, XSS, exposição de dados sem autenticação) | Alto | Média | **Alto** | CT-SEC-001..016, DAST |
| R-08 | Contrato de erro inconsistente (5xx para erro do cliente, envelope ausente) | Médio | Alta | **Médio** | CT-TRV-*, casos de suposição de erro; suspeita H-02 |
| R-09 | Interface exibe valores divergentes da API ou quebra em outro idioma | Médio | Média | **Médio** | CT-WEB-010/030/050/052, CT-WEB-060..065 |
| R-10 | Documentação divergente do comportamento (base de teste pouco confiável) | Médio | Alta | **Médio** | Análise crítica da especificação (Q/H) antes da automação; suspeita H-05 |

## 4. Níveis de teste e pirâmide

O SUT já tem testes de unidade e de componente (com catálogo simulado). Este projeto **complementa** esses testes, sem duplicá-los. O foco está no que eles não cobrem: o sistema integrado de verdade, a perspectiva caixa-preta do contrato publicado, a interface, a performance e a segurança.

```
                ▲ menos testes, mais caros
   E2E web      │  48 casos (26%)   jornadas, apresentação, i18n, acessibilidade
   Perf / Sec   │  24 casos (13%)   SLOs, carga, OWASP, DAST
   Integração   │  15 casos ( 8%)   serviços reais + injeção de falha
   API          │  96 casos (53%)   regras, cálculo, validação, contrato
   ─────────────┼─────────────────  (fronteira deste projeto)
   Componente   │  SUT: OrderApiIT, CatalogApiIT, OrderPersistenceIT
   Unidade      │  SUT: 39 backend + 53 frontend
                ▼ mais testes, mais baratos
```

Regra de alocação: **cada regra é testada no nível mais baixo capaz de verificá-la.** Os cálculos e as validações ficam na API. A UI verifica que o valor exibido é o calculado pela API, sem recalcular todas as combinações. As bordas de vigência de preço no instante exato (`validFrom`/`validTo`) já são cobertas pelo teste unitário `PriceEntryTest` do SUT e, como dependem do relógio, não são repetidas aqui de forma instável.

## 5. Tipos de teste

| Tipo | Objetivo | Onde |
|---|---|---|
| Funcional | Regras de negócio e validações | API, WEB |
| Contrato | JSON Schema das respostas, envelope de erro, headers, *drift* do OpenAPI | API (CO) |
| Integração | Colaboração real Pedidos ↔ Catálogo, resiliência | API+FI, API+DB |
| E2E | Jornadas do usuário na interface | WEB |
| Regressão | Toda a suíte automatizada em cada PR (P1/P2) e na `develop` (P1–P3) | CI |
| Performance e carga | Latência, throughput, estabilidade, saturação | PERF |
| Segurança | OWASP API Top 10, XSS, cabeçalhos, superfície exposta, DAST, SCA | SEC, DAST |
| Internacionalização | Textos, formatos e persistência de idioma; paridade das traduções | WEB, ESTÁTICO |
| Acessibilidade | WCAG 2.1 A/AA (varredura automática e teclado) | WEB |
| Exploratório | Descobrir o que a especificação não previu (§13) | Sessões guiadas por *charter* |

## 6. Técnicas de projeto de teste

| Técnica | Aplicação neste sistema |
|---|---|
| Partição de equivalência | Situações do produto (ativo, inativo, preço vigente, futuro, expirado, inexistente); formatos de e-mail; idiomas |
| Análise de valor-limite | Quantidade (0/1/999/1000), itens (0/1/50/51/100/101), tamanhos de campo (120/121, 20/21, 50/51), payload (1.048.576/1.048.577 bytes), mínimo do desconto (999,80 × 1.149,80; fronteira exata via regra ajustada), página e tamanho de página |
| Tabela de decisão | Desconto (regra ativa? × subtotal ≥ mínimo? × múltiplas regras?); combinação de produtos inválidos (tipos iguais e diferentes); precedência repetido × inexistente |
| Caso de uso | Criar → detalhar → listar; jornadas de UI |
| Suposição de erro | Tipos inválidos, campos forjados, rotas e métodos inexistentes, *chunked encoding*, duplo clique |
| Perfil operacional | Mix de carga ponderado por uso esperado (§7) |

Exemplo de rigor no oráculo: o arredondamento HALF_UP não aparece com a regra padrão (10% sobre preços terminados em 0 centavos sempre dá valor exato). O caso CT-ORD-007 usa 5% sobre R$ 1.499,70 = 74,985. HALF_UP dá **74,99**, enquanto HALF_EVEN e truncamento dariam 74,98, de modo que só a política correta passa.

## 7. Performance

A documentação do SUT não define SLAs (Q-06). A estratégia propõe SLOs iniciais (NF-01/NF-02), deriva o modelo de carga do uso esperado das telas e trata os números como **premissas a homologar** com o Client Face.

| Operação | p95 proposto | Justificativa |
|---|---|---|
| `POST /orders` | < 800 ms (p99 < 1,5 s) | Inclui chamada síncrona ao catálogo (timeout de leitura de 3 s) e transação no banco |
| `GET /orders` (20 itens) | < 400 ms | 3 queries fixas (anti-N+1 comprovado pelo SUT) |
| `GET /orders/{id}` | < 300 ms | 1 query com `join fetch` |
| `GET /products/{id}` | < 200 ms | Leitura por chave no MongoDB |
| `POST /products/prices` | < 300 ms | 1 query `findByIdIn` |

Os perfis (smoke, carga, estresse, pico, resistência) e o mix estão em [performance.md](test-cases/performance.md). No CI, o **smoke** (CT-PRF-001) é gate em todo PR, e a **carga nominal** (CT-PRF-002) é gate na `develop` e no release. Estresse, pico e resistência são execuções sob demanda, com resultado informativo para o planejamento de capacidade.

## 8. Segurança

Abordagem em camadas, sem testes destrutivos:

1. **Testes dirigidos automatizados** (CT-SEC-001..014): injeção, XSS, *mass assignment*, limites, exposição e cabeçalhos, mapeados ao OWASP API Security Top 10 2023.
2. **DAST** com OWASP ZAP (CT-SEC-015): gate com alertas *High*.
3. **SCA** das imagens (CT-SEC-016): informativo, porque o SUT não pode ser alterado.

> **Decisão de escopo (2026-10-29):** o responsável pelo projeto retirou as camadas 2 e 3 desta entrega. O gate G6 executa só os testes dirigidos. DAST e SCA ficam como condição para produção no [resumo de qualidade](quality-summary.md).

A ausência de autenticação é **risco aceito no desafio e impeditivo para produção** (ADR-013 do SUT). Essa ressalva vai explícita no resumo de qualidade.

## 9. Dados de teste

| Princípio | Prática |
|---|---|
| Massa de referência imutável | A massa inicial (RN-22) é a base. `docker compose down -v` restaura o estado original, e o CI sempre começa de um ambiente limpo. |
| Isolamento por teste | Cada teste cria os próprios pedidos com um `runId` único no nome e no e-mail do cliente. As asserções nunca dependem de contagens absolutas do banco. |
| Mutação controlada | Casos que alteram estado global (regra de desconto no PostgreSQL, produtos no MongoDB) ficam em classes **serializadas**, restauram o estado no *teardown* (mesmo em falha) e verificam a restauração. |
| Dados sintéticos | Nenhum dado pessoal real. E-mails em `example.com` (RFC 2606) e o documento do exemplo oficial. |
| Massa gerada com apoio de IA | As fronteiras numéricas (ex.: 74,985) foram propostas pela IA e **recalculadas de forma independente** em aritmética inteira (centavos) antes de entrar nos casos (`docs/ai-usage.md`). |
| Volume | A massa de volume para performance (CT-PRF-008) é criada via API, por script versionado. |

## 10. Ambientes

| Ambiente | Composição | Uso |
|---|---|---|
| Local | `sut/docker-compose.yml` (PostgreSQL 16, MongoDB 7, serviços, front-end) | Desenvolvimento e depuração das suítes |
| CI | O mesmo compose no runner `ubuntu-latest` do GitHub Actions, construído do zero a cada execução | Quality gates |
| CI com injeção de falhas | *Override* do compose (`tests/infra/`), em que `CATALOG_BASE_URL` aponta para um proxy (WireMock) diante do catálogo real | CT-INT de resiliência, CT-TRV-001 (503/504), CT-PRF-007 |

O *override* usa apenas variáveis de ambiente já suportadas pelo SUT (`sut/README.md`). **Nenhum código do SUT é alterado** (verificado pelo gate G0).

## 11. Princípios de automação

1. **O esperado vem do caso de teste**, e o caso vem da RN (ADR-QA-005). Teste que falha por divergência vira defeito, e o teste não é ajustado para passar.
2. **ID do caso no nome do teste** (`CT-ORD-007 …`), o que permite gerar a matriz e o status de automação.
3. **Independência e idempotência:** qualquer teste roda sozinho, em qualquer ordem e repetidas vezes.
4. **Sem espera fixa** (`sleep`/`waitForTimeout`): só esperas por condição.
5. **Camadas:** cliente de API, construtores de dados (*builders*), Page Objects e asserções reutilizáveis. Nada de lógica de teste duplicada.
6. **Falha diagnosticável:** cada falha anexa requisição, resposta e `X-Request-Id` (API) ou trace, screenshot e vídeo (web).
7. **Código de teste é código de produção:** lint, formatação, revisão em PR e os mesmos critérios do guia de governança.

## 12. Shift-left e quality gates

**Shift-left aplicado:**

- **Revisão da especificação antes de testar:** a análise crítica da base de teste gerou 11 questões e 8 suspeitas antes da primeira linha de automação, e o smoke do ambiente revelou a 9ª (H-09).
- **Contrato verificado em todo PR**, por JSON Schema e *drift* do OpenAPI.
- **Smoke de performance em todo PR**, para pegar regressões cedo, sem esperar a fase de performance.
- **Rastreabilidade verificada por script** (`node scripts/traceability.mjs --check`), o que impede caso sem requisito ou requisito sem caso.

**Gates da esteira** (implementação na Parte 4). Todos são bloqueantes, salvo indicação:

| Gate | Verificação | PR → `develop` | `develop` / release |
|---|---|---|---|
| G0 | Integridade do SUT (`scripts/verify-sut-integrity.sh`) | ✔ | ✔ |
| G1 | Qualidade do código de teste (Spotless, ESLint, Prettier, `tsc`) + rastreabilidade (`--check`) | ✔ | ✔ |
| G2 | Ambiente sobe saudável (*healthchecks* dos 5 containers) | ✔ | ✔ |
| G3 | Suíte de API e integração (P1 + P2) | ✔ | ✔ (P1–P3) |
| G4 | Suíte web, Chromium (P1 + P2) | ✔ | ✔ (P1–P3; Firefox/WebKit informativo) |
| G5 | Performance: smoke (CT-PRF-001) / carga nominal (CT-PRF-002) | smoke | smoke + carga |
| G6 | Segurança: suíte SEC dirigida (API e interface). ZAP fora do escopo por decisão | ✔ | ✔ |
| G7 | **Decisão de deploy**: o job de deploy depende de G0–G6 (`needs`) e só segue sem defeito S1/S2 aberto, salvo exceção registrada | — | ✔ |

**Política de defeitos conhecidos.** Um teste que reproduz um defeito confirmado **continua executando e falhando visivelmente**. Ele recebe a tag `defeito-conhecido` e o link da Issue. O efeito no gate depende da severidade:

- **S1/S2:** bloqueiam o deploy. Só uma exceção formal, aprovada pelo Client Face e registrada na Issue, libera.
- **S3/S4:** não bloqueiam, mas aparecem no relatório e no resumo de qualidade.

Nenhum teste é desativado (`@Disabled`/`skip`) para "deixar a esteira verde".

**Política de instabilidade (flakiness).**

- Na suíte web do CI, há no máximo 1 nova tentativa por teste. Um teste que passa só na nova tentativa é marcado como **instável** no relatório.
- Taxa de instabilidade acima de 2% gera Issue de manutenção.
- Um teste instável vai para quarentena (executa, mas não bloqueia) por no máximo 1 ciclo, com dono definido.
- A suíte de API não usa nova tentativa.

## 13. Testes exploratórios

Sessões de 60 min guiadas por *charter*, com notas registradas e os achados convertidos em casos ou defeitos:

| Charter | Explorar | Com | Para descobrir |
|---|---|---|---|
| EX-01 | Formulário de novo pedido | Dados extremos, colar valores, edição rápida de quantidades, navegação no meio do fluxo | Inconsistências de estado e de subtotal exibido |
| EX-02 | Troca de idioma | Mudanças durante carregamentos, erros e diálogos abertos | Textos não traduzidos, formatos misturados |
| EX-03 | Catálogo instável | Latência e quedas intermitentes do catálogo durante a criação | Mensagens confusas, pedidos duplicados por nova tentativa do usuário |
| EX-04 | Contratos de API | Variações de payload não previstas (campos extras, Unicode, nulos aninhados) | Respostas fora do envelope ou 5xx |

## 14. Gestão de defeitos

**Severidade** (impacto no negócio, definida pelo QA) × **prioridade** (urgência de correção, acordada com o PO/Client Face):

| Severidade | Definição | Exemplo neste sistema |
|---|---|---|
| S1 — Crítica | Perda ou corrupção de dados, cálculo financeiro errado, indisponibilidade sem contorno | Desconto calculado errado; pedido persistido parcialmente |
| S2 — Alta | Funcionalidade principal falha ou contrato quebrado, com contorno difícil | Pedido válido de 60 itens recusado com 503 (H-01) |
| S3 — Média | Comportamento incorreto com contorno, ou em cenário de borda | 500 em vez de 400 para id malformado (H-02) |
| S4 — Baixa | Cosmético, texto, documentação | Exemplo da documentação divergente da massa (H-05) |

**Ciclo de vida:** Novo → Triado → Confirmado → Em correção → Pronto para reteste → Fechado. Desvios possíveis: Reaberto, Rejeitado, Duplicado e Não reproduzível (com justificativa).

**Registro:** Issue no GitHub pelo template de defeito (`.github/ISSUE_TEMPLATE/defect.yml`), com passos reproduzíveis, esperado com a fonte da documentação, obtido com `X-Request-Id`, evidência e caso de teste que reproduz. Labels: `defeito`, `S1`–`S4`, componente e `doc` quando for divergência de documentação.

## 15. Métricas e reporte

| Métrica | Fonte | Uso |
|---|---|---|
| Cobertura de requisitos | Matriz de rastreabilidade (gerada) | Completude da especificação × casos |
| Cobertura de automação | Matriz (IDs encontrados em `tests/`) | Progresso da automação |
| Taxa de aprovação por suíte e prioridade | Relatórios de execução (Allure, Playwright, k6) | Gates e resumo de qualidade |
| Defeitos por severidade, componente e camada de detecção | Issues | Onde estão os riscos e qual camada de teste é mais efetiva |
| Taxa de instabilidade | Relatórios do CI (aprovado só em nova tentativa / total) | Saúde das suítes |
| Duração da esteira de PR | GitHub Actions | Meta < 15 min (feedback rápido) |
| Latência p95/p99 e taxa de erro por operação | k6 | Tendência de performance entre versões |

**Reporte:**

- Relatório de execução por pipeline (artefato do CI).
- Resumo de qualidade por versão (Parte 5), com linguagem para público não técnico: recomendação de release, riscos e próximos passos.

## 16. IA no ciclo de testes

| Atividade | Uso da IA | Validação humana |
|---|---|---|
| Estratégia e riscos | Análise do enunciado e da documentação; proposta de riscos e abordagem | Priorização e escopo decididos pelo candidato |
| Especificação (SDD) | Consolidação das regras com fonte; leitura cruzada doc × código para Q/H | Cada RN conferida na fonte citada; H só vira defeito se reproduzida |
| Casos de teste | Geração por técnica (limites, tabelas de decisão) | Valores esperados recalculados de forma independente; revisão de cobertura pela matriz |
| Massa de dados | Cálculo de fronteiras (HALF_UP, limites de tamanho) | Recalculado em aritmética inteira; inválido se depender do comportamento observado |
| Automação | Geração de código a partir dos CTs | Revisão em PR, lint e execução. O teste precisa falhar quando o comportamento diverge (prova do oráculo). |
| Análise de falhas e defeitos | Triagem de logs por `requestId`, hipótese de causa raiz, redação da Issue | Reprodução manual ou automatizada antes do registro |

**Guardrails:** a IA nunca define o resultado esperado sozinha; nenhum dado sensível é enviado; todo uso fica registrado em [ai-usage.md](ai-usage.md) com prompt, decisão e validação.
