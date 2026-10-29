# Casos de Teste — Interface Web (E2E)

Aplicação: `http://localhost:4200` (SPA Angular servida por nginx, que faz o proxy de `/api/orders` e `/api/catalog`). Pré-condição geral: ambiente completo no ar, `localStorage` limpo (idioma padrão pt-BR), massa inicial (RN-22).

**Princípios** (ADR-QA-003):

- A **UI verifica apresentação e fluxo**: as regras de cálculo já são exaustivamente cobertas na camada de API (pirâmide). A UI confere que os valores exibidos são os devolvidos pela API.
- O oráculo dos textos são os arquivos de tradução do SUT (`sut/frontend/public/assets/i18n/*.json`).
- O oráculo de formatação de moeda e data é `Intl` no mesmo locale.
- Os dados são criados via API no setup, e os estados difíceis de produzir (lista vazia, erro 5xx) usam interceptação de rede (WEB+MOCK).

## A. Shell e navegação

| ID | Cenário | Entrada / pré-condição | Resultado esperado | Ref. | Técnica | Prio | Nível |
|---|---|---|---|---|---|---|---|
| CT-WEB-001 | Idioma padrão na primeira visita | `localStorage` vazio; abrir `/` | Textos em pt-BR (menu: "Dashboard", "Pedidos", "Catálogo"); seletor indica "Português". A nomenclatura "Dashboard" × "Painel" (PDF) é registrada | RN-33, RN-28, Q-11 | PE | P1 | WEB |
| CT-WEB-002 | Navegação pelo menu lateral | Clicar em Dashboard, Pedidos e Catálogo | URLs `/`, `/orders` e `/catalog`; título da página correspondente; item do menu ativo destacado | RN-28 | CU | P2 | WEB |
| CT-WEB-003 | Rota inexistente | Abrir `/rota-inexistente` | Redireciona para `/404` com a página "não encontrada" | RN-28 | SE | P3 | WEB |
| CT-WEB-057 | Aplicação operante quando acessada por **nome de host via HTTP** (fora de `localhost`) | Navegador com `--host-resolver-rules` mapeando `qa-pedidos.test` para o front-end; pré-condição verificada: `isSecureContext = false` | Painel com textos traduzidos (título "Dashboard") e menu de navegação | RN-28, RN-33, RN-34, H-18 | SE | P2 | WEB |

## B. Painel (Dashboard)

| ID | Cenário | Entrada / pré-condição | Resultado esperado | Ref. | Técnica | Prio | Nível |
|---|---|---|---|---|---|---|---|
| CT-WEB-010 | Indicadores coerentes com a API | ≥ 1 pedido criado via API. Oráculo: `GET /api/v1/orders?page=0&size=100` no mesmo instante | "Total de pedidos" = `totalElements`; "Pedidos carregados" = `content.length`; "Valor total (pedidos carregados)" = soma dos `total` (formatada em BRL); contagem por status = pedidos `CRIADO` carregados | RN-29 | CU | P1 | WEB |
| CT-WEB-011 | Atalhos do painel | Clicar em "Criar pedido", "Ver pedidos" e "Consultar catálogo" | Navega para `/orders/new`, `/orders` e `/catalog` | RN-29 | CU | P2 | WEB |
| CT-WEB-012 | Aviso de indicadores indisponíveis | Abrir `/` | Exibe o aviso "Indicadores não disponíveis" com a mensagem correspondente | RN-29 | PE | P3 | WEB |
| CT-WEB-013 | Painel sem pedidos | API de pedidos interceptada: página vazia | Exibe "Nenhum pedido carregado ainda." e mantém os atalhos | RN-29 | PE | P3 | WEB+MOCK |
| CT-WEB-014 | Erro de API no painel | API interceptada: `503` com envelope (`requestId` conhecido) | Mensagem traduzida para 503, `requestId` exibido e botão "Tentar novamente". Ao clicar (com a API restabelecida), os indicadores são carregados | RN-34 | PE | P2 | WEB+MOCK |

