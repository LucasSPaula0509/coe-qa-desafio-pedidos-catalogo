# Matriz de Rastreabilidade

> Gerada por `node scripts/traceability.mjs` a partir de `specs/` e `docs/test-cases/`. **Não edite à mão.** O script também valida a consistência dos IDs (ADR-QA-005).

## Resumo

| Indicador | Valor |
|---|---|
| Regras funcionais (RN) | 34 |
| Requisitos não funcionais (NF) | 4 |
| Questões (Q) / suspeitas (H) | 12 / 18 |
| Casos de teste | 183 |
| Cobertura de requisitos (RN/NF com ≥ 1 caso) | 38/38 (100%) |
| Casos automatizados | 167/183 (91%) |

### Casos por área e prioridade

| Área | Arquivo | Casos | P1 / P2 / P3 |
|---|---|---|---|
| API de Catálogo | [api-catalog.md](test-cases/api-catalog.md) | 21 | 12 / 7 / 2 |
| Transversais | [api-cross-cutting.md](test-cases/api-cross-cutting.md) | 11 | 5 / 4 / 2 |
| API de Pedidos | [api-orders.md](test-cases/api-orders.md) | 64 | 38 / 23 / 3 |
| Integração | [integration.md](test-cases/integration.md) | 15 | 7 / 8 / 0 |
| Performance | [performance.md](test-cases/performance.md) | 8 | 2 / 3 / 3 |
| Segurança | [security.md](test-cases/security.md) | 16 | 4 / 7 / 5 |
| Interface web | [web-e2e.md](test-cases/web-e2e.md) | 48 | 14 / 23 / 11 |
| **Total** | | **183** | **82 / 75 / 26** |

### Casos por nível de execução

| Nível | Casos |
|---|---|
| API | 85 |
| API+DB | 16 |
| API+FI | 10 |
| WEB | 42 |
| WEB+MOCK | 5 |
| ESTÁTICO | 1 |
| SEC | 15 |
| DAST | 1 |
| PERF | 8 |

## Requisitos → casos

