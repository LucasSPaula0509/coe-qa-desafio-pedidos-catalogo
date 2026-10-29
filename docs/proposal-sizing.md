# Dimensionamento da Proposta — Qualidade

Cenário: entrega das **Partes 1 a 5** do desafio em **3 semanas**, com **1 QA sênior + 1 QA pleno**. As partes são estratégia, plano e casos; automação de API; automação web; performance e CI/CD; execução e reporte. O escopo é o produzido neste repositório: 183 casos, 3 suítes automatizadas, esteira com gates e relatório com recomendação de release.

## Premissas

- O SUT é entregue funcional em Docker Compose, com documentação (requisitos, API, banco, arquitetura) e código **congelado**. Defeitos viram Issue e não são corrigidos pelo time de QA.
- O Client Face responde às questões de especificação (Q-xx) em até 1 dia útil. Sem resposta, vale a interpretação documentada (`specs/open-questions.md`) e o risco é registrado.
- Ferramentas-padrão do COE (ADR-QA-002..004): Java/RestAssured, Playwright, k6 e GitHub Actions. Não há licenças a contratar.
- Ferramenta de IA disponível para os dois QAs, com o ganho já descontado na estimativa. Toda saída da IA é validada e registrada (`docs/ai-usage.md`).
- Time dedicado, 8 h por dia útil, sem feriados no período. Cerimônias limitadas a 30 min/dia.
- Fora do escopo: diferenciais (segurança, governança, dimensionamento e apresentação), execução em múltiplos navegadores como gate e ambiente de homologação gerenciado.

## Composição e senioridade

| Papel | Por que essa senioridade | Atribuições |
|---|---|---|
| QA sênior (líder técnico) | Decisões que custam caro se erradas: o que testar, com qual oráculo, como isolar falhas, o que bloqueia o release | Análise crítica da especificação (SDD), estratégia, plano, ADRs, arquitetura das suítes, injeção de falhas, performance, esteira, revisão de todo PR, severidade dos defeitos, resumo de qualidade |
| QA pleno | Volume de automação com padrão já definido, sob revisão do sênior | Casos de teste, automação de API e web, Page Objects, estabilização, execução, análise de falhas, registro de defeitos, relatório de execução |

## Plano por semana

### Semana 1 — Especificação e fundações (Parte 1; estimativa: 80 h)

| Atividade | Responsável | Esforço |
|---|---|---|
| Imersão no SUT e análise crítica da documentação (questões e suspeitas) | Sênior 8 h + pleno 4 h | 12 h |
| Especificação SDD (RN/NF com IDs) e script de rastreabilidade | Sênior | 6 h |
| Estratégia de testes, análise de risco e ADRs de ferramenta | Sênior | 8 h |
| Plano de testes (escopo, critérios de entrada e saída, cronograma) | Sênior | 4 h |
| Casos de teste por técnica (fronteira, partição, tabela de decisão) | Sênior 8 h + pleno 14 h | 22 h |
| Repositório: GitFlow, templates, integridade do SUT | Pleno | 6 h |
| Ambiente de teste (Compose + proxy de injeção de falhas) | Sênior 4 h + pleno 4 h | 8 h |
| Base da suíte de API (cliente, *builders*, schemas) | Pleno | 6 h |
| Buffer de contingência (10%) | — | 8 h |

### Semana 2 — Automação funcional (Partes 2 e 3; estimativa: 80 h)

| Atividade | Responsável | Esforço |
|---|---|---|
| Automação de API (pedidos, catálogo, transversais, contrato) | Pleno | 20 h |
| Integração e resiliência (timeout, 5xx, retry) por injeção de falhas | Sênior | 10 h |
| Execução da API, análise de causa raiz e registro de defeitos | Sênior | 6 h |
| Base da suíte web (Page Objects, i18n a partir das traduções do SUT, oráculos `Intl`) | Sênior | 6 h |
| Automação web (fluxos, validações, idiomas, acessibilidade) | Pleno | 18 h |
| Revisão técnica e prova de oráculo (mutação) das suítes | Sênior | 10 h |
| Buffer de contingência (12,5%) | — | 10 h |