## C. Catálogo

| ID | Cenário | Entrada / pré-condição | Resultado esperado | Ref. | Técnica | Prio | Nível |
|---|---|---|---|---|---|---|---|
| CT-WEB-020 | Consulta individual | Buscar `PROD-001` | Uma linha: ID PROD-001, "Notebook Dell Inspiron 15", preço "R$ 3.500,00", moeda BRL, situação "Ativo" | RN-30 | CU | P1 | WEB |
| CT-WEB-021 | Consulta em lote com situações diferentes | `PROD-001, PROD-004, PROD-005` | 3 linhas; PROD-004 com situação "Inativo"; PROD-005 com preço "—" e moeda "—" | RN-30, RN-08 | TD | P1 | WEB |
| CT-WEB-022 | Lote com ID inexistente | `PROD-001, PROD-999` | Linha de PROD-001 + alerta "IDs não encontrados: PROD-999" | RN-30 | PE | P1 | WEB |
| CT-WEB-023 | Consulta individual de ID inexistente | `PROD-999` | Mensagem de produto não encontrado, com `requestId` | RN-30, RN-34 | PE | P2 | WEB |
| CT-WEB-024 | Campo obrigatório | Consultar com o campo vazio | Mensagem de campo obrigatório; **nenhuma requisição** ao catálogo | RN-30 | PE | P2 | WEB |
| CT-WEB-025 | Separadores e duplicatas | `PROD-001;PROD-002  PROD-001` | Consulta em lote com 2 IDs (deduplicados); 2 linhas | RN-30 | SE | P3 | WEB |
| CT-WEB-026 | Limite do lote na interface | 50 IDs → consulta; 51 IDs | 50 → requisição feita e resultados exibidos; 51 → aviso "Consulte no máximo 50 IDs por vez." e **nenhuma requisição** | RN-30, RN-20 | VL | P2 | WEB |

## D. Novo pedido

