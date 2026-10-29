# Casos de Teste — Performance e Carga

Ferramenta: k6 (ADR-QA-004). Os limites são os **SLOs propostos** em NF-01/NF-02 e precisam ser homologados (Q-06). O modelo de carga está em `docs/test-strategy.md` §7.

**Modelo de carga nominal (premissa):**

| Operação | Peso no mix | Observação |
|---|---|---|
| `GET /api/v1/orders?page=0&size=20` | 30% | Listagem (tela de Pedidos e Painel) |
| `GET /api/v1/orders/{id}` | 20% | Detalhe de pedidos criados no próprio teste |
| `GET /api/v1/products/{id}` | 15% | Consulta de catálogo (tela de Catálogo) |
| `POST /api/v1/products/prices` | 15% | Lote de 2 a 6 IDs (validação de itens na tela) |
| `POST /api/v1/orders` | 20% | 1 a 3 itens válidos, quantidades aleatórias de 1 a 5 |

Carga nominal: **30 usuários virtuais** com *think time* de 1 a 3 s (≈ 10–15 req/s). Toda requisição tem **checks funcionais**: status esperado e, na criação, `total = subtotal − discount` e `status = CRIADO`. Uma resposta rápida e errada conta como falha.

**Ressalva de ambiente:** runners compartilhados do GitHub Actions não são ambiente de performance. No CI, os resultados detectam **regressão**. Certificar capacidade exige um ambiente dedicado e equivalente à produção (premissa do plano).

| ID | Cenário | Perfil | Critério de aprovação (thresholds) | Ref. | Técnica | Prio | Nível |
|---|---|---|---|---|---|---|---|
| CT-PRF-001 | **Smoke** de performance | 2 VUs, 1 min, todas as operações do mix | 100% dos checks; taxa de erro 0%; p95 de cada operação dentro do NF-01 | NF-01, Q-06 | CA | P1 | PERF |
| CT-PRF-002 | **Carga nominal** | Rampa 0 → 30 VUs em 2 min; 30 VUs por 10 min; rampa de descida em 1 min | p95/p99 por operação dentro do NF-01; erro < 1%; checks > 99% | NF-01, Q-06 | CA | P1 | PERF |
| CT-PRF-003 | **Estresse** (ponto de saturação) | Degraus de +30 VUs a cada 3 min até 300 VUs ou até degradar | Informativo: registrar a carga em que p95 > 2× SLO ou erro > 5% (capacidade máxima observada); sem queda de serviço (health `UP` ao final) | NF-01, NF-02, Q-06 | CA | P2 | PERF |
| CT-PRF-004 | **Pico** (spike) | 5 VUs → 150 VUs em 10 s, por 1 min → 5 VUs por 3 min | Durante o pico: sem 5xx diferentes de `503`/`504` classificados. Após o pico: p95 volta ao NF-01 em até 1 min | NF-02, Q-06 | CA | P2 | PERF |
| CT-PRF-005 | **Resistência** (soak) | 30 VUs por 60 min | Sem tendência de alta no p95 (> 20% entre o primeiro e o último quarto do teste); erro < 1%; sem erros de pool de conexão | NF-02, Q-06 | CA | P3 | PERF |
| CT-PRF-006 | Criação **independente do número de itens** (anti-N+1) | 10 VUs por 3 min com pedidos de 1 item × pedidos de 3 itens (máximo de produtos válidos da massa) | Diferença de p95 entre os dois grupos < 20% (1 chamada em lote por pedido) | NF-01, RN-09, Q-06 | CA | P3 | PERF |
| CT-PRF-007 | Degradação da dependência | Carga nominal com +1 s de latência injetada no catálogo (proxy) | Criação: p95 ≈ SLO + 1 s, sem erros. Leituras de pedidos **não afetadas** (p95 dentro do NF-01), porque não dependem do catálogo | NF-02, RN-11, Q-06 | CA | P3 | PERF |
| CT-PRF-008 | Listagem com **volume** | Base com ≥ 10.000 pedidos (carga prévia via API); 20 VUs, 5 min, páginas aleatórias e `size` de 20 e 100 | `GET /orders` p95 dentro do NF-01 com `size=20` e < 2× com `size=100` | NF-01, RN-17, Q-06 | CA | P2 | PERF |
