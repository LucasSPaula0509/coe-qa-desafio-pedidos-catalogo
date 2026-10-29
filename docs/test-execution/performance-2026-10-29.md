# Registro de Execução — Performance (Parte 4)

| Item | Valor |
|---|---|
| Data | 2026-10-29 |
| Suíte | `tests/performance` — 8 cenários k6 (CT-PRF-001..008) |
| Ferramenta | k6 0.54.0 (imagem `grafana/k6:0.54.0`), na rede do Compose |
| Ambiente | Local: Docker Desktop 4.97 (WSL2, 4 CPUs, ~10 GB). SUT com o override de teste |
| Escopo desta execução | **Validação funcional**, por decisão do responsável: smoke completo e validação de todos os cenários, **sem injeção de latência** e sem os perfis longos no ambiente local |

## Resultado

### CT-PRF-001 — Smoke (gate de PR): **aprovado**

| Operação | p95 | p99/máx | SLO (p95) | Situação |
|---|---|---|---|---|
| `POST /orders` | 112,4 ms | máx 130,6 ms | < 800 ms (p99 < 1.500 ms) | ✅ |
| `GET /orders` | 52,2 ms | máx 56,8 ms | < 400 ms | ✅ |
| `GET /orders/{id}` | 27,1 ms | máx 27,9 ms | < 300 ms | ✅ |
| `GET /products/{id}` | 15,2 ms | máx 19,7 ms | < 200 ms | ✅ |
| `POST /products/prices` | 16,6 ms | máx 16,7 ms | < 300 ms | ✅ |

- **Checks funcionais:** 100% (138 de 138): status, `total = subtotal − desconto`, status `CRIADO`, snapshot de itens, IDs do lote respondidos.
- **Taxa de erro:** 0% (0 de 60 requisições). Duração: 1 min, 2 VUs. Saída: código 0.

### Validação dos demais cenários (`k6 inspect --execution-requirements`)

Cada script foi carregado e validado (sintaxe, executores, estágios e thresholds) sem gerar carga:

| Cenário | Caso | VUs máx. | Duração total | Thresholds | Situação |
|---|---|---|---|---|---|
| `load` | CT-PRF-002 | 30 | 13m30s | 10 | ✅ válido |
| `stress` | CT-PRF-003 | 300 | 36m30s | 6 | ✅ válido |
| `spike` | CT-PRF-004 | 150 | 5m30s | 9 | ✅ válido |
| `soak` | CT-PRF-005 | 60 (janelas sequenciais) | 1h0m30s | 12 | ✅ válido |
| `items-scaling` | CT-PRF-006 | 10 | 3m30s | 5 | ✅ válido |
| `dependency-degradation` | CT-PRF-007 | 30 | 6m | 10 | ✅ válido |
| `volume` | CT-PRF-008 | 20 | 5m30s + carga da massa | 6 | ✅ válido |

**Não executados localmente:** carga, estresse, pico, resistência, escala de itens, degradação e volume. A carga nominal (CT-PRF-002) roda no CI a cada push para `develop`/`main`. Os demais rodam sob demanda (*workflow_dispatch*), em ambiente apropriado.

## Observações

| Observação | Tratamento |
|---|---|
| Durante a preparação, **uma tentativa de demonstrar a reprovação do gate deixou um stub de latência de 1 s ativo no proxy**: a criação do stub executou, embora o comando tenha sido recusado | Detectado na verificação do proxy (2 mappings, quando o esperado era 1). O stub foi removido pelo ID e o ambiente conferido (só o proxy padrão; pedido de controle em 54 ms). O smoke registrado acima foi executado **antes** do incidente, sem latência injetada |
| A interação entre carga e a H-11 (timeout do catálogo vira 500) só aparece em perfis longos ou com degradação | A carga nominal no CI e os cenários sob demanda registram `unclassified_5xx` (spike) e a taxa de erro por operação |
