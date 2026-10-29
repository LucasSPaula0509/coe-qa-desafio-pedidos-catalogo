# Projeto de Qualidade — Gestão de Pedidos e Catálogo

Desafio técnico **COE Leader — QA / Testes** (Stefanini Group): estratégia, automação (API, front-end, performance e segurança), quality gates em CI/CD e reporte de qualidade do sistema de Gestão de Pedidos e Catálogo, conduzidos com **Spec-Driven Development** e IA aplicada a todo o ciclo de testes.

> **Executive summary (EN)** — This repository contains the quality engineering project for the *Order & Catalog Management* system. The system under test (SUT) is kept byte-for-byte unchanged in `sut/` and verified by a SHA-256 manifest. Testing follows a spec-driven flow (business rules → test cases → automation → evidence) with full traceability. AI is used throughout the lifecycle, with every output validated by a human and logged in `docs/ai-usage.md`.

## Status da entrega

| Parte | Entregável | Status |
|---|---|---|
| Estrutura | Repositório, GitFlow, integridade do SUT, ADRs de stack | ✅ Concluída |
| 1 | Estratégia, plano, especificação (SDD), casos de teste, rastreabilidade gerada e validada por script | ✅ Concluída |
| 2 | Automação de API (Java + RestAssured): 111 casos, injeção de falhas, 9 defeitos confirmados | ✅ Concluída |
| 3 | Automação de front-end (Playwright): 48 casos, pt-BR/en/es, acessibilidade, 6 defeitos confirmados | ✅ Concluída |
| 4 | Performance (k6, 8 cenários) e esteira GitHub Actions com quality gates G0–G7 | ✅ Concluída |
| 5 | Relatório de execução (referência no CI), 15 defeitos como Issues, resumo de qualidade com recomendação de release | ✅ Concluída |
| Diferenciais | Segurança (14 casos OWASP, gate G6, 4 defeitos), governança de qualidade, dimensionamento, IA aplicada (log por etapa) | ✅ Concluída |

## Estrutura do repositório

```
sut/                     Sistema sob teste — NÃO ALTERAR (ver ADR-QA-001)
  docs/                  Base de teste oficial (requisitos, contratos, dados, ADRs do SUT)
sut.sha256               Manifesto de integridade do SUT (219 arquivos)
scripts/                 verify-sut-integrity.sh, traceability.mjs, summarize-junit.mjs, start-sut.sh, wait-for-sut.sh
specs/                   Especificação SDD: regras testáveis (RN), questões e suspeitas (Q/H)
docs/
  test-strategy.md       Estratégia de testes
  test-plan.md           Plano de testes
  test-cases/            Casos de teste por área (CT-*)
  traceability-matrix.md Matriz RN ↔ CT ↔ automação
  adr/                   Decisões do projeto de qualidade (ADR-QA-*)
  test-execution/        Registros de execução por suíte
  ai-usage.md            Uso de IA: ferramenta, prompts, decisões e validações
  quality-governance-guide.md  Guia de governança de qualidade para squads
  proposal-sizing.md     Dimensionamento da proposta (3 semanas, QA sênior + pleno)
tests/
  api/                   Suíte de API (Java 17, JUnit 5, RestAssured) — ver tests/api/README.md
  web/                   Suíte E2E (Playwright + TypeScript, axe-core) — ver tests/web/README.md
  performance/           Cenários k6 (smoke, carga, estresse, pico, resistência...) — ver tests/performance/README.md
  infra/                 Override do Compose com o proxy de injeção de falhas
.github/                 Esteira de quality gates (workflows/quality-gates.yml) e templates de PR e de defeito
```

## Pré-requisitos

| Ferramenta | Versão | Uso |
|---|---|---|
| Git + Bash | Git 2.40+ (no Windows, Git Bash) | Repositório e scripts |
| Docker + Docker Compose v2 | Docker 24+ | Subir o SUT (PostgreSQL, MongoDB, serviços, front-end) |
| Java + Maven | 17 / 3.9 | Suíte de API (Parte 2) |
| Node.js | 20.19+ ou 22 LTS | Suíte web (Parte 3) |
| k6 | 0.5x+ (ou imagem `grafana/k6`) | Suíte de performance (Parte 4) |

## Como executar

### 1. Verificar a integridade do SUT

```bash
bash scripts/verify-sut-integrity.sh
# OK: SUT íntegro — 219 arquivos conferem com sut.sha256.
```

### 2. Subir o sistema sob teste

Conforme as instruções do próprio SUT ([sut/README.md](sut/README.md)), com o *override* de teste que insere o proxy de injeção de falhas entre os serviços (só variável de ambiente, ver [ADR-QA-006](docs/adr/ADR-QA-006-injecao-de-falhas-e-dados.md)):

```bash
docker compose -f sut/docker-compose.yml -f tests/infra/docker-compose.qa.yml up --build -d
```

Sem o *override* (`cd sut && docker compose up --build -d`), o sistema roda exatamente como entregue. Nesse modo, os casos com a tag `fault` não se aplicam.

| Componente | URL |
|---|---|
| Front-end | http://localhost:4200 |
| order-service | http://localhost:8080/api/v1/orders · Swagger: http://localhost:8080/swagger-ui.html |
| catalog-service | http://localhost:8081/api/v1/products · Swagger: http://localhost:8081/swagger-ui.html |

| Proxy de falhas (admin) | http://localhost:8089/__admin |

