# Casos de Teste — API de Catálogo (`catalog-service`)

Endpoint base: `http://localhost:8081/api/v1/products`. Pré-condição geral: ambiente no ar com a massa inicial (RN-22). O catálogo é somente leitura, então estes casos não alteram estado.

O oráculo de preço vigente é a massa inicial avaliada no instante da execução (RN-08). As datas da massa (2025, 2030) mantêm os resultados estáveis até 2030-01-01. **Risco temporal registrado:** a partir dessa data, PROD-005 passa a ter preço vigente, e os casos que dependem dele precisam de revisão (o teste falha com mensagem explícita sobre a data).

## A. Consulta individual (`GET /products/{id}`)

| ID | Cenário | Entrada / pré-condição | Resultado esperado | Ref. | Técnica | Prio | Nível |
|---|---|---|---|---|---|---|---|
| CT-CAT-001 | Produto ativo com preço vigente | `PROD-001` | `200`; `id` PROD-001; `name` "Notebook Dell Inspiron 15"; `status` `ATIVO`; `currentPrice` = {`value` 3500.00, `currency` BRL, `validFrom` 2025-01-01T00:00:00Z, `validTo` null}. A divergência com o exemplo da documentação (`validFrom` 2026, descrição) é registrada | RN-19, RN-22, H-05 | PE | P1 | API |
| CT-CAT-002 | Produto com histórico de preços | `PROD-003` | `currentPrice.value` 649.90; `validFrom` 2025-06-01T00:00:00Z; `validTo` null (a janela de 700.00 já expirou) | RN-08, RN-19 | PE | P1 | API |
| CT-CAT-003 | Produto inativo continua consultável | `PROD-004` | `200`; `status` `INATIVO`; `currentPrice.value` 1200.00 | RN-19, RN-22 | PE | P2 | API |
| CT-CAT-004 | Preço apenas futuro | `PROD-005` | `200`; `currentPrice` `null` | RN-08, RN-19 | PE | P1 | API |
| CT-CAT-005 | Preço expirado | `PROD-006` | `200`; `currentPrice` `null` | RN-08, RN-19 | PE | P1 | API |
| CT-CAT-006 | Produto inexistente | `PROD-999` | `404 PRODUCT_NOT_FOUND` com envelope e `X-Request-Id` | RN-19, RN-23 | PE | P1 | API |
| CT-CAT-007 | Identificadores incomuns | `PROD%20001`, `%2E%2E%2F`, `prod-001`, ID com 1.000 caracteres | `404 PRODUCT_NOT_FOUND` (ou `400`) com envelope. Nunca `500` nem conteúdo de outro recurso | RN-19, RN-25 | SE | P3 | API |
| CT-CAT-008 | Contrato da consulta individual | `PROD-001` e `PROD-005` | Corpo valida o JSON Schema de produto (campos obrigatórios, tipos, `currentPrice` nulo ou objeto, datas ISO-8601); `Content-Type` `application/json` | RN-19 | CO | P1 | API |

## B. Consulta em lote (`POST /products/prices`)

| ID | Cenário | Entrada / pré-condição | Resultado esperado | Ref. | Técnica | Prio | Nível |
|---|---|---|---|---|---|---|---|
| CT-CAT-010 | Lote de produtos válidos | `["PROD-001", "PROD-002"]` | `200`; `prices` com 2 entradas (nome, status e preço corretos); `notFound` `[]` | RN-20 | PE | P1 | API |
| CT-CAT-011 | Lote com todos os cenários da massa + inexistente | PROD-001..006 + PROD-999 | `prices` com 6 entradas; status e `currentPrice` conforme a tabela da RN-22 (PROD-005/006 com `currentPrice` null; PROD-004 `INATIVO`); `notFound` = `["PROD-999"]` | RN-20, RN-08, RN-22 | TD | P1 | API |
| CT-CAT-012 | Lote só com inexistentes | `["PROD-998", "PROD-999"]` | `200`; `prices` `[]`; `notFound` com os 2 IDs | RN-20 | PE | P2 | API |
| CT-CAT-013 | Limite inferior do lote | 1 ID (`PROD-001`) | `200` com 1 entrada | RN-20 | VL | P2 | API |
| CT-CAT-014 | Limite superior do lote | 50 IDs distintos (PROD-001..006 + 44 inexistentes) | `200`; 6 em `prices`; 44 em `notFound` | RN-20 | VL | P1 | API |
| CT-CAT-015 | Acima do limite do lote | 51 IDs distintos | `400 VALIDATION_ERROR` com envelope | RN-20 | VL | P1 | API |
| CT-CAT-016 | Lote vazio ou ausente | `{"productIds": []}`, `{"productIds": null}`, `{}` | `400 VALIDATION_ERROR` | RN-20 | VL | P1 | API |
| CT-CAT-017 | ID em branco no lote | `["PROD-001", ""]`, `["PROD-001", "   "]` | `400 VALIDATION_ERROR` | RN-20 | PE | P2 | API |
| CT-CAT-018 | IDs duplicados no lote | `["PROD-001", "PROD-001"]` | `400 VALIDATION_ERROR` | RN-20, Q-04, H-06 | PE | P3 | API |
| CT-CAT-019 | Tipos inválidos no lote | `{"productIds": "PROD-001"}`, `{"productIds": [1, 2]}`, `{"productIds": [{"$ne": null}]}` | `400 VALIDATION_ERROR` com envelope. Nunca `500` nem lista de produtos | RN-20, RN-25, NF-03 | SE | P2 | API |
| CT-CAT-020 | JSON malformado | `{"productIds": [` | `400 VALIDATION_ERROR` com envelope | RN-20, RN-23 | SE | P2 | API |
| CT-CAT-021 | Catálogo é somente leitura | `POST /products`, `PUT /products/PROD-001`, `PATCH /products/PROD-001`, `DELETE /products/PROD-001` | `405` (ou `404`) com envelope; nenhuma alteração (o `GET` posterior retorna os dados da massa). Nunca `2xx` nem `500` | RN-21, RN-26, H-02 | SE | P2 | API |
| CT-CAT-022 | Contrato da consulta em lote | Lote do CT-CAT-011 | Corpo valida o JSON Schema de lote (`prices[]`, `notFound[]`, tipos, datas ISO-8601) | RN-20 | CO | P1 | API |
