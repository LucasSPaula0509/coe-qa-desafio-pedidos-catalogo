# Plano de Testes — Gestão de Pedidos e Catálogo

Estrutura baseada na ISO/IEC/IEEE 29119-3 (plano de teste). A abordagem técnica (riscos, níveis, técnicas, gates) está na [estratégia](test-strategy.md), e este plano define **o que, quando, com quem e com quais critérios**.

## 1. Identificação

| Item | Valor |
|---|---|
| Projeto | Projeto de Qualidade — Gestão de Pedidos e Catálogo (desafio COE Leader QA / Testes) |
| Versão do plano | 1.0 |
| Item sob teste | SUT importado no commit `75f5172` (`main`); integridade: manifesto `sut.sha256` (SHA-256 `d4adae3e3bb9…`, 219 arquivos) |
| Componentes | `order-service` (Java 17, Spring Boot 3.3.4, PostgreSQL 16) · `catalog-service` (Java 17, Spring Boot 3.3.4, MongoDB 7) · `frontend` (Angular 21, nginx) |
| Base de teste | `sut/docs/` + PDF §2.2 → [specs/business-rules.md](../specs/business-rules.md) (34 RN + 4 NF) |

## 2. Escopo

### 2.1 Funcionalidades a testar

| Funcionalidade | Regras | Casos |
|---|---|---|
| Criação de pedido: entrada, cálculo, desconto | RN-01..06, RN-11..15 | [api-orders.md](test-cases/api-orders.md) A, B |
| Validação de produtos no catálogo (422) | RN-07, RN-08 | [api-orders.md](test-cases/api-orders.md) C |
| Consulta e listagem de pedidos | RN-16..18 | [api-orders.md](test-cases/api-orders.md) D, E |
| Catálogo: consulta individual e em lote | RN-19..22 | [api-catalog.md](test-cases/api-catalog.md) |
| Integração, resiliência e snapshot | RN-09..11 | [integration.md](test-cases/integration.md) |
| Envelope de erro, `X-Request-Id`, semântica HTTP, saúde | RN-23..27 | [api-cross-cutting.md](test-cases/api-cross-cutting.md) |
| Interface web: Painel, Catálogo, Novo pedido, Pedidos, Idiomas, acessibilidade | RN-28..34, NF-04 | [web-e2e.md](test-cases/web-e2e.md) |
| Performance e carga | NF-01, NF-02 | [performance.md](test-cases/performance.md) |
| Segurança | NF-03 | [security.md](test-cases/security.md) |

Total: **179 casos** (81 P1, 72 P2, 26 P3). A distribuição detalhada está na [matriz de rastreabilidade](traceability-matrix.md).

### 2.2 Funcionalidades fora do escopo