Para voltar ao estado inicial (massa de dados original), use `docker compose -f sut/docker-compose.yml -f tests/infra/docker-compose.qa.yml down -v`.

### 3. Validar a rastreabilidade

```bash
node scripts/traceability.mjs --check   # valida IDs e confere se a matriz está atualizada
node scripts/traceability.mjs           # regenera docs/traceability-matrix.md após alterar specs/ ou casos
```

### 4. Executar as suítes

**API** (detalhes, tags e variáveis em [tests/api/README.md](tests/api/README.md)):

```bash
cd tests/api
mvn test                                      # completa: 190 execuções; defeitos conhecidos falham de propósito
mvn test -DexcludedGroups=defeito-conhecido   # visão do gate: 100% deve passar
node ../../scripts/summarize-junit.mjs target/surefire-reports --failures
```

**Web** (detalhes em [tests/web/README.md](tests/web/README.md); suba o ambiente do zero — ver H-13):

```bash
cd tests/web
npm ci && npx playwright install chromium
npm test                 # completa: 51 execuções; defeitos conhecidos falham de propósito
npm run test:gate        # visão do gate: 100% deve passar
npm run report           # relatório HTML com trace, screenshot e vídeo das falhas
```

**Performance** (detalhes em [tests/performance/README.md](tests/performance/README.md)):

```bash
bash scripts/wait-for-sut.sh                 # prontidão e aquecimento do ambiente
bash tests/performance/run.sh smoke          # gate de PR; saída ≠ 0 = threshold violado
bash tests/performance/run.sh load           # carga nominal (gate na develop/main)
```

### 5. Esteira de CI/CD (quality gates)

[`.github/workflows/quality-gates.yml`](.github/workflows/quality-gates.yml) roda em todo PR, em push para `develop`/`main` e sob demanda ([ADR-QA-007](docs/adr/ADR-QA-007-esteira-quality-gates.md)):

| Gate | Verificação |
|---|---|
| G0 | Integridade do SUT |
| G1 | Rastreabilidade + qualidade do código de teste (Spotless, tsc, ESLint, Prettier) |
| G2 | Ambiente do zero, saudável e aquecido (em cada job de suíte) |
| G3 / G4 | API e web sem defeitos conhecidos (bloqueantes); defeitos conhecidos em passo não bloqueante, no resumo |
| G5 | k6: smoke em todo PR; carga nominal na `develop`/`main` |
| G6 | Segurança: testes dirigidos da API e da interface (OWASP) |
| G7 | Deploy (simulado) só com G0–G6 aprovados e **sem defeito S1/S2 aberto** sem o label `excecao-aprovada` |

## Documentação

| Documento | Conteúdo |
|---|---|
| [Especificação (SDD)](specs/README.md) | Processo SDD, [regras testáveis](specs/business-rules.md) (34 RN + 4 NF) e [questões e suspeitas de defeito](specs/open-questions.md) |
| [Estratégia de testes](docs/test-strategy.md) | Resumo executivo, riscos, pirâmide, técnicas, dados, ambientes, quality gates, defeitos e métricas |
| [Plano de testes](docs/test-plan.md) | Escopo, critérios de entrada e saída, suspensão, cronograma, papéis, riscos do projeto |
| [Casos de teste](docs/test-cases/README.md) | 183 casos por área, com técnica, prioridade e nível de execução |
| [Matriz de rastreabilidade](docs/traceability-matrix.md) | Requisitos ↔ casos ↔ automação (gerada por `scripts/traceability.mjs`) |
| [**Resumo de qualidade da versão**](docs/quality-summary.md) | Recomendação de release, defeitos em linguagem de negócio, riscos, indicadores e evolução |
| [**Relatório de execução**](docs/test-execution/README.md) | Execução de referência no CI (gates, suítes, desempenho, decisão de deploy) e histórico |
| [Execução da suíte de API](docs/test-execution/api-2026-10-29.md) | Resultado, estabilidade (3 execuções idênticas) e defeitos confirmados |
| [Execução da suíte web](docs/test-execution/web-2026-10-29.md) | Resultado, estabilidade, prova de oráculo e defeitos da interface |
| [Execução de performance](docs/test-execution/performance-2026-10-29.md) | Smoke aprovado e validação dos 8 cenários k6 |
| [Execução de segurança](docs/test-execution/security-2026-10-29.md) | Proteções verificadas (OWASP) e defeitos de segurança |
| [**Guia de governança de qualidade**](docs/quality-governance-guide.md) | Papéis, definição de pronto, critérios que bloqueiam o PR, severidade e métricas para squads |
| [**Dimensionamento da proposta**](docs/proposal-sizing.md) | Partes 1–5 em 3 semanas com QA sênior + pleno: esforço, senioridade, premissas e riscos |
| [Uso de IA](docs/ai-usage.md) | Ferramenta, motivo, prompts, decisões e validações por etapa |
| [ADRs do projeto de qualidade](docs/adr/) | Estrutura do repositório, stacks (API, web, performance), SDD, injeção de falhas e dados, esteira de quality gates |
| [Guia de contribuição](CONTRIBUTING.md) | GitFlow, commits, PRs e registro de defeitos |

## Uso de IA

Ferramenta principal: **Claude Code** (modelo Claude Opus 5.5), agente de IA no VS Code. O motivo da escolha, o log por etapa e as validações estão em [docs/ai-usage.md](docs/ai-usage.md).

**Link da conversa com a IA:** `<INSERIR_LINK_DO_CHAT_AQUI>`
