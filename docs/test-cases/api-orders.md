# Casos de Teste — API de Pedidos (`order-service`)

Endpoint base: `http://localhost:8080/api/v1/orders`. Pré-condição geral: ambiente no ar (`/actuator/health` = `UP` nos dois serviços), massa inicial (RN-22) e regra de desconto inicial ativa (1000,00 / 10%). Cliente `CLIENTE_PADRAO`, salvo indicação. Convenções em [README.md](README.md).

Os valores monetários são comparados **numericamente com 2 casas** (`BigDecimal`), e não como texto do JSON.

## A. Criação e cálculo

| ID | Cenário | Entrada / pré-condição | Resultado esperado | Ref. | Técnica | Prio | Nível |
|---|---|---|---|---|---|---|---|
| CT-ORD-001 | Pedido válido de 1 item abaixo do mínimo de desconto | PROD-002 × 1 | `201`; item `productName` "Mouse Logitech MX Master 3S", `unitPrice` 499.90, `lineTotal` 499.90; `subtotal` 499.90; `discount` 0.00; `total` 499.90; `status` `CRIADO` | RN-12, RN-13, RN-14 | PE | P1 | API |
| CT-ORD-002 | Pedido com vários itens e desconto (exemplo oficial da documentação) | PROD-001 × 2, PROD-002 × 1 | `201`; `lineTotal` 7000.00 e 499.90; `subtotal` 7499.90; `discount` 749.99; `total` 6749.91 | RN-12, RN-13 | CU | P1 | API |
| CT-ORD-003 | Maior subtotal possível **abaixo** do mínimo com a massa | PROD-002 × 2 | `subtotal` 999.80; `discount` 0.00; `total` 999.80 | RN-13, Q-09 | VL | P1 | API |
| CT-ORD-004 | Menor subtotal possível **acima** do mínimo com a massa | PROD-002 × 1, PROD-003 × 1 | `subtotal` 1149.80; `discount` 114.98; `total` 1034.82 | RN-13, Q-09 | VL | P1 | API |
| CT-ORD-005 | Subtotal **exatamente igual** ao mínimo recebe desconto ("a partir de") | Regra ajustada: mínimo 999.80, 10%. PROD-002 × 2 | `subtotal` 999.80; `discount` 99.98; `total` 899.82 | RN-13, Q-09 | VL | P1 | API+DB |
| CT-ORD-006 | Subtotal 1 centavo abaixo do mínimo não recebe desconto | Regra ajustada: mínimo 999.81, 10%. PROD-002 × 2 | `subtotal` 999.80; `discount` 0.00; `total` 999.80 | RN-13, Q-09 | VL | P2 | API+DB |
| CT-ORD-007 | Arredondamento **HALF_UP** do desconto (terceira casa = 5, dígito anterior par, o que distingue HALF_UP de HALF_EVEN e de truncamento) | Regra ajustada: mínimo 1000.00, **5%**. PROD-002 × 3 (1499.70 × 5% = 74.985) | `discount` **74.99** (HALF_EVEN e truncamento dariam 74.98); `total` 1424.71 | RN-13, RN-12 | VL | P1 | API+DB |
| CT-ORD-008 | Arredondamento para baixo (terceira casa < 5) | Regra ajustada: mínimo 1000.00, **3%**. PROD-002 × 3 (1499.70 × 3% = 44.991) | `discount` 44.99; `total` 1454.71 | RN-13 | VL | P2 | API+DB |
| CT-ORD-009 | Sem regra de desconto ativa → desconto zero | Todas as regras com `active = FALSE`. PROD-001 × 1 | `subtotal` 3500.00; `discount` 0.00; `total` 3500.00 | RN-13 | TD | P1 | API+DB |
| CT-ORD-010 | Mais de uma regra ativa → vale a **mais antiga** | Regra inicial mantida + nova regra ativa mais recente (mínimo 0, 50%). PROD-001 × 1 | `discount` 350.00 (10%, regra mais antiga), não 1750.00; `total` 3150.00 | RN-13 | TD | P2 | API+DB |
| CT-ORD-011 | Percentual máximo (100%) → total zero | Única regra ativa: mínimo 0, 100%. PROD-002 × 1 | `discount` 499.90; `total` 0.00; `201` (invariante `total ≥ 0` respeitada) | RN-13 | VL | P3 | API+DB |
| CT-ORD-012 | Quantidade máxima em um item | PROD-001 × 999 | `lineTotal` 3496500.00; `subtotal` 3496500.00; `discount` 349650.00; `total` 3146850.00 | RN-03, RN-12, RN-13 | VL | P2 | API |
| CT-ORD-013 | Maior valor agregado possível com a massa (todos os produtos válidos, quantidade máxima) | PROD-001 × 999, PROD-002 × 999, PROD-003 × 999 | `lineTotal` 3496500.00 / 499400.10 / 649250.10; `subtotal` 4645150.20; `discount` 464515.02; `total` 4180635.18 | RN-12, RN-13 | VL | P2 | API |
| CT-ORD-014 | Produto com histórico usa o preço da **janela vigente** | PROD-003 × 1 | `unitPrice` 649.90 (não 700.00, que está expirado) | RN-08 | PE | P1 | API |
| CT-ORD-015 | Campos de preço, total, status e id enviados pelo cliente são **ignorados** | PROD-001 × 1 com `unitPrice` 0.01, `productName` "Grátis" e `lineTotal` 0.01 no item; `subtotal` 0.01, `discount` 3500, `total` 0, `status` "PAGO", `id` fixo e `createdAt` "2000-01-01T00:00:00Z" na raiz | `201`; `unitPrice` 3500.00; `productName` "Notebook Dell Inspiron 15"; `subtotal` 3500.00; `discount` 350.00; `total` 3150.00; `status` `CRIADO`; `id` diferente do enviado; `createdAt` atual | RN-05, RN-14 | SE | P1 | API |
| CT-ORD-016 | Contrato da resposta de criação | PROD-001 × 1 | `201`; header `Location` = `/api/v1/orders/{id}`, com o mesmo `id` do corpo; `id` é UUID; `createdAt` em ISO-8601 UTC, a até 5 min do relógio do teste; corpo valida contra o JSON Schema de pedido; `Content-Type` `application/json` | RN-15 | CO | P1 | API |
| CT-ORD-017 | Pedido criado fica persistido e íntegro | Criar com PROD-001 × 2 e PROD-002 × 1 | `GET /orders/{id}` retorna corpo **idêntico** ao da criação (cliente, itens, valores, status, `createdAt`) | RN-16, RN-11 | CU | P1 | API |
| CT-ORD-018 | Caracteres especiais e Unicode no cliente | `name` "José Ação Ñúñez 李雷", `document` "123.456.789-01" | `201`; os valores retornam idênticos na criação e no `GET` (UTF-8 preservado) | RN-01 | PE | P2 | API |

