# Regras Testáveis (RN / NF)

Consolidação da base de teste oficial em regras verificáveis. Cada regra cita a fonte (abreviações em [README.md](README.md)). Quando a documentação é ambígua, a regra referencia a questão (`Q-nn`) com a interpretação adotada em [open-questions.md](open-questions.md).

## A. Criação de pedido — entrada (`POST /api/v1/orders`)

| ID | Regra | Fonte |
|---|---|---|
| RN-01 | **Dados do cliente.** `client.name`, `client.document` e `client.email` são obrigatórios e não podem ser vazios nem só espaços. O e-mail deve ter formato válido. Tamanhos máximos: nome 120, documento 20, e-mail 120 caracteres. Violação → `400 VALIDATION_ERROR`, com `details` indicando o campo. (Q-02, Q-03) | PDF §2.2.2; api; db (`VARCHAR(120/20/120)`) |
| RN-02 | **Quantidade de itens.** O pedido tem de 1 a 100 itens. Lista ausente, nula, vazia ou com mais de 100 itens → `400 VALIDATION_ERROR`. (Q-07) | PDF §2.2.2; api; req; arch |
| RN-03 | **Item.** `productId` é obrigatório, não vazio, com no máximo 50 caracteres. `quantity` é inteiro obrigatório entre 1 e 999. Violação → `400 VALIDATION_ERROR`. (Q-05) | PDF §2.2.2; api; db (`product_id VARCHAR(50)`) |
| RN-04 | **Produto repetido.** O mesmo `productId` em mais de um item → `400 VALIDATION_ERROR` (detalhe `productId: duplicate product <id>`). A validação ocorre antes de consultar o catálogo. | PDF §2.2.2; ADR-009 |
| RN-05 | **Cliente não define preço.** Cada item informa apenas `productId` e `quantity`. Preço, nome do produto, desconto, total, status e identificador são obtidos ou calculados pelo sistema, e campos com esses dados enviados pelo cliente não influenciam o resultado. | PDF §2.2 ("o serviço de Pedidos não é a autoridade sobre preço") e §2.2.2 |
| RN-06 | **Corpo da requisição.** Corpo acima de 1 MB → `413 PAYLOAD_TOO_LARGE`, rejeitado antes da desserialização. JSON malformado ou ilegível → `400 VALIDATION_ERROR`. (Q-08) | PDF §2.2.2; api; arch |

## B. Validação dos produtos no catálogo

| ID | Regra | Fonte |
|---|---|---|
| RN-07 | **All-or-nothing.** Se qualquer produto não existir (`PRODUCT_NOT_FOUND`), estiver inativo (`PRODUCT_INACTIVE`) ou não tiver preço vigente (`NO_CURRENT_PRICE`), o pedido inteiro é rejeitado com `422`, com os IDs em `details` e **sem persistência parcial**. (Q-01) | PDF §2.2.2; api; ADR-004 |
| RN-08 | **Preço vigente.** É o da janela cujo intervalo contém o instante da consulta: `validFrom ≤ instante < validTo`, com `validTo` nulo significando vigência indefinida. Sem janela vigente → `currentPrice` nulo. | PDF §2.2.2; ADR-007; api |
| RN-09 | **Integração em lote.** A criação de um pedido faz **uma única** chamada `POST /api/v1/products/prices` ao catálogo com os IDs distintos do pedido, propagando o `X-Request-Id` corrente. | ADR-004; ADR-010; arch |
| RN-10 | **Resiliência.** Catálogo inacessível → `503 CATALOG_UNAVAILABLE`. Sem resposta dentro do tempo limite (leitura 3 s, conexão 2 s) → `504 CATALOG_TIMEOUT`. Até 2 tentativas em erro de conexão. Em qualquer falha, nada é persistido. | PDF §2.2.2; arch; ADR-004 |

## C. Cálculo e registro do pedido

| ID | Regra | Fonte |
|---|---|---|
| RN-11 | **Snapshot.** O nome e o preço unitário do produto são registrados no item no momento da criação. Alterações posteriores no catálogo não mudam pedidos existentes. | PDF §2.2.2; ADR-006 |
| RN-12 | **Totais de item e subtotal.** `lineTotal = unitPrice × quantity`; `subtotal = Σ lineTotal`. Valores monetários com escala 2 e arredondamento `HALF_UP`. | req; ADR-015 |
| RN-13 | **Desconto.** Aplica-se a regra ativa (havendo mais de uma, a mais antiga com `active = TRUE`). Se `subtotal ≥ minimum_amount`, então `discount = subtotal × percentage / 100`, `HALF_UP`, 2 casas. Abaixo do mínimo, ou sem regra ativa, `discount = 0,00`. `total = subtotal − discount`. Regra inicial: a partir de R$ 1.000,00 → 10%. | PDF §2.2.2; ADR-005; db; req; seed |
| RN-14 | **Status inicial.** Todo pedido é criado com status `CRIADO`. | PDF §2.2.2; ADR-012 |
| RN-15 | **Resposta de criação.** `201 Created` com header `Location: /api/v1/orders/{id}` e corpo com `id` (UUID), `client`, `items` (`productId`, `productName`, `unitPrice`, `quantity`, `lineTotal`), `subtotal`, `discount`, `total`, `status` e `createdAt` (ISO-8601, UTC). | api |