| ID | Cenário | Entrada / pré-condição | Resultado esperado | Ref. | Técnica | Prio | Nível |
|---|---|---|---|---|---|---|---|
| CT-WEB-030 | **Criação de pedido com desconto** (fluxo principal) | Cliente válido; adicionar PROD-001 × 2 e PROD-002 × 1; "Criar pedido" | A tabela mostra o preço unitário (R$ 3.500,00 / R$ 499,90), o total por item (R$ 7.000,00 / R$ 499,90) e o subtotal R$ 7.499,90. Após criar: toast "Pedido criado com sucesso", navega a `/orders/{id}` e o detalhe exibe desconto R$ 749,99 e total R$ 6.749,91, iguais ao `GET` da API | RN-31, RN-32, RN-13 | CU | P1 | WEB |
| CT-WEB-031 | Criação abaixo do mínimo de desconto | PROD-002 × 1 | Detalhe com desconto R$ 0,00 e total R$ 499,90 | RN-31, RN-13 | PE | P1 | WEB |
| CT-WEB-032 | Campos do cliente obrigatórios | Itens válidos, cliente vazio, "Criar pedido" | Mensagem de obrigatório nos 3 campos; aviso "Verifique os campos destacados…"; **nenhum** `POST /orders` | RN-31, RN-01 | PE | P1 | WEB |
| CT-WEB-033 | E-mail inválido | E-mail `maria@` | Mensagem de e-mail inválido no campo; nenhum `POST` | RN-31, RN-01 | PE | P2 | WEB |
| CT-WEB-034 | Adicionar produto inexistente | `PROD-999` | Aviso "Produto não encontrado: PROD-999"; item **não** adicionado | RN-31, RN-07 | PE | P1 | WEB |
| CT-WEB-035 | Adicionar produto inativo | `PROD-004` | Aviso "Produto inativo no catálogo: PROD-004"; item não adicionado | RN-31, RN-07 | PE | P1 | WEB |
| CT-WEB-036 | Adicionar produto sem preço vigente | `PROD-005` e `PROD-006` | Aviso "Produto sem preço vigente: <id>" para cada um; itens não adicionados | RN-31, RN-07, RN-08 | PE | P1 | WEB |
| CT-WEB-037 | Adicionar produto já incluído | PROD-001 duas vezes | Aviso "Produto já adicionado: PROD-001"; continua 1 linha | RN-31, RN-04 | PE | P2 | WEB |
| CT-WEB-038 | Limites de quantidade na interface | Quantidade 0 e 1000 (ao adicionar e ao editar a linha) | Mensagem "Quantidade deve estar entre 1 e 999."; criação bloqueada enquanto houver valor inválido | RN-31, RN-03 | VL | P2 | WEB |
| CT-WEB-039 | Edição de quantidade recalcula valores exibidos | PROD-002 × 1 → alterar para 3 | Total da linha R$ 1.499,70; subtotal atualizado | RN-31 | CU | P2 | WEB |
| CT-WEB-040 | Remoção de itens | 2 itens → remover um; depois o outro | Tabela e subtotal atualizados; sem itens → "Nenhum item adicionado…" | RN-31 | CU | P2 | WEB |
| CT-WEB-041 | Criar sem itens | Cliente válido, nenhum item | Aviso de formulário inválido; nenhum `POST /orders` | RN-31, RN-02 | PE | P2 | WEB |
| CT-WEB-042 | Erro de regra de negócio na criação | `POST /orders` interceptado: `422 PRODUCT_INACTIVE` com `requestId` conhecido | Mensagem "Produto inativo no catálogo." com `requestId`; permanece no formulário com os dados preservados; botão habilitado de novo | RN-31, RN-34 | PE | P2 | WEB+MOCK |
| CT-WEB-043 | Catálogo indisponível na criação | `POST /orders` interceptado: `503 CATALOG_UNAVAILABLE` | Mensagem "Serviço de catálogo indisponível…" com `requestId`; dados preservados | RN-31, RN-34, RN-10 | PE | P2 | WEB+MOCK |
| CT-WEB-044 | Cancelar com e sem dados | (a) com dados → Cancelar → "Descartar e sair"; (b) com dados → Cancelar → fechar o diálogo; (c) sem dados → Cancelar | (a) vai para `/orders`; (b) permanece com os dados; (c) vai direto para `/orders`, sem diálogo | RN-31, H-14 | TD | P3 | WEB |
| CT-WEB-045 | Duplo clique em "Criar pedido" | Formulário válido; dois cliques rápidos | Exatamente **1** `POST /orders` e 1 pedido criado | RN-31 | SE | P2 | WEB |

## E. Pedidos (listagem e detalhe)

