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