### Semana 3 — Não funcionais, esteira e reporte (Partes 4 e 5; estimativa: 80 h)

| Atividade | Responsável | Esforço |
|---|---|---|
| Cenários k6 (smoke, carga, estresse, pico, resistência) com thresholds do SLO | Sênior | 10 h |
| Esteira com gates G0–G7 e bloqueio de deploy por severidade | Sênior 8 h + pleno 6 h | 14 h |
| Estabilização (5 execuções seguidas sem instabilidade) | Pleno | 8 h |
| Execução de referência no CI, análise e Issues (web e performance) | Sênior 4 h + pleno 6 h | 10 h |
| Correções de automação após revisão e reexecução | Pleno | 8 h |
| Relatório de execução consolidado | Pleno | 6 h |
| Resumo de qualidade e recomendação de release (público não técnico) | Sênior | 4 h |
| Evidências de uso de IA (prompts, validações, correções) | Sênior 2 h + pleno 2 h | 4 h |
| Revisão final, *handover* e alinhamento com o Client Face | Sênior | 6 h |
| Buffer de contingência (12,5%) | — | 10 h |

**Total estimado: 240 h (3 semanas × 2 pessoas × 40 h)**, com 28 h de buffer (~12%). Por pessoa e semana: sênior com 38/32/34 h, pleno com 34/38/36 h. A capacidade nominal comporta o plano, sem hora extra.

| Parte | Sênior | Pleno | Total |
|---|---|---|---|
| 1. Estratégia, plano e casos (inclui o ambiente de teste) | 38 h | 28 h | 66 h |
| 2. API (inclui a base da suíte) | 21 h | 26 h | 47 h |
| 3. Web | 11 h | 18 h | 29 h |
| 4. Performance e CI/CD (inclui a estabilização) | 18 h | 14 h | 32 h |
| 5. Execução e reporte | 16 h | 22 h | 38 h |
| Buffer | — | — | 28 h |
| **Total** | **104 h** | **108 h** | **240 h** |

A revisão técnica da semana 2 (10 h do sênior) foi dividida igualmente entre API e web.

## Itens adicionais (fora das 240 h)

| Item | Esforço | Responsável |
|---|---|---|
| Testes de segurança dirigidos (OWASP) e gate próprio | 16 h | Sênior |
| DAST (ZAP) e SCA (imagens) com triagem dos achados | 12 h | Sênior + pleno |
| Guia de governança e dimensionamento | 8 h | Sênior |
| Apresentação executiva | 4 h | Sênior |

## Riscos e mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Volume de defeitos maior que o previsto, consumindo triagem e registro | Alta | Médio | Suspeitas levantadas na semana 1 (shift-left); template de Issue; política de defeito conhecido (o teste segue executando, sem retrabalho) |
| Instabilidade no E2E | Média | Alto | Esperas por condição, 1 worker, dados isolados por execução; quarentena com dono |
| Falha externa na esteira (registro de pacotes, runner) | Média | Médio | Novas tentativas na build com espera crescente; diagnóstico pelo log antes de reexecutar |
| Respostas do Client Face atrasadas | Média | Médio | Interpretação documentada e reversível; impacto marcado nos casos afetados |
| Pressão por escopo (múltiplos navegadores, segurança completa) | Alta | Médio | Itens adicionais estimados à parte; mudança só com estimativa de impacto |
| Ambiente local indisponível (Docker/WSL) | Média | Baixo | CI como ambiente de referência desde a semana 1 |

## Comunicação com stakeholders

Status semanal de 1 página: o que foi entregue, defeitos por severidade, riscos e decisões, com links para os ADRs e as Issues. A linguagem é de resultado de negócio ("a criação de pedidos está validada; há 1 defeito que impede a liberação"). O detalhe técnico fica nos relatórios de execução.