| Requisito | Descrição | Casos | P1 / P2 / P3 |
|---|---|---|---|
| RN-01 | Dados do cliente | [CT-ORD-018](test-cases/api-orders.md), [CT-ORD-020](test-cases/api-orders.md), [CT-ORD-021](test-cases/api-orders.md), [CT-ORD-022](test-cases/api-orders.md), [CT-ORD-023](test-cases/api-orders.md), [CT-ORD-024](test-cases/api-orders.md), [CT-ORD-025](test-cases/api-orders.md), [CT-ORD-026](test-cases/api-orders.md), [CT-ORD-027](test-cases/api-orders.md), [CT-ORD-028](test-cases/api-orders.md), [CT-INT-011](test-cases/integration.md), [CT-WEB-032](test-cases/web-e2e.md), [CT-WEB-033](test-cases/web-e2e.md) | 6 / 7 / 0 |
| RN-02 | Quantidade de itens | [CT-ORD-030](test-cases/api-orders.md), [CT-ORD-031](test-cases/api-orders.md), [CT-ORD-032](test-cases/api-orders.md), [CT-ORD-033](test-cases/api-orders.md), [CT-INT-012](test-cases/integration.md), [CT-WEB-041](test-cases/web-e2e.md) | 5 / 1 / 0 |
| RN-03 | Item | [CT-ORD-012](test-cases/api-orders.md), [CT-ORD-034](test-cases/api-orders.md), [CT-ORD-035](test-cases/api-orders.md), [CT-ORD-036](test-cases/api-orders.md), [CT-ORD-037](test-cases/api-orders.md), [CT-ORD-038](test-cases/api-orders.md), [CT-INT-011](test-cases/integration.md), [CT-WEB-038](test-cases/web-e2e.md) | 3 / 5 / 0 |
| RN-04 | Produto repetido | [CT-ORD-039](test-cases/api-orders.md), [CT-ORD-040](test-cases/api-orders.md), [CT-ORD-041](test-cases/api-orders.md), [CT-INT-011](test-cases/integration.md), [CT-WEB-037](test-cases/web-e2e.md) | 1 / 3 / 1 |
| RN-05 | Cliente não define preço | [CT-ORD-015](test-cases/api-orders.md), [CT-INT-001](test-cases/integration.md), [CT-SEC-001](test-cases/security.md) | 3 / 0 / 0 |
| RN-06 | Corpo da requisição | [CT-ORD-042](test-cases/api-orders.md), [CT-ORD-043](test-cases/api-orders.md), [CT-ORD-045](test-cases/api-orders.md), [CT-ORD-046](test-cases/api-orders.md), [CT-SEC-005](test-cases/security.md) | 2 / 3 / 0 |
| RN-07 | All-or-nothing | [CT-ORD-032](test-cases/api-orders.md), [CT-ORD-033](test-cases/api-orders.md), [CT-ORD-038](test-cases/api-orders.md), [CT-ORD-041](test-cases/api-orders.md), [CT-ORD-050](test-cases/api-orders.md), [CT-ORD-051](test-cases/api-orders.md), [CT-ORD-052](test-cases/api-orders.md), [CT-ORD-053](test-cases/api-orders.md), [CT-ORD-054](test-cases/api-orders.md), [CT-ORD-055](test-cases/api-orders.md), [CT-ORD-056](test-cases/api-orders.md), [CT-INT-003](test-cases/integration.md), [CT-INT-006](test-cases/integration.md), [CT-INT-015](test-cases/integration.md), [CT-WEB-034](test-cases/web-e2e.md), [CT-WEB-035](test-cases/web-e2e.md), [CT-WEB-036](test-cases/web-e2e.md) | 12 / 4 / 1 |
| RN-08 | Preço vigente | [CT-CAT-002](test-cases/api-catalog.md), [CT-CAT-004](test-cases/api-catalog.md), [CT-CAT-005](test-cases/api-catalog.md), [CT-CAT-011](test-cases/api-catalog.md), [CT-ORD-014](test-cases/api-orders.md), [CT-ORD-052](test-cases/api-orders.md), [CT-ORD-053](test-cases/api-orders.md), [CT-INT-001](test-cases/integration.md), [CT-INT-002](test-cases/integration.md), [CT-WEB-021](test-cases/web-e2e.md), [CT-WEB-036](test-cases/web-e2e.md) | 11 / 0 / 0 |
| RN-09 | Integração em lote | [CT-ORD-033](test-cases/api-orders.md), [CT-INT-004](test-cases/integration.md), [CT-INT-005](test-cases/integration.md), [CT-PRF-006](test-cases/performance.md) | 1 / 2 / 1 |
| RN-10 | Resiliência | [CT-INT-006](test-cases/integration.md), [CT-INT-007](test-cases/integration.md), [CT-INT-008](test-cases/integration.md), [CT-INT-009](test-cases/integration.md), [CT-INT-015](test-cases/integration.md), [CT-WEB-043](test-cases/web-e2e.md) | 3 / 3 / 0 |
| RN-11 | Snapshot | [CT-ORD-017](test-cases/api-orders.md), [CT-ORD-079](test-cases/api-orders.md), [CT-INT-001](test-cases/integration.md), [CT-INT-002](test-cases/integration.md), [CT-INT-003](test-cases/integration.md), [CT-INT-010](test-cases/integration.md), [CT-PRF-007](test-cases/performance.md) | 3 / 3 / 1 |
| RN-12 | Totais de item e subtotal | [CT-ORD-001](test-cases/api-orders.md), [CT-ORD-002](test-cases/api-orders.md), [CT-ORD-007](test-cases/api-orders.md), [CT-ORD-012](test-cases/api-orders.md), [CT-ORD-013](test-cases/api-orders.md), [CT-INT-014](test-cases/integration.md) | 3 / 3 / 0 |
| RN-13 | Desconto | [CT-ORD-001](test-cases/api-orders.md), [CT-ORD-002](test-cases/api-orders.md), [CT-ORD-003](test-cases/api-orders.md), [CT-ORD-004](test-cases/api-orders.md), [CT-ORD-005](test-cases/api-orders.md), [CT-ORD-006](test-cases/api-orders.md), [CT-ORD-007](test-cases/api-orders.md), [CT-ORD-008](test-cases/api-orders.md), [CT-ORD-009](test-cases/api-orders.md), [CT-ORD-010](test-cases/api-orders.md), [CT-ORD-011](test-cases/api-orders.md), [CT-ORD-012](test-cases/api-orders.md), [CT-ORD-013](test-cases/api-orders.md), [CT-INT-014](test-cases/integration.md), [CT-WEB-030](test-cases/web-e2e.md), [CT-WEB-031](test-cases/web-e2e.md) | 9 / 6 / 1 |
| RN-14 | Status inicial | [CT-ORD-001](test-cases/api-orders.md), [CT-ORD-015](test-cases/api-orders.md) | 2 / 0 / 0 |
| RN-15 | Resposta de criação | [CT-TRV-010](test-cases/api-cross-cutting.md), [CT-TRV-011](test-cases/api-cross-cutting.md), [CT-ORD-016](test-cases/api-orders.md), [CT-INT-013](test-cases/integration.md), [CT-INT-014](test-cases/integration.md), [CT-SEC-013](test-cases/security.md) | 2 / 2 / 2 |
| RN-16 | Detalhe | [CT-ORD-017](test-cases/api-orders.md), [CT-ORD-060](test-cases/api-orders.md), [CT-ORD-061](test-cases/api-orders.md), [CT-INT-010](test-cases/integration.md), [CT-INT-013](test-cases/integration.md), [CT-WEB-052](test-cases/web-e2e.md) | 5 / 1 / 0 |
| RN-17 | Listagem paginada | [CT-ORD-070](test-cases/api-orders.md), [CT-ORD-071](test-cases/api-orders.md), [CT-ORD-072](test-cases/api-orders.md), [CT-ORD-073](test-cases/api-orders.md), [CT-ORD-074](test-cases/api-orders.md), [CT-ORD-075](test-cases/api-orders.md), [CT-ORD-079](test-cases/api-orders.md), [CT-INT-010](test-cases/integration.md), [CT-INT-013](test-cases/integration.md), [CT-PRF-008](test-cases/performance.md), [CT-WEB-050](test-cases/web-e2e.md), [CT-WEB-051](test-cases/web-e2e.md), [CT-WEB-058](test-cases/web-e2e.md) | 5 / 7 / 1 |
| RN-18 | Filtro por status | [CT-ORD-076](test-cases/api-orders.md), [CT-ORD-077](test-cases/api-orders.md), [CT-INT-013](test-cases/integration.md) | 3 / 0 / 0 |
| RN-19 | Consulta individual | [CT-CAT-001](test-cases/api-catalog.md), [CT-CAT-002](test-cases/api-catalog.md), [CT-CAT-003](test-cases/api-catalog.md), [CT-CAT-004](test-cases/api-catalog.md), [CT-CAT-005](test-cases/api-catalog.md), [CT-CAT-006](test-cases/api-catalog.md), [CT-CAT-007](test-cases/api-catalog.md), [CT-CAT-008](test-cases/api-catalog.md), [CT-TRV-011](test-cases/api-cross-cutting.md) | 6 / 1 / 2 |
| RN-20 | Consulta em lote | [CT-CAT-010](test-cases/api-catalog.md), [CT-CAT-011](test-cases/api-catalog.md), [CT-CAT-012](test-cases/api-catalog.md), [CT-CAT-013](test-cases/api-catalog.md), [CT-CAT-014](test-cases/api-catalog.md), [CT-CAT-015](test-cases/api-catalog.md), [CT-CAT-016](test-cases/api-catalog.md), [CT-CAT-017](test-cases/api-catalog.md), [CT-CAT-018](test-cases/api-catalog.md), [CT-CAT-019](test-cases/api-catalog.md), [CT-CAT-020](test-cases/api-catalog.md), [CT-CAT-022](test-cases/api-catalog.md), [CT-TRV-011](test-cases/api-cross-cutting.md), [CT-INT-012](test-cases/integration.md), [CT-SEC-003](test-cases/security.md), [CT-WEB-026](test-cases/web-e2e.md) | 8 / 6 / 2 |
| RN-21 | Somente leitura | [CT-CAT-021](test-cases/api-catalog.md) | 0 / 1 / 0 |
| RN-22 | Massa inicial | [CT-CAT-001](test-cases/api-catalog.md), [CT-CAT-003](test-cases/api-catalog.md), [CT-CAT-011](test-cases/api-catalog.md) | 2 / 1 / 0 |
| RN-23 | Envelope de erro único | [CT-CAT-006](test-cases/api-catalog.md), [CT-CAT-020](test-cases/api-catalog.md), [CT-TRV-001](test-cases/api-cross-cutting.md), [CT-TRV-002](test-cases/api-cross-cutting.md), [CT-TRV-010](test-cases/api-cross-cutting.md), [CT-ORD-042](test-cases/api-orders.md), [CT-ORD-046](test-cases/api-orders.md), [CT-ORD-061](test-cases/api-orders.md), [CT-SEC-002](test-cases/security.md), [CT-SEC-007](test-cases/security.md) | 7 / 3 / 0 |
| RN-24 | Identificador da requisição | [CT-TRV-003](test-cases/api-cross-cutting.md), [CT-TRV-004](test-cases/api-cross-cutting.md), [CT-TRV-005](test-cases/api-cross-cutting.md), [CT-INT-005](test-cases/integration.md), [CT-SEC-009](test-cases/security.md) | 2 / 1 / 2 |
| RN-25 | Semântica HTTP | [CT-CAT-007](test-cases/api-catalog.md), [CT-CAT-019](test-cases/api-catalog.md), [CT-TRV-006](test-cases/api-cross-cutting.md), [CT-TRV-007](test-cases/api-cross-cutting.md), [CT-ORD-044](test-cases/api-orders.md), [CT-ORD-062](test-cases/api-orders.md), [CT-ORD-078](test-cases/api-orders.md), [CT-INT-012](test-cases/integration.md), [CT-SEC-006](test-cases/security.md) | 1 / 7 / 1 |
| RN-26 | Proteções operacionais sem autenticação | [CT-CAT-021](test-cases/api-catalog.md), [CT-TRV-007](test-cases/api-cross-cutting.md), [CT-SEC-012](test-cases/security.md), [CT-SEC-013](test-cases/security.md) | 0 / 3 / 1 |
| RN-27 | Saúde | [CT-TRV-008](test-cases/api-cross-cutting.md), [CT-TRV-009](test-cases/api-cross-cutting.md), [CT-SEC-010](test-cases/security.md) | 1 / 2 / 0 |
| RN-28 | Navegação | [CT-WEB-001](test-cases/web-e2e.md), [CT-WEB-002](test-cases/web-e2e.md), [CT-WEB-003](test-cases/web-e2e.md), [CT-WEB-057](test-cases/web-e2e.md) | 1 / 2 / 1 |
| RN-29 | Painel | [CT-WEB-010](test-cases/web-e2e.md), [CT-WEB-011](test-cases/web-e2e.md), [CT-WEB-012](test-cases/web-e2e.md), [CT-WEB-013](test-cases/web-e2e.md) | 1 / 1 / 2 |
| RN-30 | Catálogo | [CT-WEB-020](test-cases/web-e2e.md), [CT-WEB-021](test-cases/web-e2e.md), [CT-WEB-022](test-cases/web-e2e.md), [CT-WEB-023](test-cases/web-e2e.md), [CT-WEB-024](test-cases/web-e2e.md), [CT-WEB-025](test-cases/web-e2e.md), [CT-WEB-026](test-cases/web-e2e.md), [CT-WEB-063](test-cases/web-e2e.md) | 3 / 4 / 1 |
| RN-31 | Novo pedido | [CT-WEB-030](test-cases/web-e2e.md), [CT-WEB-031](test-cases/web-e2e.md), [CT-WEB-032](test-cases/web-e2e.md), [CT-WEB-033](test-cases/web-e2e.md), [CT-WEB-034](test-cases/web-e2e.md), [CT-WEB-035](test-cases/web-e2e.md), [CT-WEB-036](test-cases/web-e2e.md), [CT-WEB-037](test-cases/web-e2e.md), [CT-WEB-038](test-cases/web-e2e.md), [CT-WEB-039](test-cases/web-e2e.md), [CT-WEB-040](test-cases/web-e2e.md), [CT-WEB-041](test-cases/web-e2e.md), [CT-WEB-042](test-cases/web-e2e.md), [CT-WEB-043](test-cases/web-e2e.md), [CT-WEB-044](test-cases/web-e2e.md), [CT-WEB-045](test-cases/web-e2e.md), [CT-WEB-063](test-cases/web-e2e.md) | 6 / 10 / 1 |
| RN-32 | Pedidos | [CT-WEB-030](test-cases/web-e2e.md), [CT-WEB-050](test-cases/web-e2e.md), [CT-WEB-051](test-cases/web-e2e.md), [CT-WEB-058](test-cases/web-e2e.md), [CT-WEB-052](test-cases/web-e2e.md), [CT-WEB-053](test-cases/web-e2e.md), [CT-WEB-054](test-cases/web-e2e.md), [CT-WEB-055](test-cases/web-e2e.md), [CT-WEB-056](test-cases/web-e2e.md) | 3 / 4 / 2 |
| RN-33 | Idiomas | [CT-WEB-001](test-cases/web-e2e.md), [CT-WEB-057](test-cases/web-e2e.md), [CT-WEB-060](test-cases/web-e2e.md), [CT-WEB-061](test-cases/web-e2e.md), [CT-WEB-062](test-cases/web-e2e.md), [CT-WEB-063](test-cases/web-e2e.md), [CT-WEB-064](test-cases/web-e2e.md), [CT-WEB-065](test-cases/web-e2e.md) | 2 / 4 / 2 |
| RN-34 | Erros na interface | [CT-WEB-057](test-cases/web-e2e.md), [CT-WEB-014](test-cases/web-e2e.md), [CT-WEB-023](test-cases/web-e2e.md), [CT-WEB-042](test-cases/web-e2e.md), [CT-WEB-043](test-cases/web-e2e.md), [CT-WEB-053](test-cases/web-e2e.md), [CT-WEB-056](test-cases/web-e2e.md), [CT-WEB-065](test-cases/web-e2e.md) | 0 / 7 / 1 |
| NF-01 | Desempenho (SLO proposto) | [CT-PRF-001](test-cases/performance.md), [CT-PRF-002](test-cases/performance.md), [CT-PRF-003](test-cases/performance.md), [CT-PRF-006](test-cases/performance.md), [CT-PRF-008](test-cases/performance.md) | 2 / 2 / 1 |
| NF-02 | Estabilidade sob carga | [CT-PRF-003](test-cases/performance.md), [CT-PRF-004](test-cases/performance.md), [CT-PRF-005](test-cases/performance.md), [CT-PRF-007](test-cases/performance.md) | 0 / 2 / 2 |
| NF-03 | Segurança de entrada e exposição | [CT-CAT-019](test-cases/api-catalog.md), [CT-TRV-002](test-cases/api-cross-cutting.md), [CT-TRV-009](test-cases/api-cross-cutting.md), [CT-SEC-002](test-cases/security.md), [CT-SEC-003](test-cases/security.md), [CT-SEC-004](test-cases/security.md), [CT-SEC-006](test-cases/security.md), [CT-SEC-007](test-cases/security.md), [CT-SEC-008](test-cases/security.md), [CT-SEC-009](test-cases/security.md), [CT-SEC-010](test-cases/security.md), [CT-SEC-011](test-cases/security.md), [CT-SEC-014](test-cases/security.md), [CT-SEC-015](test-cases/security.md), [CT-SEC-016](test-cases/security.md) | 4 / 7 / 4 |
| NF-04 | Acessibilidade básica | [CT-WEB-070](test-cases/web-e2e.md), [CT-WEB-071](test-cases/web-e2e.md) | 0 / 0 / 2 |