## D. Consulta e listagem de pedidos

| ID | Regra | Fonte |
|---|---|---|
| RN-16 | **Detalhe.** `GET /api/v1/orders/{id}` retorna o pedido completo, idêntico ao devolvido na criação. Pedido inexistente → `404 ORDER_NOT_FOUND`. | api |
| RN-17 | **Listagem paginada.** `GET /api/v1/orders`: `page` (padrão 0), `size` (padrão 20, máximo 100). Valores fora da faixa são **normalizados**. Ordenação do mais recente para o mais antigo (`createdAt` desc). Corpo: `content`, `page`, `size`, `totalElements`, `totalPages`. (Q-10) | PDF §2.2.2; api; ADR-011 |
| RN-18 | **Filtro por status.** Parâmetro opcional `status`. Valor válido: `CRIADO`. Valor inválido → `400 VALIDATION_ERROR` com a lista de valores aceitos. | PDF §2.2.2; api; ADR-011 |

## E. Catálogo (`catalog-service`)

| ID | Regra | Fonte |
|---|---|---|
| RN-19 | **Consulta individual.** `GET /api/v1/products/{id}` retorna `id`, `name`, `description`, `status` e `currentPrice` (`value`, `currency`, `validFrom`, `validTo`), ou `currentPrice: null` sem janela vigente. Produto inexistente → `404 PRODUCT_NOT_FOUND`. | api |
| RN-20 | **Consulta em lote.** `POST /api/v1/products/prices` com 1 a 50 IDs não vazios e sem duplicatas. Retorna `prices` (`productId`, `name`, `status`, `currentPrice`) e `notFound` (IDs inexistentes). Entrada inválida → `400 VALIDATION_ERROR`. (Q-04) | api; arch |
| RN-21 | **Somente leitura.** O catálogo não possui endpoints de cadastro. Os produtos vêm da massa inicial carregada na subida do ambiente. | PDF §2.2.2 |
| RN-22 | **Massa inicial.** O ambiente sobe com a massa abaixo, que cobre os cenários de vigência e de situação. (H-05) | db; req; seed |

Massa inicial (RN-22), conforme `seed`:

| Produto | Nome | Situação | Janelas de preço | Preço vigente esperado |
|---|---|---|---|---|
| PROD-001 | Notebook Dell Inspiron 15 | ATIVO | 3500.00 desde 2025-01-01 (sem fim) | 3500.00 BRL |
| PROD-002 | Mouse Logitech MX Master 3S | ATIVO | 499.90 desde 2025-01-01 (sem fim) | 499.90 BRL |
| PROD-003 | Teclado Mecanico Keychron K2 | ATIVO | 700.00 de 2025-01-01 a 2025-06-01; 649.90 desde 2025-06-01 | 649.90 BRL |
| PROD-004 | Monitor LG UltraGear 24 | INATIVO | 1200.00 desde 2025-01-01 | 1200.00 (produto inativo) |
| PROD-005 | Webcam Logitech C920 | ATIVO | 599.00 a partir de 2030-01-01 (futuro) | nenhum (`null`) |
| PROD-006 | Headset HyperX Cloud II | ATIVO | 799.00 de 2025-01-01 a 2025-03-01 (expirado) | nenhum (`null`) |

Regra de desconto inicial (`discount_rule`): `minimum_amount = 1000.00`, `percentage = 10.00`, `active = TRUE`.

## F. Regras transversais (ambos os serviços)

| ID | Regra | Fonte |
|---|---|---|
| RN-23 | **Envelope de erro único.** Todo erro responde `{"error": {"code", "message", "details": []}, "requestId"}`, com códigos estáveis e documentados. Erros internos não expõem stack trace nem detalhes de implementação. | PDF §2.2.2; ADR-008; arch |
| RN-24 | **Identificador da requisição.** O header `X-Request-Id` é **sempre** devolvido (sucesso e erro). Quando o cliente envia o header, o valor é aceito e ecoado. Quando não envia, é gerado. O `requestId` do envelope coincide com o header. | PDF §2.2.2; api; arch |
| RN-25 | **Semântica HTTP.** 400 = entrada inválida; 404 = recurso inexistente; 422 = regra de negócio violada; 413 = payload grande; 5xx = falha de infraestrutura (503/504 classificam a dependência). Entrada inválida do cliente nunca resulta em 5xx. | ADR-008 |
| RN-26 | **Proteções operacionais sem autenticação.** Os endpoints são abertos (decisão documentada), com validação de entrada, limite de payload, limites de itens e quantidade, paginação limitada e **nenhuma operação destrutiva exposta**. | ADR-013 |
| RN-27 | **Saúde.** `/actuator/health` responde `UP` com o serviço disponível. O Actuator expõe apenas `health` e `info`. | arch; `application.yml` dos serviços (configuração) |

