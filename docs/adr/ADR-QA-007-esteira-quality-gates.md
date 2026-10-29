# ADR-QA-007: Esteira de quality gates no GitHub Actions

**Status:** Aceita

## Contexto

O desafio exige uma esteira de CI/CD que execute as suítes automatizadas como **quality gate obrigatório antes de qualquer deploy** (PDF §2.6.2). A estratégia (§12) define os gates G0 a G7 e uma política de defeitos conhecidos. As restrições:

- O SUT não pode ser alterado. O CI original dele (`sut/.github/`) fica inativo (ADR-QA-001).
- O SUT tem **defeitos reais confirmados** (Issues #4 a #20). Uma esteira que só fica verde com os testes desses defeitos desativados esconderia o risco. Uma esteira permanentemente vermelha deixaria de ser útil como gate.
- Repositório privado no plano gratuito: sem proteção de branch nem *environments* com aprovação.

## Decisão

Workflow único (`.github/workflows/quality-gates.yml`), disparado em PR, em push para `develop`/`main` e sob demanda:

| Gate | Job | Bloqueia | Conteúdo |
|---|---|---|---|
| G0 | `g0-integrity` | Sim | Manifesto SHA-256 do SUT |
| G1 | `g1-static` | Sim | Rastreabilidade (`--check`), Spotless + compilação da API, `tsc` + ESLint + Prettier da web |
| G2 | Em cada job de suíte | Sim | Compose **do zero** (com o proxy de falhas), prontidão dos 5 componentes e aquecimento (`scripts/wait-for-sut.sh`) |
| G3 | `g3-api` | Sim | `mvn test -DexcludedGroups=defeito-conhecido` |
| G4 | `g4-web` | Sim | Playwright na **imagem oficial 1.48.2** (sem download de navegador: mitigação da ADR-QA-003), rede do host, `--grep-invert @defeito-conhecido` |
| G5 | `g5-performance` | Sim | k6 em container: smoke em todo PR; carga nominal em push para `develop`/`main`; qualquer cenário sob demanda |
| G6 | — | — | Segurança: entra com a suíte de segurança (Parte diferenciais) |
| G7 | `g7-deploy` | Sim | Depende de G0–G5 e consulta as Issues: **defeito S1/S2 aberto sem o label `excecao-aprovada` bloqueia o deploy** (simulado) |

**Defeitos conhecidos:** em G3 e G4, os testes marcados como defeito conhecido rodam num passo próprio, **não bloqueante**, com resultado no resumo do job e nos artefatos. O bloqueio por esses defeitos acontece no G7, **pela severidade da Issue**, e não pela falha do teste. Assim:

- um PR que não piora nada passa nos gates de suíte;
- o deploy fica bloqueado enquanto houver defeito S1/S2 sem exceção formal;
- nenhum teste é desativado.

**Evidências:** resumo em Markdown em cada job (`scripts/summarize-junit.mjs --markdown`, resumos do k6) e artefatos (Surefire, Allure, relatório HTML e traces do Playwright, resultados do k6, logs do SUT em caso de falha).

## Alternativas consideradas

- **Um job único sequencial:** ambiente construído uma vez só, mas sem paralelismo e com um gate "tudo ou nada", difícil de diagnosticar. Jobs separados por gate dão status independente no PR e paralelizam API, web e performance.
- **Desativar os testes de defeito conhecido (`@Disabled`/`skip`):** rejeitado pela estratégia. O defeito some do relatório.
- **Bloquear o PR pelos testes de defeito conhecido:** impediria qualquer evolução da própria suíte enquanto o SUT tiver defeitos que a equipe de QA não pode corrigir.

## Consequências

- **Positivas:**
  - O gate de deploy reflete o risco real (severidade) e é auditável (lista das Issues no resumo).
  - O ambiente sobe do zero em cada job, sem o IP obsoleto do nginx (H-13).
  - Os navegadores vêm da imagem oficial, sem download em tempo de execução.
- **Negativas:**
  - O SUT é construído em três jobs (API, web, performance), o que soma minutos de CI. O custo é aceito pelo paralelismo e pelo isolamento.
  - O runner compartilhado não é ambiente de performance: o G5 detecta regressão, não certifica capacidade (ADR-QA-004).
  - Sem proteção de branch no plano atual, a obrigatoriedade do gate vale pelo processo (CONTRIBUTING). Com o repositório público ou em plano pago, os checks G0–G5 devem virar *required status checks*.
