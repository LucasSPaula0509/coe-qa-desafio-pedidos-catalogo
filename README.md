# Projeto de Qualidade — Gestão de Pedidos e Catálogo

Desafio técnico **COE Leader — QA / Testes** (Stefanini Group): estratégia, automação (API, front-end, performance e segurança), quality gates em CI/CD e reporte de qualidade do sistema de Gestão de Pedidos e Catálogo, conduzidos com **Spec-Driven Development** e IA aplicada a todo o ciclo de testes.

> **Executive summary (EN)** — This repository contains the quality engineering project for the *Order & Catalog Management* system. The system under test (SUT) is kept byte-for-byte unchanged in `sut/` and verified by a SHA-256 manifest. Testing follows a spec-driven flow (business rules → test cases → automation → evidence) with full traceability. AI is used throughout the lifecycle, with every output validated by a human and logged in `docs/ai-usage.md`.

## Status da entrega

| Parte | Entregável | Status |
|---|---|---|
| Estrutura | Repositório, GitFlow, integridade do SUT, ADRs de stack | ✅ Concluída |
| 1 | Estratégia, plano, especificação (SDD), 179 casos de teste, rastreabilidade gerada e validada por script | ✅ Concluída |
| 2 | Automação de API (Java + RestAssured) | ⏳ Planejada |
| 3 | Automação de front-end (Playwright) | ⏳ Planejada |
| 4 | Performance (k6) e quality gates no CI/CD | ⏳ Planejada |
| 5 | Relatório de execução, Issues de defeito, resumo de qualidade | ⏳ Planejada |
| Diferenciais | IA aplicada, segurança, governança, dimensionamento | ⏳ Em andamento (log de IA ativo) |

## Estrutura do repositório

```
sut/                     Sistema sob teste — NÃO ALTERAR (ver ADR-QA-001)
  docs/                  Base de teste oficial (requisitos, contratos, dados, ADRs do SUT)
sut.sha256               Manifesto de integridade do SUT (219 arquivos)
scripts/                 verify-sut-integrity.sh, traceability.mjs
specs/                   Especificação SDD: regras testáveis (RN), questões e suspeitas (Q/H)
docs/
  test-strategy.md       Estratégia de testes
  test-plan.md           Plano de testes
  test-cases/            Casos de teste por área (CT-*)
  traceability-matrix.md Matriz RN ↔ CT ↔ automação
  adr/                   Decisões do projeto de qualidade (ADR-QA-*)
  ai-usage.md            Uso de IA: ferramenta, prompts, decisões e validações
tests/                   Suítes automatizadas (a partir da Parte 2)
.github/                 Templates de PR e de Issue de defeito (workflows na Parte 4)
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

Conforme as instruções do próprio SUT ([sut/README.md](sut/README.md)):

```bash
cd sut
docker compose up --build -d
```

| Componente | URL |
|---|---|
| Front-end | http://localhost:4200 |
| order-service | http://localhost:8080/api/v1/orders · Swagger: http://localhost:8080/swagger-ui.html |
| catalog-service | http://localhost:8081/api/v1/products · Swagger: http://localhost:8081/swagger-ui.html |

Para voltar ao estado inicial (massa de dados original), use `docker compose down -v`.

### 3. Validar a rastreabilidade

```bash
node scripts/traceability.mjs --check   # valida IDs e confere se a matriz está atualizada
node scripts/traceability.mjs           # regenera docs/traceability-matrix.md após alterar specs/ ou casos
```

### 4. Executar as suítes

Instruções adicionadas a cada parte entregue (Partes 2 a 4).

## Documentação

| Documento | Conteúdo |
|---|---|
| [Especificação (SDD)](specs/README.md) | Processo SDD, [regras testáveis](specs/business-rules.md) (34 RN + 4 NF) e [questões e suspeitas de defeito](specs/open-questions.md) |
| [Estratégia de testes](docs/test-strategy.md) | Resumo executivo, riscos, pirâmide, técnicas, dados, ambientes, quality gates, defeitos e métricas |
| [Plano de testes](docs/test-plan.md) | Escopo, critérios de entrada e saída, suspensão, cronograma, papéis, riscos do projeto |
| [Casos de teste](docs/test-cases/README.md) | 179 casos por área, com técnica, prioridade e nível de execução |
| [Matriz de rastreabilidade](docs/traceability-matrix.md) | Requisitos ↔ casos ↔ automação (gerada por `scripts/traceability.mjs`) |
| [Uso de IA](docs/ai-usage.md) | Ferramenta, motivo, prompts, decisões e validações por etapa |
| [ADRs do projeto de qualidade](docs/adr/) | Estrutura do repositório, stacks (API, web, performance) e SDD |
| [Guia de contribuição](CONTRIBUTING.md) | GitFlow, commits, PRs e registro de defeitos |

## Uso de IA

Ferramenta principal: **Claude Code** (modelo Claude Opus 5.5), agente de IA no VS Code. O motivo da escolha, o log por etapa e as validações estão em [docs/ai-usage.md](docs/ai-usage.md).

**Link da conversa com a IA:** `<INSERIR_LINK_DO_CHAT_AQUI>`