Ver [estratégia §2](test-strategy.md#2-escopo): testes unitários e de componente (já existem no SUT), autenticação (inexistente, ADR-013), ciclo de vida além de `CRIADO`, cadastro de produtos e certificação de capacidade em ambiente de produção.

## 3. Abordagem (resumo)

| Suíte | Ferramenta | Casos | Execução |
|---|---|---|---|
| API + integração + transversais | Java 17, JUnit 5, RestAssured (ADR-QA-002) | 110 | Todo PR (P1/P2); `develop` (P1–P3) |
| Web E2E | Playwright (ADR-QA-003) | 45 | Todo PR (P1/P2, Chromium); `develop` (P1–P3, 3 navegadores) |
| Performance | k6 (ADR-QA-004) | 8 | Smoke em todo PR; carga na `develop`/release; demais sob demanda |
| Segurança | RestAssured/Playwright + OWASP ZAP + Trivy | 16 | Todo PR (SEC + ZAP baseline); SCA na `develop` |

## 4. Critérios de entrada

| Critério | Verificação |
|---|---|
| SUT íntegro | `scripts/verify-sut-integrity.sh` OK |
| Ambiente saudável | `docker compose up` com os 5 containers no ar; `/actuator/health` = `UP` nos dois serviços; front-end responde em `:4200` |
| Massa inicial carregada | `POST /products/prices` com PROD-001..006 retorna os 6 produtos conforme a RN-22; regra de desconto 1000,00/10% ativa |
| Especificação revisada | RN e Q revisadas; matriz consistente (`node scripts/traceability.mjs --check`) |
| Smoke funcional OK | CT-ORD-002, CT-CAT-001 e CT-WEB-020 aprovados. Se o smoke falhar, a execução é **suspensa** (§6) |

## 5. Critérios de saída

| Critério | Meta |
|---|---|
| Execução | 100% dos casos P1 e P2 executados; P3 executados ou com justificativa registrada |
| Aprovação | 100% dos P1 aprovados, **ou** falhando apenas por defeito registrado (tag `defeito-conhecido`) |
| Defeitos | Nenhum defeito S1/S2 aberto sem exceção formal aprovada pelo Client Face |
| Performance | Smoke e carga nominal dentro dos thresholds do NF-01 (ou desvio registrado e aceito) |
| Segurança | Nenhum alerta *High* do ZAP sem tratamento; achados registrados |
| Confiabilidade | Taxa de instabilidade < 2% nas últimas 5 execuções do CI |
| Reporte | Relatório de execução publicado, Issues registradas e resumo de qualidade entregue |

## 6. Suspensão e retomada

| Suspender quando | Retomar quando |
|---|---|
| O smoke funcional falha (ambiente ou build quebrado) | O ambiente for recriado (`down -v` + `up`) e o smoke passar |
| Defeito S1 bloqueia mais de 30% dos casos de uma suíte | Houver contorno ou correção; casos bloqueados marcados como "bloqueado", nunca como "aprovado" |
| O SUT recebido muda (manifesto diverge) | O novo SUT for importado em commit dedicado, com manifesto regenerado e análise de impacto nas RN |

## 7. Entregáveis

| Entregável | Local | Parte |
|---|---|---|
| Especificação (RN, Q, H) | `specs/` | 1 |
| Estratégia, plano, casos, matriz | `docs/` | 1 |
| Suítes automatizadas | `tests/api`, `tests/web`, `tests/performance`, `tests/security` | 2, 3, 4, Dif. |
| Esteira com quality gates | `.github/workflows/` | 4 |
| Relatório de execução | Artefatos do CI + `docs/` | 5 |
| Defeitos | Issues do GitHub | 5 |
| Resumo de qualidade | `docs/quality-summary.md` | 5 |
| Uso de IA | `docs/ai-usage.md` + link da conversa | Todas |
| Guia de governança e dimensionamento | `docs/` | Dif. |

## 8. Ambiente e ferramentas

| Recurso | Especificação |
|---|---|
| SUT | `sut/docker-compose.yml`; proxy de falhas via *override* em `tests/infra/` |
| Máquina local | Docker Desktop (Compose v2), Java 17, Maven 3.9, Node.js 20.19+/22, k6 |
| CI | GitHub Actions, `ubuntu-latest`, com Docker nativo |
| Relatórios | Allure (API), Playwright HTML report (web), k6 summary JSON/HTML (performance), ZAP HTML report |
| Gestão | Repositório GitHub (GitFlow, PRs), Issues com template de defeito |

## 9. Cronograma

Sequência por fase. Cada fase termina com PR revisado para `develop` e só começa depois que o critério de pronto da anterior é atingido. O dimensionamento em horas (cenário de 3 semanas com QA sênior + pleno) é o artefato da Parte diferenciais.

| Fase | Atividades | Pronto quando |
|---|---|---|
| 0. Estrutura | Repositório, GitFlow, integridade do SUT, ADRs de stack | ✅ PR `feature/estrutura-repositorio` |
| 1. Estratégia e planejamento | Especificação SDD, estratégia, plano, casos, matriz | ✅ PR `feature/parte-1-estrategia-planejamento` |
| 2. API | Projeto Maven, cliente/builders, schemas, CT-ORD/CAT/TRV/INT (incluindo injeção de falhas) | Casos P1/P2 automatizados; H-01..H-07 confirmadas ou descartadas |
| 3. Web | Projeto Playwright, helper de i18n, Page Objects, CT-WEB | Casos P1/P2 automatizados, sem instabilidade em 5 execuções seguidas |
| 4. Performance e CI/CD | Scripts k6 e workflow com gates G0–G7 | Esteira verde ou vermelha **só** por defeito conhecido; deploy condicionado |
| 5. Execução e reporte | Execução completa, Issues, relatório, resumo de qualidade | Critérios de saída (§5) avaliados e documentados |
| Dif. | Segurança (CT-SEC + ZAP), guia de governança, dimensionamento, apresentação | Artefatos revisados e publicados |

## 10. Papéis e responsabilidades

Papéis do cenário de referência (equipe de QA sênior + pleno, em parceria com o Client Face). Na execução deste desafio, o candidato acumula os papéis de QA, com a IA como apoio em todas as fases.

| Papel | Responsabilidades |
|---|---|
| QA Sênior (líder técnico) | Estratégia, análise de risco, especificação SDD, arquitetura das suítes, injeção de falhas, performance, segurança, revisão de todo PR, decisão de severidade e recomendação de release |
| QA Pleno | Automação de API e web a partir dos casos, manutenção da massa de dados, execução e triagem inicial de falhas, registro de defeitos |
| Client Face | Homologar premissas (SLOs, Q-xx), negociar prioridades de correção e aprovar exceções de release |
| Squad de desenvolvimento | Esclarecer comportamento, corrigir defeitos e revisar testes que tocam o contrato |

## 11. Riscos do projeto

| ID | Risco | Probab. | Impacto | Mitigação | Contingência |
|---|---|---|---|---|---|
| RP-01 | **Docker indisponível na estação local** (verificado na preparação: não instalado) | Alta | Alto | Instalar o Docker Desktop antes da Fase 2; CI como ambiente de referência desde o primeiro PR | Executar as suítes só no CI e depurar pelos artefatos (logs, traces) |
| RP-02 | Ausência de SLA documentado | Certa | Médio | SLOs propostos (NF-01) e explicitados como premissa | Reportar a performance como linha de base comparativa, sem aprovação ou reprovação absoluta |
| RP-03 | Runner de CI compartilhado gera variação de latência | Alta | Médio | Thresholds de smoke conservadores; carga nominal na `develop` com histórico | Repetir a execução para confirmar antes de atribuir regressão |
| RP-04 | Testes que mutam estado global interferem entre si | Média | Alto | Classes serializadas, *teardown* com verificação, ambiente limpo por execução no CI | `docker compose down -v` e nova execução isolada |
| RP-05 | Instabilidade da UI (SPA zoneless, toasts assíncronos) | Média | Médio | Esperas por estado, sem `sleep`, localizadores semânticos | Quarentena com dono e prazo (estratégia §12) |
| RP-06 | Esteira permanentemente vermelha por defeitos reais do SUT | Alta | Médio | Política de defeitos conhecidos (estratégia §12): visibilidade sem esconder testes | Exceção formal registrada pelo Client Face |
| RP-07 | Data fixa da massa (PROD-005 passa a ter preço em 2030-01-01) | Baixa (no prazo) | Médio | Risco temporal documentado em `api-catalog.md`; o teste falha com mensagem explícita | Revisar a massa e os casos afetados |
| RP-08 | Interpretações (Q-xx) não confirmadas pelos stakeholders | Média | Médio | Interpretação explícita por Q; casos afetados listados | Reclassificar casos e defeitos quando houver decisão |

## 12. Comunicação

| Comunicação | Público | Frequência | Conteúdo |
|---|---|---|---|
| Status de qualidade | Client Face e stakeholders | Semanal (1 página) | Progresso por fase, defeitos por severidade, riscos e decisões pendentes (Q-xx), em linguagem de negócio |
| Relatório de execução | Squad | A cada pipeline | Resultados por suíte, instabilidade, links de evidência |
| Alerta de defeito S1/S2 | Client Face e squad | Imediato | Issue com impacto de negócio e recomendação |
| Resumo de qualidade da versão | Stakeholders | Por release | Recomendação de release, riscos residuais e evolução sugerida |

## 13. Aprovação

| Papel | Nome | Data | Situação |
|---|---|---|---|
| QA Sênior (autor) | Lucas de Souza Paula | 2026-10-29 | Elaborado |
| Client Face | — | — | Pendente (homologação das Q-xx e do NF-01) |