## B. Validação de entrada (400)

Todos os casos deste grupo verificam também: **envelope de erro** (RN-23), header `X-Request-Id` (RN-24) e **nenhum pedido persistido** (busca pelo e-mail do `<runId>` sem resultado).

| ID | Cenário | Entrada / pré-condição | Resultado esperado | Ref. | Técnica | Prio | Nível |
|---|---|---|---|---|---|---|---|
| CT-ORD-020 | Cliente ausente | Sem o objeto `client` | `400 VALIDATION_ERROR`; `details` menciona `client` | RN-01 | PE | P1 | API |
| CT-ORD-021 | Nome inválido | `name`: ausente, `null`, `""`, `"   "` (4 variações) | `400 VALIDATION_ERROR`; `details` menciona `client.name` | RN-01 | PE | P1 | API |
| CT-ORD-022 | Documento inválido | `document`: ausente, `null`, `""`, `"   "` | `400 VALIDATION_ERROR`; `details` menciona `client.document` | RN-01, Q-02 | PE | P1 | API |
| CT-ORD-023 | E-mail ausente ou vazio | `email`: ausente, `null`, `""`, `"   "` | `400 VALIDATION_ERROR`; `details` menciona `client.email` | RN-01 | PE | P1 | API |
| CT-ORD-024 | E-mail com formato inválido | `"maria"`, `"maria@"`, `"@example.com"`, `"maria silva@example.com"`, `"maria@@example.com"` | `400 VALIDATION_ERROR` para cada um; `details` menciona `client.email` | RN-01, Q-03 | PE | P1 | API |
| CT-ORD-025 | E-mail válido com subendereço e subdomínio | `"qa.<runId>+tag@sub.example.com"` | `201` | RN-01, Q-03 | PE | P2 | API |
| CT-ORD-026 | Limite do nome | `name` com 120 e com 121 caracteres | 120 → `201`; 121 → `400 VALIDATION_ERROR` | RN-01 | VL | P2 | API |
| CT-ORD-027 | Limite do documento | `document` com 20 e com 21 caracteres | 20 → `201`; 21 → `400 VALIDATION_ERROR` | RN-01, Q-02 | VL | P2 | API |
| CT-ORD-028 | Limite do e-mail | E-mail com 120 e com 121 caracteres (parte local ≤ 64 e rótulos de domínio ≤ 63, conforme RFC 5321) | 120 → `201`; 121 → `400 VALIDATION_ERROR` | RN-01 | VL | P2 | API |
| CT-ORD-030 | Pedido sem itens | `items`: ausente, `null`, `[]` | `400 VALIDATION_ERROR`; `details` menciona `items` | RN-02 | VL | P1 | API |
| CT-ORD-031 | Acima do limite de itens | 101 itens com `productId` distintos | `400 VALIDATION_ERROR` (rejeitado na validação, sem 422/5xx) | RN-02 | VL | P1 | API |
| CT-ORD-032 | Limite máximo de itens é aceito pela validação | 100 itens distintos: PROD-001..003 + 97 IDs inexistentes | **Não** é rejeitado por limite. `422 PRODUCT_NOT_FOUND` com os 97 IDs em `details`. Nunca `5xx` | RN-02, RN-07, Q-07, H-01 | VL | P1 | API |
| CT-ORD-033 | 50 itens distintos (limite do lote do catálogo) | PROD-001..003 + 47 IDs inexistentes | `422 PRODUCT_NOT_FOUND` com os 47 IDs em `details` | RN-02, RN-07, RN-09 | VL | P1 | API |
| CT-ORD-034 | Limites da quantidade | `quantity`: -1, 0, 1, 999, 1000 (PROD-002) | 1 e 999 → `201`; -1, 0 e 1000 → `400 VALIDATION_ERROR` | RN-03 | VL | P1 | API |
| CT-ORD-035 | Quantidade ausente | `quantity` ausente ou `null` | `400 VALIDATION_ERROR` | RN-03 | PE | P1 | API |
| CT-ORD-036 | Quantidade com tipo não inteiro | `quantity`: `1.5`, `"2"`, `true`, `"abc"` | `400 VALIDATION_ERROR` para cada um (sem conversão silenciosa) | RN-03, Q-05, H-03 | SE | P2 | API |
| CT-ORD-037 | `productId` inválido | `productId`: ausente, `null`, `""`, `"   "` | `400 VALIDATION_ERROR` | RN-03 | PE | P1 | API |
| CT-ORD-038 | Limite do `productId` | 50 caracteres (inexistente) e 51 caracteres | 50 → passa a validação e retorna `422 PRODUCT_NOT_FOUND`; 51 → `400 VALIDATION_ERROR` | RN-03, RN-07 | VL | P2 | API |
| CT-ORD-039 | Produto repetido | PROD-001 × 1 e PROD-001 × 2 | `400 VALIDATION_ERROR`; `details` contém `duplicate product PROD-001` | RN-04 | TD | P1 | API |
| CT-ORD-040 | Repetição tem precedência sobre a validação de catálogo | PROD-999 × 1 e PROD-999 × 1 | `400 VALIDATION_ERROR` (não `422`) | RN-04 | TD | P2 | API |
| CT-ORD-041 | IDs diferenciam maiúsculas e minúsculas | PROD-001 × 1 e `prod-001` × 1 | Não são duplicados. `422 PRODUCT_NOT_FOUND` com `prod-001` em `details` | RN-04, RN-07 | PE | P3 | API |
| CT-ORD-042 | JSON malformado | Corpo `{"client": {` (truncado) | `400 VALIDATION_ERROR` com envelope | RN-06, RN-23 | SE | P1 | API |
| CT-ORD-043 | Corpo vazio | `Content-Type: application/json` sem corpo | `400 VALIDATION_ERROR` com envelope | RN-06 | SE | P2 | API |
| CT-ORD-044 | `Content-Type` não suportado | Corpo JSON válido com `Content-Type: text/plain` | `4xx` (400 ou 415) com envelope. Nunca `500` | RN-25, H-02 | SE | P2 | API |
| CT-ORD-045 | Corpo de **exatamente 1 MB** é aceito | Pedido válido (PROD-002 × 1) completado com espaços em branco JSON até 1.048.576 bytes | Não é rejeitado por tamanho: `201` | RN-06, Q-08 | VL | P2 | API |
| CT-ORD-046 | Corpo **acima de 1 MB** é rejeitado | O mesmo pedido com 1.048.577 bytes (`Content-Length` informado) | `413 PAYLOAD_TOO_LARGE` com envelope e `X-Request-Id`; nenhum pedido persistido | RN-06, RN-23, Q-08 | VL | P1 | API |

