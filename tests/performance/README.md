# Suíte de Performance e Carga (k6)

Automação dos casos `CT-PRF-001..008` ([docs/test-cases/performance.md](../../docs/test-cases/performance.md)) com k6 0.54.0 em container ([ADR-QA-004](../../docs/adr/ADR-QA-004-stack-testes-performance.md)). Os limites são os **SLOs propostos** em NF-01/NF-02 (premissa a homologar, Q-06).

## Execução

Pré-requisito: ambiente do SUT no ar com o *override* de teste (o CT-PRF-007 usa o proxy de falhas).

```bash
# na raiz do repositório
bash scripts/wait-for-sut.sh                               # prontidão + aquecimento
bash tests/performance/run.sh smoke                        # CT-PRF-001 (gate em todo PR)
bash tests/performance/run.sh load                         # CT-PRF-002 (gate na develop/main)
bash tests/performance/run.sh load -e HOLD=3m              # carga com sustentação reduzida
bash tests/performance/run.sh stress -e MAX_VUS=90 -e STEP=1m
```

O k6 roda na rede do Compose (`sut_default`) e acessa os serviços pelo nome (`order-service:8080`, `catalog-service:8081`, `catalog-proxy:8080`). O comportamento é o mesmo no Windows (Git Bash) e no CI. **Código de saída diferente de 0 = threshold violado = gate reprovado.**

| Cenário | Caso | Perfil | Duração padrão | Uso |
|---|---|---|---|---|
| `smoke` | CT-PRF-001 | 2 VUs, todas as operações | 1 min | Gate em todo PR |
| `load` | CT-PRF-002 | 0 → 30 VUs, sustentação, descida (`HOLD`) | 13 min | Gate na `develop`/`main` |
| `stress` | CT-PRF-003 | +30 VUs a cada `STEP` até `MAX_VUS` | 36 min | Sob demanda (informativo) |
| `spike` | CT-PRF-004 | 5 → 150 VUs em 10 s + recuperação | 5,5 min | Sob demanda |
| `soak` | CT-PRF-005 | 30 VUs (`DURATION`) | 60 min | Sob demanda |
| `items-scaling` | CT-PRF-006 | 1 item × 3 itens por pedido | 3,5 min | Sob demanda |
| `dependency-degradation` | CT-PRF-007 | Carga com +1 s de latência injetada no catálogo | 6 min | Sob demanda (**injeta latência**: não usar em ambiente compartilhado) |
| `volume` | CT-PRF-008 | Massa ≥ 10.000 pedidos + listagem | 5,5 min + carga da massa | Sob demanda (cria pedidos) |

No CI, qualquer cenário pode ser executado pelo *workflow_dispatch* (`performance_profile`).

## Estrutura

```
lib/config.js       Endereços e SLOs (NF-01) → thresholds por operação (tag "name")
lib/operations.js   Operações do mix com checks funcionais (status, total = subtotal − desconto, snapshot)
lib/summary.js      Resumo em texto e JSON em results/ (artefato do CI)
scenarios/*.js      Um arquivo por caso (o ID do caso fica no cabeçalho)
run.sh              Execução em container (imagem fixada; K6_IMAGE e SUT_NETWORK sobrescrevíveis)
```

## Princípios

- **Resposta rápida e errada é falha:** toda requisição tem checks funcionais, e o threshold de `checks` faz parte do gate.
- **Thresholds por operação** (`http_req_duration{name:…}`), e não só a média geral, para que uma operação lenta não fique escondida.
- **Think time de 1 a 3 s** no mix: o perfil simula usuários, e não uma rajada sintética.
- **Resultados comparativos:** em runner compartilhado, o resultado indica regressão, não capacidade certificada (ADR-QA-004).
