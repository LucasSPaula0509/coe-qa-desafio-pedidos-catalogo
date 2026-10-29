# ADR-QA-003: Testes de front-end com Playwright (TypeScript)

**Status:** Aceita

## Contexto

A Parte 3 exige automatizar a interface web (SPA Angular 21 zoneless) para Painel, Catálogo, Novo pedido, Pedidos e Idiomas (pt-BR, en, es). Restrições observadas no SUT:

- **Não há nenhum `data-testid`** nos templates, e o código do SUT não pode ser alterado.
- Todos os textos vêm de `sut/frontend/public/assets/i18n/*.json` e mudam com o idioma em tempo de execução.
- Parte do conteúdo é assíncrono (toasts, spinners, paginação do ng-bootstrap), e a estabilidade é critério de avaliação ("confiabilidade da execução").

## Decisão

**Playwright Test com TypeScript**, com estas convenções:

- **Localizadores semânticos:** `getByRole`, `getByLabel` e os `id` existentes (`#client-name`, `#catalog-ids`). O SUT tem labels, `aria-label` e `caption` acessíveis.
- **Textos lidos dos JSONs de i18n do próprio SUT** (helper `t('orders.form.submit', lang)`). O mesmo teste roda nos três idiomas, e uma mudança de tradução não quebra o seletor.
- **Page Object Model** por página (`DashboardPage`, `CatalogPage`, `OrderFormPage`, `OrderListPage`, `OrderDetailPage`).
- **Dados criados via API** no setup (`request` fixture), para não depender da ordem dos testes nem de dados de outra suíte.
- **Sem `waitForTimeout`:** só esperas por estado (auto-wait e asserções web-first).
- **Evidências:** trace, screenshot e vídeo retidos apenas em falha. Os retries do CI ficam registrados para medir a instabilidade (*flakiness*), e não para escondê-la.
- Chromium como navegador obrigatório no gate. Firefox e WebKit rodam no job estendido (não bloqueante).

## Alternativas consideradas

- **Cypress:** maduro, mas multi-aba e multi-navegador são limitados, e paralelismo e trace exigem serviço pago.
- **Selenium/WebDriverIO:** maior controle, porém mais código de infraestrutura (esperas, drivers) para o mesmo resultado, e mais risco de instabilidade em uma SPA zoneless.

## Consequências

- **Positivas:**
  - Auto-wait e asserções web-first reduzem a instabilidade.
  - O trace viewer acelera a análise de falhas e serve de evidência em Issues.
  - Testar em vários idiomas sai praticamente sem custo.
  - A mesma linguagem do front-end do SUT (TypeScript) facilita a manutenção pelo squad de front.
- **Negativas:**
  - Localizadores por papel e texto dependem da acessibilidade do SUT. Onde ela falhar, o problema será registrado como achado de acessibilidade, porque não é possível adicionar `data-testid`.
  - Mais uma toolchain (Node) no CI, mitigada por cache.

## Versões e dependências (atualização de 2026-10-29)

- **Playwright fixado em 1.48.2**, por decisão do responsável pelo projeto. Todas as dependências usam versões exatas (`--save-exact`) e o `package-lock.json` é versionado.
- **Risco aceito:** o `npm audit` aponta a [GHSA-7mvr-c777-76hp](https://github.com/advisories/GHSA-7mvr-c777-76hp) (alta) para Playwright < 1.55.1: o **download dos navegadores** (`npx playwright install`) não verifica o certificado SSL. O risco é restrito à instalação (possível MITM), não à execução dos testes. Mitigação:
  - no CI, usar a imagem oficial `mcr.microsoft.com/playwright:v1.48.2-jammy`, que já traz os navegadores e elimina o download em tempo de execução;
  - localmente, instalar os navegadores apenas em rede confiável.
  A atualização para ≥ 1.55.1 fica registrada como recomendação.
- **Uma única cópia do `playwright-core`:** o `@axe-core/playwright` declara `playwright-core >= 1.0.0` como dependência *peer*, e o npm instalava uma segunda cópia (1.64.1). O `overrides` do `package.json` força a 1.48.2 em toda a árvore, mantendo a versão decidida e os tipos coerentes.
- **ESLint 9.39.5** (mesma versão maior), para corrigir a vulnerabilidade baixa do `@eslint/plugin-kit` (GHSA-xffm-g5w8-qvg7), com `typescript-eslint` 8.71.2, compatível com ela.