## G. Interface web

| ID | Regra | Fonte |
|---|---|---|
| RN-28 | **Navegação.** O shell tem menu para Dashboard (Painel), Pedidos e Catálogo e um seletor de idioma. Rota inexistente → página 404. | PDF §2.2.1; fe-arch |
| RN-29 | **Painel.** Exibe o total de pedidos (`totalElements`), os pedidos carregados (até 100, da primeira página), o valor total dos pedidos carregados e a contagem por status. Mostra também o aviso de indicadores indisponíveis e os atalhos para criar pedido, ver pedidos e consultar o catálogo. Sem pedidos → estado vazio. | PDF §2.2.1; fe-int |
| RN-30 | **Catálogo.** Consulta por 1 ID (individual) ou vários IDs separados por vírgula (lote, até 50). Exibe ID, nome, preço vigente formatado, moeda e situação. IDs inexistentes do lote são sinalizados. Produto sem preço vigente exibe "—". | PDF §2.2.1; fe-int; i18n (`catalog.searchHint`) |
| RN-31 | **Novo pedido.** O usuário informa os dados do cliente (nome, documento, e-mail) e adiciona produtos por ID e quantidade. O produto é validado no catálogo ao ser adicionado: inexistente, inativo, sem preço ou repetido gera aviso e não é adicionado. A tela exibe o preço unitário, o total por item e o subtotal. O desconto e o total são calculados pelo sistema na criação. Em caso de sucesso, mostra confirmação e abre o detalhe do pedido. Cancelar com dados preenchidos pede confirmação. | PDF §2.2.1; fe-int |
| RN-32 | **Pedidos.** Listagem paginada (20 por página) com identificador, cliente, quantidade de itens, subtotal, desconto, total, situação e data de criação, com acesso ao detalhe. O detalhe mostra o cliente, o resumo (id, situação, data, subtotal, desconto, total) e os itens (produto, quantidade, preço unitário, total). Lista vazia → estado vazio com ação de criar pedido. | PDF §2.2.1; fe-int |
| RN-33 | **Idiomas.** pt-BR (padrão), en e es. A troca acontece em tempo de execução, sem recarregar a página, e atualiza textos, moeda e datas no formato do idioma. A escolha é persistida (`localStorage`, chave `app-language`). Chave ausente → fallback pt-BR. | PDF §2.2.1; i18n |
| RN-34 | **Erros na interface.** As falhas de API são exibidas com mensagem traduzida por código do envelope, com fallback código → status HTTP → mensagem genérica. A mensagem mostra o `requestId` e oferece "tentar novamente" nas telas de consulta. Backend inacessível → mensagem de conexão. | fe-int |

## H. Requisitos não funcionais testáveis

A documentação do SUT **não define SLAs**. Os limites abaixo são **propostos** pelo projeto de qualidade (premissas) e precisam ser homologados com os stakeholders (Q-06).

| ID | Requisito | Fonte |
|---|---|---|
| NF-01 | **Desempenho (SLO proposto).** Sob carga nominal: `POST /orders` p95 < 800 ms e p99 < 1500 ms; `GET /orders` p95 < 400 ms; `GET /orders/{id}` p95 < 300 ms; `GET /products/{id}` p95 < 200 ms; `POST /products/prices` p95 < 300 ms; taxa de erro < 1%. | Premissa (Q-06); ADR-004, ADR-010, ADR-011 |
| NF-02 | **Estabilidade sob carga.** Sem erros 5xx, vazamento de recursos ou degradação progressiva (> 20% no p95) em carga sustentada. O sistema se recupera após pico. | Premissa (Q-06) |
| NF-03 | **Segurança de entrada e exposição.** Sem injeção (SQL/NoSQL), XSS ou vazamento de informação técnica. Superfície exposta mínima (Actuator, headers). Entradas abusivas são rejeitadas sem 5xx. | ADR-008; ADR-013; RN-23, RN-26 |
| NF-04 | **Acessibilidade básica.** As telas usam rótulos, papéis e textos alternativos acessíveis (WCAG 2.1 nível A/AA), sem violações críticas. | fe-arch ("componentes acessíveis do ng-bootstrap"); boa prática |