## C. Validação dos produtos no catálogo (422)

Todos os casos verificam **ausência de persistência parcial**: nenhuma linha nova em `orders`/`order_items` para o `<runId>` (consulta direta ao PostgreSQL).

| ID | Cenário | Entrada / pré-condição | Resultado esperado | Ref. | Técnica | Prio | Nível |
|---|---|---|---|---|---|---|---|
| CT-ORD-050 | Produto inexistente | PROD-999 × 1 | `422 PRODUCT_NOT_FOUND`; `details` = `["PROD-999"]` | RN-07 | PE | P1 | API+DB |
| CT-ORD-051 | Produto inativo | PROD-004 × 1 | `422 PRODUCT_INACTIVE`; `details` = `["PROD-004"]` | RN-07 | PE | P1 | API+DB |
| CT-ORD-052 | Produto com preço **futuro** (sem janela vigente) | PROD-005 × 1 | `422 NO_CURRENT_PRICE`; `details` = `["PROD-005"]` | RN-07, RN-08 | PE | P1 | API+DB |
| CT-ORD-053 | Produto com preço **expirado** | PROD-006 × 1 | `422 NO_CURRENT_PRICE`; `details` = `["PROD-006"]` | RN-07, RN-08 | PE | P1 | API+DB |
| CT-ORD-054 | Item válido + item inválido → rejeição total | PROD-001 × 1 e PROD-999 × 1 | `422 PRODUCT_NOT_FOUND`; nenhum pedido nem item persistido | RN-07 | TD | P1 | API+DB |
| CT-ORD-055 | Vários inválidos do mesmo tipo | PROD-005 × 1 e PROD-006 × 1 | `422 NO_CURRENT_PRICE`; `details` contém PROD-005 e PROD-006 | RN-07 | TD | P2 | API+DB |
| CT-ORD-056 | Inválidos de tipos diferentes | PROD-999, PROD-004 e PROD-005 (× 1) | `422` com um dos códigos aplicáveis; `details` coerente com o código devolvido (só IDs daquele tipo). Ordem de precedência registrada | RN-07, Q-01 | TD | P2 | API+DB |