| ID | Cenário | Entrada / pré-condição | Resultado esperado | Ref. | Técnica | Prio | Nível |
|---|---|---|---|---|---|---|---|
| CT-WEB-050 | Pedido recém-criado no topo da lista | Pedido criado via API no setup | Primeira linha: id curto (13 caracteres + "…", id completo no tooltip), nome do cliente, quantidade de itens, subtotal, desconto, total, situação "Criado" e data, coerentes com a API | RN-32, RN-17 | CU | P1 | WEB |
| CT-WEB-051 | Paginação | ≥ 21 pedidos no ambiente | Controle de paginação visível. O controle "próxima" leva à página 2, que exibe os pedidos de `GET ?page=1&size=20`, na mesma ordem (válido em qualquer volume) | RN-32, RN-17 | CU | P2 | WEB |
| CT-WEB-058 | Paginação com **volume**: todos os links de página ao alcance | ≥ 1.000 pedidos (criados via API, em lotes, se faltarem) | Links da primeira, da segunda e da última página visíveis na tela (rolando se preciso); clicar na página 2 exibe os pedidos de `GET ?page=1&size=20` | RN-32, RN-17, H-15 | VL | P2 | WEB |
| CT-WEB-052 | Detalhe a partir da lista | Clicar em "Ver" no pedido do setup | Detalhe com o cliente (nome, documento, e-mail), o resumo (id, situação, data, subtotal, desconto, total) e os itens (produto, ID, quantidade, preço unitário, total), iguais ao `GET /orders/{id}` | RN-32, RN-16 | CU | P1 | WEB |
| CT-WEB-053 | Detalhe de pedido inexistente | Abrir `/orders/<uuid-aleatório>` | Mensagem "Pedido não encontrado." com `requestId` | RN-32, RN-34 | PE | P2 | WEB |
| CT-WEB-054 | Lista vazia | API interceptada: página vazia | Estado vazio com ação "Novo pedido", que navega para `/orders/new` | RN-32 | PE | P3 | WEB+MOCK |
| CT-WEB-055 | Voltar do detalhe | No detalhe, clicar em "Voltar" | Retorna para `/orders` | RN-32 | CU | P3 | WEB |
| CT-WEB-056 | Lista de pedidos continua disponível após **redeploy** do `order-service` | `order-service` recriado com outro IP (o IP anterior é ocupado por um container descartável para forçar a troca); o front-end **não** é reiniciado | A tela de Pedidos carrega a listagem normalmente (o proxy do front-end acompanha o novo endereço). Ambiente restaurado ao final | RN-32, RN-34, H-13 | SE | P2 | WEB |

## F. Idiomas

| ID | Cenário | Entrada / pré-condição | Resultado esperado | Ref. | Técnica | Prio | Nível |
|---|---|---|---|---|---|---|---|
| CT-WEB-060 | Troca de idioma em tempo de execução | pt-BR → en → es pelo seletor | Menu, títulos e rótulos mudam para o idioma escolhido (oráculo: JSON de tradução) **sem recarregar a página** (nenhum evento de navegação ou *load*) | RN-33 | CU | P1 | WEB |
| CT-WEB-061 | Formatação de moeda e data por idioma | Detalhe de um pedido conhecido em pt-BR, en e es | Valores e datas formatados como `Intl.NumberFormat`/`DateTimeFormat` no locale correspondente (BRL, 2 casas) | RN-33, H-17 | PE | P2 | WEB |
| CT-WEB-062 | Preferência de idioma persistida | Escolher es e recarregar a página | Interface continua em es; `localStorage['app-language']` = `es` | RN-33 | CU | P2 | WEB |
| CT-WEB-063 | Fluxos principais em cada idioma | Parametrizado (pt-BR, en, es): CT-WEB-020 e CT-WEB-030 | Fluxos concluídos com os textos do idioma corrente | RN-33, RN-30, RN-31 | PE | P2 | WEB |
| CT-WEB-064 | Paridade dos arquivos de tradução | `pt-BR.json`, `en.json`, `es.json` | Mesmo conjunto de chaves nos 3 arquivos, nenhum valor vazio e os mesmos *placeholders* (`{{id}}`, `{{ids}}`, `{{name}}`) por chave | RN-33 | CO | P3 | ESTÁTICO |
| CT-WEB-065 | Mensagens de erro traduzidas | CT-WEB-034 em en e es | Avisos no idioma corrente ("Product not found: PROD-999" / "Producto no encontrado: PROD-999") | RN-33, RN-34 | PE | P3 | WEB |

## G. Acessibilidade

| ID | Cenário | Entrada / pré-condição | Resultado esperado | Ref. | Técnica | Prio | Nível |
|---|---|---|---|---|---|---|---|
| CT-WEB-070 | Varredura automática de acessibilidade | Painel, Catálogo (com resultados), Novo pedido (com itens), Pedidos, Detalhe | axe-core (WCAG 2.1 A/AA): nenhuma violação de impacto *critical* ou *serious* | NF-04, H-16 | CO | P3 | WEB |
| CT-WEB-071 | Operação só por teclado | Fluxo do CT-WEB-030 sem mouse | Fluxo concluído com Tab/Enter/Espaço; foco visível | NF-04 | CU | P3 | WEB |
