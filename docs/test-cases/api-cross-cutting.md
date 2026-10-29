# Casos de Teste — Regras Transversais das APIs

Regras que valem para **os dois serviços**. Os casos são parametrizados por serviço (`order-service` :8080 e `catalog-service` :8081).

| ID | Cenário | Entrada / pré-condição | Resultado esperado | Ref. | Técnica | Prio | Nível |
|---|---|---|---|---|---|---|---|
| CT-TRV-001 | Envelope de erro em **todos** os códigos produzíveis | Um disparador para cada erro: 400 (validação e JSON), 404 (pedido e produto), 413, 422 (3 códigos), 503, 504 | Corpo valida o JSON Schema do envelope (`error.code` string, `error.message` string, `error.details` array, `requestId` string); `code` pertence à lista documentada em `arch` | RN-23, H-11 | CO | P1 | API+FI |
| CT-TRV-002 | Erros não vazam detalhes de implementação | As mesmas respostas do CT-TRV-001 + entradas que provocam erro inesperado (ex.: CT-ORD-062) | Nenhum corpo contém stack trace, nome de classe Java (`com.stefanini`, `java.`, `org.springframework`), SQL ou caminho de arquivo | RN-23, NF-03 | SE | P1 | API |
| CT-TRV-003 | `X-Request-Id` **gerado** quando ausente | Requisições sem o header: sucesso (`GET`) e erro (`404`) | Header `X-Request-Id` presente e não vazio (UUID). Em erro, `requestId` do envelope = header. Valores distintos entre requisições | RN-24 | CO | P1 | API |
| CT-TRV-004 | `X-Request-Id` do cliente **ecoado** | Header `X-Request-Id: trv-<runId>` em sucesso e em erro | Resposta com o mesmo valor no header. Em erro, `requestId` do envelope = `trv-<runId>` | RN-24 | CO | P1 | API |
| CT-TRV-005 | `X-Request-Id` em branco é substituído | Header `X-Request-Id: "   "` | Header de resposta com um valor gerado, não vazio | RN-24 | VL | P3 | API |
| CT-TRV-006 | Rota inexistente | `GET /api/v1/unknown`, `GET /api/v2/orders`, `GET /api/v1/products` (sem id) | `404` com envelope. Nunca `500` | RN-25, H-02 | SE | P2 | API |
| CT-TRV-007 | Método não suportado (nenhuma operação destrutiva exposta) | `PUT`, `PATCH` e `DELETE` em `/api/v1/orders` e `/api/v1/orders/{id}` | `405` com envelope; o pedido continua existindo e inalterado. Nunca `2xx` nem `500` | RN-25, RN-26, H-02 | SE | P2 | API |
| CT-TRV-008 | Saúde dos serviços | `GET /actuator/health` | `200`; `status` `UP` | RN-27 | PE | P1 | API |
| CT-TRV-009 | Actuator expõe apenas `health` e `info` | `GET /actuator/env`, `/actuator/beans`, `/actuator/heapdump`, `/actuator/configprops` | `404` (não expostos) | RN-27, NF-03, H-02 | SE | P2 | API |
| CT-TRV-010 | Tipo de conteúdo das respostas | Respostas de sucesso, de erro e de `413` | `Content-Type` `application/json` (com ou sem `charset`) | RN-15, RN-23 | CO | P2 | API |
| CT-TRV-011 | OpenAPI publicado coerente com a documentação | `GET /v3/api-docs` dos dois serviços | Os caminhos documentados em `api` existem no OpenAPI, com os métodos correspondentes (verificação de *drift* de contrato) | RN-15, RN-19, RN-20 | CO | P3 | API |