## D. Consulta por identificador

| ID | Cenário | Entrada / pré-condição | Resultado esperado | Ref. | Técnica | Prio | Nível |
|---|---|---|---|---|---|---|---|
| CT-ORD-060 | Consulta de pedido existente | Pedido criado no setup | `200`; corpo idêntico ao da criação; valida o JSON Schema de pedido | RN-16 | CO | P1 | API |
| CT-ORD-061 | Pedido inexistente | UUID v4 aleatório | `404 ORDER_NOT_FOUND` com envelope | RN-16, RN-23 | PE | P1 | API |
| CT-ORD-062 | Identificador com formato inválido | `abc`, `123`, `PROD-001` | `4xx` (400 ou 404) com envelope. Nunca `500` | RN-25, H-02 | SE | P2 | API |

## E. Listagem

Os casos de listagem são **robustos a dados acumulados** de outras execuções: comparam posições relativas e pedidos criados pelo próprio teste, nunca contagens absolutas do banco.

| ID | Cenário | Entrada / pré-condição | Resultado esperado | Ref. | Técnica | Prio | Nível |
|---|---|---|---|---|---|---|---|
| CT-ORD-070 | Listagem sem parâmetros | `GET /orders` | `200`; `page` 0; `size` 20; `content` com no máximo 20 itens; `totalElements` e `totalPages` coerentes (`totalPages = ⌈totalElements / size⌉`); valida o JSON Schema de página | RN-17 | CO | P1 | API |
| CT-ORD-071 | Ordenação do mais recente para o mais antigo | 3 pedidos criados em sequência pelo teste | Os 3 aparecem no topo da página 0, na ordem inversa da criação; `createdAt` não crescente em toda a página | RN-17 | CU | P1 | API |
| CT-ORD-072 | Navegação entre páginas sem sobreposição | ≥ 4 pedidos; `size=2`, páginas 0 e 1 | Páginas sem `id` em comum; a página 1 continua a ordenação da página 0 | RN-17 | CU | P1 | API |
| CT-ORD-073 | Normalização de `size` | `size` = 1, 100, 101, 0, -5 | 1 → `size` 1; 100 → 100; 101 → **100**; 0 → **1**; -5 → **1** (normalizado, sem erro) | RN-17 | VL | P2 | API |
| CT-ORD-074 | Normalização de `page` negativa | `page=-1` | `200`; `page` 0 | RN-17 | VL | P2 | API |
| CT-ORD-075 | Página além do total | `page=99999` | `200`; `content` vazio; `totalElements` e `totalPages` corretos | RN-17, Q-10 | VL | P3 | API |
| CT-ORD-076 | Filtro por status válido | `status=CRIADO` | `200`; todos os itens de `content` com `status` `CRIADO` | RN-18 | PE | P1 | API |
| CT-ORD-077 | Filtro por status inválido | `status` = `PAGO`, `criado`, `CRIADO'--` | `400 VALIDATION_ERROR`; `details` lista os valores aceitos (`CRIADO`) | RN-18 | PE | P1 | API |
| CT-ORD-078 | Parâmetros de paginação não numéricos | `page=abc`; `size=1e3` | `400` com envelope. Nunca `500` | RN-25, H-02 | SE | P2 | API |
| CT-ORD-079 | Itens completos na listagem | Pedido do teste com 2 itens | O pedido na listagem traz `items` com snapshot (`productName`, `unitPrice`, `lineTotal`) igual ao do detalhe | RN-17, RN-11 | CO | P2 | API |
