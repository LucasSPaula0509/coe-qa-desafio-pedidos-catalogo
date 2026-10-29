# Suíte de Testes da Interface Web (E2E)

Automação dos casos `CT-WEB` ([docs/test-cases/web-e2e.md](../../docs/test-cases/web-e2e.md)): **48 casos, 53 execuções** (casos de idioma rodam por idioma). Stack: Playwright 1.48.2 + TypeScript, axe-core para acessibilidade ([ADR-QA-003](../../docs/adr/ADR-QA-003-stack-testes-web.md)).

## Pré-requisitos

- Node.js 20 ou superior
- Ambiente do SUT no ar com o *override* de teste, **subido do zero**, para que o nginx do front-end resolva os serviços atuais (ver H-13):

```bash
# na raiz do repositório
docker compose -f sut/docker-compose.yml -f tests/infra/docker-compose.qa.yml up --build -d
```

- Dependências e navegador (rede confiável; ver o risco aceito na ADR-QA-003):

```bash
cd tests/web
npm ci
npx playwright install chromium            # gate
npx playwright install firefox webkit      # opcional: job estendido
```

| Variável | Padrão | Uso |
|---|---|---|
| `WEB_BASE_URL` | `http://localhost:4200` | Front-end |
| `ORDER_API_URL` | `http://localhost:8080` | Preparação de dados e oráculo (pedidos) |
| `CATALOG_API_URL` | `http://localhost:8081` | Oráculo (catálogo) |
| `SUT_NETWORK`, `ORDER_SERVICE_CONTAINER`, `FRONTEND_CONTAINER` | `sut_default`, `sut-order-service-1`, `sut-frontend-1` | CT-WEB-056 (*redeploy*) |

## Execução

```bash
npm test                     # suíte completa no Chromium (navegador do gate)
npm run test:all-browsers    # Chromium, Firefox e WebKit (job estendido, informativo)
npm run test:gate            # visão do gate: chromium, sem @defeito-conhecido — tudo deve passar
npm run test:p1              # somente P1
npx playwright test --grep "CT-WEB-030"       # um caso
npm run report               # abre o relatório HTML (trace, screenshot e vídeo das falhas)
npm run verify               # tsc + ESLint + Prettier (qualidade do código de teste)
```

### Tags

| Tag | Significado |
|---|---|
| `@P1`, `@P2`, `@P3` | Prioridade por risco |
| `@defeito-conhecido` | Reproduz um defeito registrado: `knownDefect('<nº da Issue>', 'H-xx', …)`. Continua executando e falhando (estratégia §12) |
| `@infra` | Manipula containers (CT-WEB-056). Restaura o ambiente no `finally` |

## Estrutura

```
playwright.config.ts   1 worker (oráculos dependem do estado global de pedidos), fuso/locale fixos,
                       trace/vídeo/screenshot só em falha, nova tentativa só no CI, relatórios HTML + JUnit
pages/                 Page Objects: shell (menu, idioma, toasts, erro), painel, catálogo, novo pedido,
                       listagem, detalhe — localizadores semânticos com textos das traduções do SUT
support/
  i18n.ts              t(chave, idioma): lê sut/frontend/public/assets/i18n/*.json (oráculo de textos)
  format.ts            Oráculos de moeda e data com o Intl do próprio navegador; soma em centavos
  assertions.ts        expectCurrency (espera por estado com expect.poll)
  api.ts               Preparação de dados e oráculo via API (pedido único por teste)
  fixtures.ts          Fixtures de páginas e API; captura de requisições; envelope de erro para mocks
  known-defect.ts      Tag + anotações (Issue e suspeita) para defeitos conhecidos
  docker.ts            Controle do ambiente para o caso de redeploy
specs/                 Um arquivo por área de docs/test-cases/web-e2e.md
```

## Convenções

- **Sem `data-testid` no SUT** (que não pode ser alterado): localizadores por papel, rótulo e nome acessível, com textos tirados das traduções. Uma mudança de texto não quebra o teste, e o mesmo teste roda em pt-BR, en e es.
- **A UI verifica apresentação e fluxo.** Os valores esperados vêm da API ou da massa inicial, nunca recalculados por regra de negócio duplicada (as regras são cobertas na suíte de API).
- **Estados difíceis** (lista vazia, 503, 422) são simulados com `page.route` e envelope de erro do SUT (casos WEB+MOCK).
- **Sem espera fixa:** o ESLint proíbe `waitForTimeout` e `force`. Só asserções web-first e `expect.poll`.
- **Prova de oráculo:** a mutação de 1 centavo na resposta da API e a troca de idioma não aplicada fazem as asserções reprovarem (verificado na construção da suíte).

## Resultado de referência (2026-10-29, ambiente local, Chromium)

| Execução | Execuções | Aprovadas | Falhas | Duração |
|---|---|---|---|---|
| Completa (3 execuções seguidas) | 51 | 46 | 5, **todas** `@defeito-conhecido`, conjunto idêntico nas 3 | ~3 min |

Atualização (Parte 4): o CT-WEB-057 (acesso por nome de host via HTTP, H-18 / #20) foi adicionado ao executar a suíte em container. Ele é um `@defeito-conhecido`. Depois da primeira execução no CI, a H-15 passou a ser reproduzida pelo **CT-WEB-058** (com volume controlado), e o CT-WEB-051 voltou ao gate: a visão do gate passou a ter **47/47**.
| Visão do gate (`npm run test:gate`) | 46 | 46 | 0 | ~1,3 min |

Detalhes em [docs/test-execution/web-2026-10-29.md](../../docs/test-execution/web-2026-10-29.md).