## Questões e suspeitas → casos

Uma suspeita (H) só vira Issue quando o caso indicado a reproduz.

| ID | Descrição | Casos |
|---|---|---|
| Q-01 | Pedido com produtos inválidos de tipos diferentes (ex.: inexistente + inativo): qual código prevalece? A do… | [CT-ORD-056](test-cases/api-orders.md) |
| Q-02 | O PDF diz "nome, documento e e-mail válido". O documento precisa ser CPF/CNPJ válido? A API só exige "obrig… | [CT-ORD-022](test-cases/api-orders.md), [CT-ORD-027](test-cases/api-orders.md) |
| Q-03 | Qual o critério de "e-mail com formato válido"? Casos limítrofes como user@host (sem TLD) não estão definidos. | [CT-ORD-024](test-cases/api-orders.md), [CT-ORD-025](test-cases/api-orders.md) |
| Q-04 | A frase "max 50 IDs, sem duplicatas" no lote do catálogo é restrição de entrada (→ 400) ou descrição do res… | [CT-CAT-018](test-cases/api-catalog.md) |
| Q-05 | quantity é "inteiro". Valores como 1.5, "2" (string) ou true devem ser aceitos com conversão? | [CT-ORD-036](test-cases/api-orders.md) |
| Q-06 | Não há SLA, volume esperado nem perfil de uso documentados. | [CT-PRF-001](test-cases/performance.md), [CT-PRF-002](test-cases/performance.md), [CT-PRF-003](test-cases/performance.md), [CT-PRF-004](test-cases/performance.md), [CT-PRF-005](test-cases/performance.md), [CT-PRF-006](test-cases/performance.md), [CT-PRF-007](test-cases/performance.md), [CT-PRF-008](test-cases/performance.md) |
| Q-07 | O pedido aceita até 100 itens (RN-02), mas o lote do catálogo aceita no máximo 50 IDs (RN-20), e a criação… | [CT-ORD-032](test-cases/api-orders.md), [CT-INT-012](test-cases/integration.md) |
| Q-08 | O limite é de "1 MB". Em bytes, isso é 1.000.000 ou 1.048.576? E "acima de" inclui o próprio limite? | [CT-ORD-045](test-cases/api-orders.md), [CT-ORD-046](test-cases/api-orders.md) |
| Q-09 | Com o seed atual, não é possível atingir exatamente R$ 1.000,00 de subtotal (preços 3500,00 / 499,90 / 649,… | [CT-ORD-003](test-cases/api-orders.md), [CT-ORD-004](test-cases/api-orders.md), [CT-ORD-005](test-cases/api-orders.md), [CT-ORD-006](test-cases/api-orders.md) |
| Q-10 | Página além do total (ex.: page=99999): resposta esperada? | [CT-ORD-075](test-cases/api-orders.md) |
| Q-12 | Catálogo inacessível sem resposta à conexão: "indisponível" (503, PDF §2.2.2) ou "não respondeu no tempo li… | [CT-INT-015](test-cases/integration.md) |
| Q-11 | O PDF chama a tela inicial de "Painel". A UI exibe "Dashboard" em pt-BR e en e "Panel" em es. | [CT-WEB-001](test-cases/web-e2e.md) |
| H-01 | Pedido com 51 a 100 produtos distintos falha com 503 CATALOG_UNAVAILABLE. | [CT-ORD-032](test-cases/api-orders.md), [CT-INT-012](test-cases/integration.md) |
| H-02 | Erros de entrada tratados pelo framework viram 500 INTERNAL_ERROR: id que não é UUID, page/size não numéric… | [CT-CAT-021](test-cases/api-catalog.md), [CT-TRV-006](test-cases/api-cross-cutting.md), [CT-TRV-007](test-cases/api-cross-cutting.md), [CT-TRV-009](test-cases/api-cross-cutting.md), [CT-ORD-044](test-cases/api-orders.md), [CT-ORD-062](test-cases/api-orders.md), [CT-ORD-078](test-cases/api-orders.md) |
| H-03 | Tipos incompatíveis são convertidos silenciosamente em vez de rejeitados: quantity 1.5 → 1 e "2" → 2; produ… | [CT-CAT-019](test-cases/api-catalog.md), [CT-ORD-036](test-cases/api-orders.md) |
| H-04 | Corpo acima de 1 MB enviado com Transfer-Encoding: chunked (sem Content-Length) não é rejeitado com 413. | [CT-SEC-005](test-cases/security.md) |
| H-05 | A documentação diverge da massa inicial e tem uma lacuna de vigência no exemplo. | [CT-CAT-001](test-cases/api-catalog.md) |
| H-06 | O lote do catálogo aceita IDs duplicados (deduplicação silenciosa). | [CT-CAT-018](test-cases/api-catalog.md) |
| H-07 | X-Request-Id enviado pelo cliente é aceito sem limite de tamanho nem validação de formato e é ecoado na res… | [CT-SEC-009](test-cases/security.md) |
| H-08 | O front-end (nginx) não envia cabeçalhos de segurança (CSP, X-Frame-Options, X-Content-Type-Options, Referr… | [CT-SEC-008](test-cases/security.md) |
| H-09 | Campos nulos são omitidos das respostas em vez de devolvidos como null: currentPrice sem preço vigente e va… | [CT-CAT-001](test-cases/api-catalog.md), [CT-CAT-004](test-cases/api-catalog.md), [CT-CAT-005](test-cases/api-catalog.md), [CT-CAT-008](test-cases/api-catalog.md), [CT-CAT-022](test-cases/api-catalog.md) |
| H-10 | O createdAt devolvido na criação difere do persistido e devolvido nas consultas. | [CT-ORD-017](test-cases/api-orders.md) |
| H-11 | Falha de IO na resposta do catálogo (timeout de leitura ou conexão interrompida) resulta em 500 INTERNAL_ER… | [CT-TRV-001](test-cases/api-cross-cutting.md), [CT-INT-006](test-cases/integration.md), [CT-INT-007](test-cases/integration.md) |
| H-12 | Requisições rejeitadas pelo servidor web (Tomcat), antes da aplicação, respondem página HTML sem envelope e… | [CT-CAT-007](test-cases/api-catalog.md) |
| H-13 | O front-end deixa de alcançar o order-service após um redeploy dele: /api/orders responde 502 (ou é encamin… | [CT-WEB-056](test-cases/web-e2e.md) |
| H-14 | O diálogo "Descartar pedido?" é exibido vazio (sem título, mensagem e rótulos dos botões) e confirmar não s… | [CT-WEB-044](test-cases/web-e2e.md) |
| H-15 | A paginação da listagem transborda: com muitas páginas, os primeiros links (e a seta "anterior") ficam fora… | [CT-WEB-058](test-cases/web-e2e.md) |
| H-16 | No detalhe do pedido, a região rolável da tabela de itens não é acessível por teclado. | [CT-WEB-070](test-cases/web-e2e.md) |
| H-17 | Em espanhol, valores de 4 dígitos são agrupados (3.500,00 BRL), divergindo da regra do CLDR para es (3500,0… | [CT-WEB-061](test-cases/web-e2e.md) |
| H-18 | O front-end fica inoperante quando servido por HTTP fora de localhost (ex.: homologação ou intranet): exibe… | [CT-WEB-057](test-cases/web-e2e.md) |

## Casos → requisitos e automação

| Caso | Cenário | Requisitos | Prio | Nível | Automação |
|---|---|---|---|---|---|
| [CT-CAT-001](test-cases/api-catalog.md) | Produto ativo com preço vigente | RN-19, RN-22, H-05, H-09 | P1 | API | ✅ Automatizado |
| [CT-CAT-002](test-cases/api-catalog.md) | Produto com histórico de preços | RN-08, RN-19 | P1 | API | ✅ Automatizado |
| [CT-CAT-003](test-cases/api-catalog.md) | Produto inativo continua consultável | RN-19, RN-22 | P2 | API | ✅ Automatizado |
| [CT-CAT-004](test-cases/api-catalog.md) | Preço apenas futuro | RN-08, RN-19, H-09 | P1 | API | ✅ Automatizado |
| [CT-CAT-005](test-cases/api-catalog.md) | Preço expirado | RN-08, RN-19, H-09 | P1 | API | ✅ Automatizado |
| [CT-CAT-006](test-cases/api-catalog.md) | Produto inexistente | RN-19, RN-23 | P1 | API | ✅ Automatizado |
| [CT-CAT-007](test-cases/api-catalog.md) | Identificadores incomuns | RN-19, RN-25, H-12 | P3 | API | ✅ Automatizado |
| [CT-CAT-008](test-cases/api-catalog.md) | Contrato da consulta individual | RN-19, H-09 | P1 | API | ✅ Automatizado |
| [CT-CAT-010](test-cases/api-catalog.md) | Lote de produtos válidos | RN-20 | P1 | API | ✅ Automatizado |
| [CT-CAT-011](test-cases/api-catalog.md) | Lote com todos os cenários da massa + inexistente | RN-20, RN-08, RN-22 | P1 | API | ✅ Automatizado |
| [CT-CAT-012](test-cases/api-catalog.md) | Lote só com inexistentes | RN-20 | P2 | API | ✅ Automatizado |
| [CT-CAT-013](test-cases/api-catalog.md) | Limite inferior do lote | RN-20 | P2 | API | ✅ Automatizado |
| [CT-CAT-014](test-cases/api-catalog.md) | Limite superior do lote | RN-20 | P1 | API | ✅ Automatizado |
| [CT-CAT-015](test-cases/api-catalog.md) | Acima do limite do lote | RN-20 | P1 | API | ✅ Automatizado |
| [CT-CAT-016](test-cases/api-catalog.md) | Lote vazio ou ausente | RN-20 | P1 | API | ✅ Automatizado |
| [CT-CAT-017](test-cases/api-catalog.md) | ID em branco no lote | RN-20 | P2 | API | ✅ Automatizado |
| [CT-CAT-018](test-cases/api-catalog.md) | IDs duplicados no lote | RN-20, Q-04, H-06 | P3 | API | ✅ Automatizado |
| [CT-CAT-019](test-cases/api-catalog.md) | Tipos inválidos no lote | RN-20, RN-25, NF-03, H-03 | P2 | API | ✅ Automatizado |
| [CT-CAT-020](test-cases/api-catalog.md) | JSON malformado | RN-20, RN-23 | P2 | API | ✅ Automatizado |
| [CT-CAT-021](test-cases/api-catalog.md) | Catálogo é somente leitura | RN-21, RN-26, H-02 | P2 | API | ✅ Automatizado |
| [CT-CAT-022](test-cases/api-catalog.md) | Contrato da consulta em lote | RN-20, H-09 | P1 | API | ✅ Automatizado |
| [CT-TRV-001](test-cases/api-cross-cutting.md) | Envelope de erro em todos os códigos produzíveis | RN-23, H-11 | P1 | API+FI | ✅ Automatizado |
| [CT-TRV-002](test-cases/api-cross-cutting.md) | Erros não vazam detalhes de implementação | RN-23, NF-03 | P1 | API | ✅ Automatizado |
| [CT-TRV-003](test-cases/api-cross-cutting.md) | X-Request-Id gerado quando ausente | RN-24 | P1 | API | ✅ Automatizado |
| [CT-TRV-004](test-cases/api-cross-cutting.md) | X-Request-Id do cliente ecoado | RN-24 | P1 | API | ✅ Automatizado |
| [CT-TRV-005](test-cases/api-cross-cutting.md) | X-Request-Id em branco é substituído | RN-24 | P3 | API | ✅ Automatizado |
| [CT-TRV-006](test-cases/api-cross-cutting.md) | Rota inexistente | RN-25, H-02 | P2 | API | ✅ Automatizado |
| [CT-TRV-007](test-cases/api-cross-cutting.md) | Método não suportado (nenhuma operação destrutiva exposta) | RN-25, RN-26, H-02 | P2 | API | ✅ Automatizado |
| [CT-TRV-008](test-cases/api-cross-cutting.md) | Saúde dos serviços | RN-27 | P1 | API | ✅ Automatizado |
| [CT-TRV-009](test-cases/api-cross-cutting.md) | Actuator expõe apenas health e info | RN-27, NF-03, H-02 | P2 | API | ✅ Automatizado |
| [CT-TRV-010](test-cases/api-cross-cutting.md) | Tipo de conteúdo das respostas | RN-15, RN-23 | P2 | API | ✅ Automatizado |
| [CT-TRV-011](test-cases/api-cross-cutting.md) | OpenAPI publicado coerente com a documentação | RN-15, RN-19, RN-20 | P3 | API | ✅ Automatizado |
| [CT-ORD-001](test-cases/api-orders.md) | Pedido válido de 1 item abaixo do mínimo de desconto | RN-12, RN-13, RN-14 | P1 | API | ✅ Automatizado |
| [CT-ORD-002](test-cases/api-orders.md) | Pedido com vários itens e desconto (exemplo oficial da documentação) | RN-12, RN-13 | P1 | API | ✅ Automatizado |
| [CT-ORD-003](test-cases/api-orders.md) | Maior subtotal possível abaixo do mínimo com a massa | RN-13, Q-09 | P1 | API | ✅ Automatizado |
| [CT-ORD-004](test-cases/api-orders.md) | Menor subtotal possível acima do mínimo com a massa | RN-13, Q-09 | P1 | API | ✅ Automatizado |
| [CT-ORD-005](test-cases/api-orders.md) | Subtotal exatamente igual ao mínimo recebe desconto ("a partir de") | RN-13, Q-09 | P1 | API+DB | ✅ Automatizado |
| [CT-ORD-006](test-cases/api-orders.md) | Subtotal 1 centavo abaixo do mínimo não recebe desconto | RN-13, Q-09 | P2 | API+DB | ✅ Automatizado |
| [CT-ORD-007](test-cases/api-orders.md) | Arredondamento HALF_UP do desconto (terceira casa = 5, dígito anterior par, o que distingue HALF_UP de HALF_EVEN e de truncamento) | RN-13, RN-12 | P1 | API+DB | ✅ Automatizado |
| [CT-ORD-008](test-cases/api-orders.md) | Arredondamento para baixo (terceira casa < 5) | RN-13 | P2 | API+DB | ✅ Automatizado |
| [CT-ORD-009](test-cases/api-orders.md) | Sem regra de desconto ativa → desconto zero | RN-13 | P1 | API+DB | ✅ Automatizado |
| [CT-ORD-010](test-cases/api-orders.md) | Mais de uma regra ativa → vale a mais antiga | RN-13 | P2 | API+DB | ✅ Automatizado |
| [CT-ORD-011](test-cases/api-orders.md) | Percentual máximo (100%) → total zero | RN-13 | P3 | API+DB | ✅ Automatizado |
| [CT-ORD-012](test-cases/api-orders.md) | Quantidade máxima em um item | RN-03, RN-12, RN-13 | P2 | API | ✅ Automatizado |
| [CT-ORD-013](test-cases/api-orders.md) | Maior valor agregado possível com a massa (todos os produtos válidos, quantidade máxima) | RN-12, RN-13 | P2 | API | ✅ Automatizado |
| [CT-ORD-014](test-cases/api-orders.md) | Produto com histórico usa o preço da janela vigente | RN-08 | P1 | API | ✅ Automatizado |
| [CT-ORD-015](test-cases/api-orders.md) | Campos de preço, total, status e id enviados pelo cliente são ignorados | RN-05, RN-14 | P1 | API | ✅ Automatizado |
| [CT-ORD-016](test-cases/api-orders.md) | Contrato da resposta de criação | RN-15 | P1 | API | ✅ Automatizado |
| [CT-ORD-017](test-cases/api-orders.md) | Pedido criado fica persistido e íntegro | RN-16, RN-11, H-10 | P1 | API | ✅ Automatizado |
| [CT-ORD-018](test-cases/api-orders.md) | Caracteres especiais e Unicode no cliente | RN-01 | P2 | API | ✅ Automatizado |
| [CT-ORD-020](test-cases/api-orders.md) | Cliente ausente | RN-01 | P1 | API | ✅ Automatizado |
| [CT-ORD-021](test-cases/api-orders.md) | Nome inválido | RN-01 | P1 | API | ✅ Automatizado |
| [CT-ORD-022](test-cases/api-orders.md) | Documento inválido | RN-01, Q-02 | P1 | API | ✅ Automatizado |
| [CT-ORD-023](test-cases/api-orders.md) | E-mail ausente ou vazio | RN-01 | P1 | API | ✅ Automatizado |
| [CT-ORD-024](test-cases/api-orders.md) | E-mail com formato inválido | RN-01, Q-03 | P1 | API | ✅ Automatizado |
| [CT-ORD-025](test-cases/api-orders.md) | E-mail válido com subendereço e subdomínio | RN-01, Q-03 | P2 | API | ✅ Automatizado |
| [CT-ORD-026](test-cases/api-orders.md) | Limite do nome | RN-01 | P2 | API | ✅ Automatizado |
| [CT-ORD-027](test-cases/api-orders.md) | Limite do documento | RN-01, Q-02 | P2 | API | ✅ Automatizado |
| [CT-ORD-028](test-cases/api-orders.md) | Limite do e-mail | RN-01 | P2 | API | ✅ Automatizado |
| [CT-ORD-030](test-cases/api-orders.md) | Pedido sem itens | RN-02 | P1 | API | ✅ Automatizado |
| [CT-ORD-031](test-cases/api-orders.md) | Acima do limite de itens | RN-02 | P1 | API | ✅ Automatizado |
| [CT-ORD-032](test-cases/api-orders.md) | Limite máximo de itens é aceito pela validação | RN-02, RN-07, Q-07, H-01 | P1 | API | ✅ Automatizado |
| [CT-ORD-033](test-cases/api-orders.md) | 50 itens distintos (limite do lote do catálogo) | RN-02, RN-07, RN-09 | P1 | API | ✅ Automatizado |
| [CT-ORD-034](test-cases/api-orders.md) | Limites da quantidade | RN-03 | P1 | API | ✅ Automatizado |
| [CT-ORD-035](test-cases/api-orders.md) | Quantidade ausente | RN-03 | P1 | API | ✅ Automatizado |
| [CT-ORD-036](test-cases/api-orders.md) | Quantidade com tipo não inteiro | RN-03, Q-05, H-03 | P2 | API | ✅ Automatizado |
| [CT-ORD-037](test-cases/api-orders.md) | productId inválido | RN-03 | P1 | API | ✅ Automatizado |
| [CT-ORD-038](test-cases/api-orders.md) | Limite do productId | RN-03, RN-07 | P2 | API | ✅ Automatizado |
| [CT-ORD-039](test-cases/api-orders.md) | Produto repetido | RN-04 | P1 | API | ✅ Automatizado |
| [CT-ORD-040](test-cases/api-orders.md) | Repetição tem precedência sobre a validação de catálogo | RN-04 | P2 | API | ✅ Automatizado |
| [CT-ORD-041](test-cases/api-orders.md) | IDs diferenciam maiúsculas e minúsculas | RN-04, RN-07 | P3 | API | ✅ Automatizado |
| [CT-ORD-042](test-cases/api-orders.md) | JSON malformado | RN-06, RN-23 | P1 | API | ✅ Automatizado |
| [CT-ORD-043](test-cases/api-orders.md) | Corpo vazio | RN-06 | P2 | API | ✅ Automatizado |
| [CT-ORD-044](test-cases/api-orders.md) | Content-Type não suportado | RN-25, H-02 | P2 | API | ✅ Automatizado |
| [CT-ORD-045](test-cases/api-orders.md) | Corpo de exatamente 1 MB é aceito | RN-06, Q-08 | P2 | API | ✅ Automatizado |
| [CT-ORD-046](test-cases/api-orders.md) | Corpo acima de 1 MB é rejeitado | RN-06, RN-23, Q-08 | P1 | API | ✅ Automatizado |
| [CT-ORD-050](test-cases/api-orders.md) | Produto inexistente | RN-07 | P1 | API+DB | ✅ Automatizado |
| [CT-ORD-051](test-cases/api-orders.md) | Produto inativo | RN-07 | P1 | API+DB | ✅ Automatizado |
| [CT-ORD-052](test-cases/api-orders.md) | Produto com preço futuro (sem janela vigente) | RN-07, RN-08 | P1 | API+DB | ✅ Automatizado |
| [CT-ORD-053](test-cases/api-orders.md) | Produto com preço expirado | RN-07, RN-08 | P1 | API+DB | ✅ Automatizado |
| [CT-ORD-054](test-cases/api-orders.md) | Item válido + item inválido → rejeição total | RN-07 | P1 | API+DB | ✅ Automatizado |
| [CT-ORD-055](test-cases/api-orders.md) | Vários inválidos do mesmo tipo | RN-07 | P2 | API+DB | ✅ Automatizado |
| [CT-ORD-056](test-cases/api-orders.md) | Inválidos de tipos diferentes | RN-07, Q-01 | P2 | API+DB | ✅ Automatizado |
| [CT-ORD-060](test-cases/api-orders.md) | Consulta de pedido existente | RN-16 | P1 | API | ✅ Automatizado |
| [CT-ORD-061](test-cases/api-orders.md) | Pedido inexistente | RN-16, RN-23 | P1 | API | ✅ Automatizado |
| [CT-ORD-062](test-cases/api-orders.md) | Identificador com formato inválido | RN-25, H-02 | P2 | API | ✅ Automatizado |
| [CT-ORD-070](test-cases/api-orders.md) | Listagem sem parâmetros | RN-17 | P1 | API | ✅ Automatizado |
| [CT-ORD-071](test-cases/api-orders.md) | Ordenação do mais recente para o mais antigo | RN-17 | P1 | API | ✅ Automatizado |
| [CT-ORD-072](test-cases/api-orders.md) | Navegação entre páginas sem sobreposição | RN-17 | P1 | API | ✅ Automatizado |
| [CT-ORD-073](test-cases/api-orders.md) | Normalização de size | RN-17 | P2 | API | ✅ Automatizado |
| [CT-ORD-074](test-cases/api-orders.md) | Normalização de page negativa | RN-17 | P2 | API | ✅ Automatizado |
| [CT-ORD-075](test-cases/api-orders.md) | Página além do total | RN-17, Q-10 | P3 | API | ✅ Automatizado |
| [CT-ORD-076](test-cases/api-orders.md) | Filtro por status válido | RN-18 | P1 | API | ✅ Automatizado |
| [CT-ORD-077](test-cases/api-orders.md) | Filtro por status inválido | RN-18 | P1 | API | ✅ Automatizado |
| [CT-ORD-078](test-cases/api-orders.md) | Parâmetros de paginação não numéricos | RN-25, H-02 | P2 | API | ✅ Automatizado |
| [CT-ORD-079](test-cases/api-orders.md) | Itens completos na listagem | RN-17, RN-11 | P2 | API | ✅ Automatizado |
| [CT-INT-001](test-cases/integration.md) | Pedido usa nome e preço vigentes do catálogo | RN-05, RN-08, RN-11 | P1 | API | ✅ Automatizado |
| [CT-INT-002](test-cases/integration.md) | Snapshot: mudança de preço e nome no catálogo não altera pedidos existentes | RN-11, RN-08 | P1 | API+DB | ✅ Automatizado |
| [CT-INT-003](test-cases/integration.md) | Produto inativado no catálogo passa a ser recusado sem afetar pedidos antigos | RN-07, RN-11 | P2 | API+DB | ✅ Automatizado |
| [CT-INT-004](test-cases/integration.md) | Uma única chamada em lote por pedido (anti-N+1) | RN-09 | P2 | API+FI | ✅ Automatizado |
| [CT-INT-005](test-cases/integration.md) | Propagação do X-Request-Id até o catálogo | RN-09, RN-24 | P2 | API+FI | ✅ Automatizado |
| [CT-INT-006](test-cases/integration.md) | Catálogo derruba a conexão durante a chamada | RN-10, RN-07, H-11 | P1 | API+FI | ✅ Automatizado |
| [CT-INT-007](test-cases/integration.md) | Catálogo lento além do limite | RN-10, H-11 | P1 | API+FI | ✅ Automatizado |
| [CT-INT-008](test-cases/integration.md) | Catálogo lento dentro do limite | RN-10 | P2 | API+FI | ✅ Automatizado |
| [CT-INT-009](test-cases/integration.md) | Catálogo responde erro 5xx | RN-10 | P2 | API+FI | ✅ Automatizado |
| [CT-INT-010](test-cases/integration.md) | Catálogo fora não afeta leitura de pedidos | RN-11, RN-16, RN-17 | P2 | API+FI | ✅ Automatizado |
| [CT-INT-011](test-cases/integration.md) | Validações de entrada não dependem do catálogo | RN-04, RN-03, RN-01 | P2 | API+FI | ✅ Automatizado |
| [CT-INT-012](test-cases/integration.md) | Pedido com 51 produtos distintos (acima do limite do lote do catálogo) | RN-02, RN-20, RN-25, Q-07, H-01 | P1 | API | ✅ Automatizado |
| [CT-INT-013](test-cases/integration.md) | Jornada de API ponta a ponta | RN-15, RN-16, RN-17, RN-18 | P1 | API | ✅ Automatizado |
| [CT-INT-014](test-cases/integration.md) | Criação concorrente | RN-12, RN-13, RN-15 | P2 | API | ✅ Automatizado |
| [CT-INT-015](test-cases/integration.md) | Catálogo inacessível (destino fora do ar) | RN-10, RN-07, Q-12 | P1 | API+FI | ✅ Automatizado |
| [CT-PRF-001](test-cases/performance.md) | Smoke de performance | NF-01, Q-06 | P1 | PERF | ✅ Automatizado |
| [CT-PRF-002](test-cases/performance.md) | Carga nominal | NF-01, Q-06 | P1 | PERF | ✅ Automatizado |
| [CT-PRF-003](test-cases/performance.md) | Estresse (ponto de saturação) | NF-01, NF-02, Q-06 | P2 | PERF | ✅ Automatizado |
| [CT-PRF-004](test-cases/performance.md) | Pico (spike) | NF-02, Q-06 | P2 | PERF | ✅ Automatizado |
| [CT-PRF-005](test-cases/performance.md) | Resistência (soak) | NF-02, Q-06 | P3 | PERF | ✅ Automatizado |
| [CT-PRF-006](test-cases/performance.md) | Criação independente do número de itens (anti-N+1) | NF-01, RN-09, Q-06 | P3 | PERF | ✅ Automatizado |
| [CT-PRF-007](test-cases/performance.md) | Degradação da dependência | NF-02, RN-11, Q-06 | P3 | PERF | ✅ Automatizado |
| [CT-PRF-008](test-cases/performance.md) | Listagem com volume | NF-01, RN-17, Q-06 | P2 | PERF | ✅ Automatizado |
| [CT-SEC-001](test-cases/security.md) | Manipulação de preço e total pelo cliente (*mass assignment*) | RN-05 | P1 | SEC | ⏳ Planejado |
| [CT-SEC-002](test-cases/security.md) | Injeção SQL em parâmetros do order-service | NF-03, RN-23 | P1 | SEC | ⏳ Planejado |
| [CT-SEC-003](test-cases/security.md) | Injeção NoSQL no catalog-service | NF-03, RN-20 | P1 | SEC | ⏳ Planejado |
| [CT-SEC-004](test-cases/security.md) | XSS armazenado via dados do cliente | NF-03 | P1 | SEC | ⏳ Planejado |
| [CT-SEC-005](test-cases/security.md) | Limite de payload contornado por *chunked encoding* | RN-06, H-04 | P2 | SEC | ⏳ Planejado |
| [CT-SEC-006](test-cases/security.md) | Estruturas abusivas | NF-03, RN-25 | P2 | SEC | ⏳ Planejado |
| [CT-SEC-007](test-cases/security.md) | Vazamento de informação técnica | NF-03, RN-23 | P2 | SEC | ⏳ Planejado |
| [CT-SEC-008](test-cases/security.md) | Cabeçalhos de segurança do front-end | NF-03, H-08 | P2 | SEC | ⏳ Planejado |
| [CT-SEC-009](test-cases/security.md) | X-Request-Id abusivo | NF-03, RN-24, H-07 | P3 | SEC | ⏳ Planejado |
| [CT-SEC-010](test-cases/security.md) | Superfície exposta | NF-03, RN-27 | P2 | SEC | ⏳ Planejado |
| [CT-SEC-011](test-cases/security.md) | Política de CORS | NF-03 | P3 | SEC | ⏳ Planejado |
| [CT-SEC-012](test-cases/security.md) | Nenhuma operação destrutiva exposta | RN-26 | P2 | SEC | ⏳ Planejado |
| [CT-SEC-013](test-cases/security.md) | Identificadores não previsíveis | RN-15, RN-26 | P3 | SEC | ⏳ Planejado |
| [CT-SEC-014](test-cases/security.md) | Ausência de limitação de taxa | NF-03 | P3 | SEC | ⏳ Planejado |
| [CT-SEC-015](test-cases/security.md) | Varredura dinâmica (DAST) | NF-03 | P2 | DAST | ⏳ Planejado |
| [CT-SEC-016](test-cases/security.md) | Componentes vulneráveis (SCA) | NF-03 | P3 | SEC | ⏳ Planejado |
| [CT-WEB-001](test-cases/web-e2e.md) | Idioma padrão na primeira visita | RN-33, RN-28, Q-11 | P1 | WEB | ✅ Automatizado |
| [CT-WEB-002](test-cases/web-e2e.md) | Navegação pelo menu lateral | RN-28 | P2 | WEB | ✅ Automatizado |
| [CT-WEB-003](test-cases/web-e2e.md) | Rota inexistente | RN-28 | P3 | WEB | ✅ Automatizado |
| [CT-WEB-057](test-cases/web-e2e.md) | Aplicação operante quando acessada por nome de host via HTTP (fora de localhost) | RN-28, RN-33, RN-34, H-18 | P2 | WEB | ✅ Automatizado |
| [CT-WEB-010](test-cases/web-e2e.md) | Indicadores coerentes com a API | RN-29 | P1 | WEB | ✅ Automatizado |
| [CT-WEB-011](test-cases/web-e2e.md) | Atalhos do painel | RN-29 | P2 | WEB | ✅ Automatizado |
| [CT-WEB-012](test-cases/web-e2e.md) | Aviso de indicadores indisponíveis | RN-29 | P3 | WEB | ✅ Automatizado |
| [CT-WEB-013](test-cases/web-e2e.md) | Painel sem pedidos | RN-29 | P3 | WEB+MOCK | ✅ Automatizado |
| [CT-WEB-014](test-cases/web-e2e.md) | Erro de API no painel | RN-34 | P2 | WEB+MOCK | ✅ Automatizado |
| [CT-WEB-020](test-cases/web-e2e.md) | Consulta individual | RN-30 | P1 | WEB | ✅ Automatizado |
| [CT-WEB-021](test-cases/web-e2e.md) | Consulta em lote com situações diferentes | RN-30, RN-08 | P1 | WEB | ✅ Automatizado |
| [CT-WEB-022](test-cases/web-e2e.md) | Lote com ID inexistente | RN-30 | P1 | WEB | ✅ Automatizado |
| [CT-WEB-023](test-cases/web-e2e.md) | Consulta individual de ID inexistente | RN-30, RN-34 | P2 | WEB | ✅ Automatizado |
| [CT-WEB-024](test-cases/web-e2e.md) | Campo obrigatório | RN-30 | P2 | WEB | ✅ Automatizado |
| [CT-WEB-025](test-cases/web-e2e.md) | Separadores e duplicatas | RN-30 | P3 | WEB | ✅ Automatizado |
| [CT-WEB-026](test-cases/web-e2e.md) | Limite do lote na interface | RN-30, RN-20 | P2 | WEB | ✅ Automatizado |
| [CT-WEB-030](test-cases/web-e2e.md) | Criação de pedido com desconto (fluxo principal) | RN-31, RN-32, RN-13 | P1 | WEB | ✅ Automatizado |
| [CT-WEB-031](test-cases/web-e2e.md) | Criação abaixo do mínimo de desconto | RN-31, RN-13 | P1 | WEB | ✅ Automatizado |
| [CT-WEB-032](test-cases/web-e2e.md) | Campos do cliente obrigatórios | RN-31, RN-01 | P1 | WEB | ✅ Automatizado |
| [CT-WEB-033](test-cases/web-e2e.md) | E-mail inválido | RN-31, RN-01 | P2 | WEB | ✅ Automatizado |
| [CT-WEB-034](test-cases/web-e2e.md) | Adicionar produto inexistente | RN-31, RN-07 | P1 | WEB | ✅ Automatizado |
| [CT-WEB-035](test-cases/web-e2e.md) | Adicionar produto inativo | RN-31, RN-07 | P1 | WEB | ✅ Automatizado |
| [CT-WEB-036](test-cases/web-e2e.md) | Adicionar produto sem preço vigente | RN-31, RN-07, RN-08 | P1 | WEB | ✅ Automatizado |
| [CT-WEB-037](test-cases/web-e2e.md) | Adicionar produto já incluído | RN-31, RN-04 | P2 | WEB | ✅ Automatizado |
| [CT-WEB-038](test-cases/web-e2e.md) | Limites de quantidade na interface | RN-31, RN-03 | P2 | WEB | ✅ Automatizado |
| [CT-WEB-039](test-cases/web-e2e.md) | Edição de quantidade recalcula valores exibidos | RN-31 | P2 | WEB | ✅ Automatizado |
| [CT-WEB-040](test-cases/web-e2e.md) | Remoção de itens | RN-31 | P2 | WEB | ✅ Automatizado |
| [CT-WEB-041](test-cases/web-e2e.md) | Criar sem itens | RN-31, RN-02 | P2 | WEB | ✅ Automatizado |
| [CT-WEB-042](test-cases/web-e2e.md) | Erro de regra de negócio na criação | RN-31, RN-34 | P2 | WEB+MOCK | ✅ Automatizado |
| [CT-WEB-043](test-cases/web-e2e.md) | Catálogo indisponível na criação | RN-31, RN-34, RN-10 | P2 | WEB+MOCK | ✅ Automatizado |
| [CT-WEB-044](test-cases/web-e2e.md) | Cancelar com e sem dados | RN-31, H-14 | P3 | WEB | ✅ Automatizado |
| [CT-WEB-045](test-cases/web-e2e.md) | Duplo clique em "Criar pedido" | RN-31 | P2 | WEB | ✅ Automatizado |
| [CT-WEB-050](test-cases/web-e2e.md) | Pedido recém-criado no topo da lista | RN-32, RN-17 | P1 | WEB | ✅ Automatizado |
| [CT-WEB-051](test-cases/web-e2e.md) | Paginação | RN-32, RN-17 | P2 | WEB | ✅ Automatizado |
| [CT-WEB-058](test-cases/web-e2e.md) | Paginação com volume: todos os links de página ao alcance | RN-32, RN-17, H-15 | P2 | WEB | ✅ Automatizado |
| [CT-WEB-052](test-cases/web-e2e.md) | Detalhe a partir da lista | RN-32, RN-16 | P1 | WEB | ✅ Automatizado |
| [CT-WEB-053](test-cases/web-e2e.md) | Detalhe de pedido inexistente | RN-32, RN-34 | P2 | WEB | ✅ Automatizado |
| [CT-WEB-054](test-cases/web-e2e.md) | Lista vazia | RN-32 | P3 | WEB+MOCK | ✅ Automatizado |
| [CT-WEB-055](test-cases/web-e2e.md) | Voltar do detalhe | RN-32 | P3 | WEB | ✅ Automatizado |
| [CT-WEB-056](test-cases/web-e2e.md) | Lista de pedidos continua disponível após redeploy do order-service | RN-32, RN-34, H-13 | P2 | WEB | ✅ Automatizado |
| [CT-WEB-060](test-cases/web-e2e.md) | Troca de idioma em tempo de execução | RN-33 | P1 | WEB | ✅ Automatizado |
| [CT-WEB-061](test-cases/web-e2e.md) | Formatação de moeda e data por idioma | RN-33, H-17 | P2 | WEB | ✅ Automatizado |
| [CT-WEB-062](test-cases/web-e2e.md) | Preferência de idioma persistida | RN-33 | P2 | WEB | ✅ Automatizado |
| [CT-WEB-063](test-cases/web-e2e.md) | Fluxos principais em cada idioma | RN-33, RN-30, RN-31 | P2 | WEB | ✅ Automatizado |
| [CT-WEB-064](test-cases/web-e2e.md) | Paridade dos arquivos de tradução | RN-33 | P3 | ESTÁTICO | ✅ Automatizado |
| [CT-WEB-065](test-cases/web-e2e.md) | Mensagens de erro traduzidas | RN-33, RN-34 | P3 | WEB | ✅ Automatizado |
| [CT-WEB-070](test-cases/web-e2e.md) | Varredura automática de acessibilidade | NF-04, H-16 | P3 | WEB | ✅ Automatizado |
| [CT-WEB-071](test-cases/web-e2e.md) | Operação só por teclado | NF-04 | P3 | WEB | ✅ Automatizado |
