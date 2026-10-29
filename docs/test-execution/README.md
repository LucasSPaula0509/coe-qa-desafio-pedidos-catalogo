# Relatório de Execução das Suítes

Relatório consolidado da execução das suítes automatizadas (Parte 5). A **execução de referência** é a da esteira de CI. Ela roda em ambiente Linux limpo, com o SUT construído do zero, todos os gates, carga nominal e decisão de deploy. Os registros locais por suíte trazem a análise detalhada de cada falha.

## Execução de referência (CI)

| Item | Valor |
|---|---|
| Execução | [GitHub Actions #40932400876](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/actions/runs/40932400876) (push em `develop`) |
| Commit | `9634f48` (merge do PR #23) |
| Data | 2026-10-29, 18:39:59 → 18:58:00 UTC (18 min) |
| Ambiente | `ubuntu-latest`; SUT construído do zero (`scripts/start-sut.sh`) com o proxy de falhas; prontidão e aquecimento (`scripts/wait-for-sut.sh`) |
| SUT | Manifesto `sut.sha256` íntegro (G0) |

### Gates

| Gate | Resultado | Duração | O que verificou |
|---|---|---|---|
| G0 · Integridade do SUT | ✅ Aprovado | 5 s | 219 arquivos conferem com o manifesto |
| G1 · Qualidade e rastreabilidade | ✅ Aprovado | 36 s | Matriz consistente (183 casos, 38 requisitos); Spotless e compilação da API; `tsc`, ESLint e Prettier da web |
| G3 · API e integração | ✅ Aprovado | 4 min 23 s | 129/129 execuções do gate |
| G4 · Web E2E (Chromium) | ✅ Aprovado | 9 min 5 s | 47/47 casos do gate |
| G5 · Performance | ✅ Aprovado | 17 min 4 s | Smoke e carga nominal dentro de todos os thresholds |
| G7 · Decisão de deploy | ⛔ **Bloqueado** | 5 s | 4 defeitos S2 abertos sem exceção aprovada |

> **Leitura:** todas as suítes passaram nos gates, ou seja, nada piorou e a automação é estável. **O deploy foi bloqueado de propósito**: a esteira aplica a política de release (estratégia §12; ADR-QA-007) e impede a implantação enquanto houver defeito de severidade alta aberto.

Decisão registrada pelo G7:

```
Deploy bloqueado — defeitos S1/S2 abertos sem exceção aprovada (label `excecao-aprovada`):
#16 [DEF][H-15] frontend: paginação da listagem transborda e torna as primeiras páginas inalcançáveis
#14 [DEF][H-13] frontend: após redeploy do order-service, /api/orders responde 502 (ou vai para outro serviço) até reiniciar o front-end
#5  [DEF][H-11] order-service: timeout ou conexão interrompida do catálogo retorna 500 em vez de 504/503 (sem nova tentativa)
#4  [DEF][H-01] order-service: pedido com 51 a 100 produtos distintos retorna 503 em vez de 201/422
```

### Resultado por suíte

| Suíte | Visão | Execuções | Aprovadas | Falhas | Duração |
|---|---|---|---|---|---|
| API (`tests/api`) | Gate (sem defeitos conhecidos) | 129 | **129** | 0 | 39 s |
| | Defeitos conhecidos (não bloqueante) | 61 | 17 | 44 | 15 s |
| Web (`tests/web`) | Gate (sem defeitos conhecidos) | 47 | **47** | 0 | 59 s |
| | Defeitos conhecidos (não bloqueante) | 6 | 0 | 6 | 187 s |
| Performance (`tests/performance`) | Smoke (CT-PRF-001) | 64 requisições | 152/152 checks | 0% de erro | 1 min |
| | Carga nominal (CT-PRF-002) | 10.321 requisições | 24.820/24.820 checks | 0% de erro | 13 min |

As 17 aprovações entre os defeitos conhecidos da API são **variações** de casos parametrizados que se comportam como especificado (ex.: `quantity: true` e `"abc"` são rejeitados corretamente; só `1.5` e `"2"` reproduzem o #7). Todas as **44 falhas e 6 falhas** mapeiam para Issues registradas (`@KnownDefect` / `knownDefect`). Nenhuma falha ficou sem explicação.

### Desempenho (SLOs propostos, NF-01)

| Operação | SLO p95 | Smoke p95 | Carga p95 | Carga máx. | Situação |
|---|---|---|---|---|---|
| `POST /orders` | < 800 ms (p99 < 1.500 ms) | 53,4 ms | 36,7 ms | 252,3 ms | ✅ |
| `GET /orders` | < 400 ms | 18,7 ms | 7,6 ms | 35,3 ms | ✅ |
| `GET /orders/{id}` | < 300 ms | 15,4 ms | 4,6 ms | 39,0 ms | ✅ |
| `GET /products/{id}` | < 200 ms | 6,3 ms | 4,8 ms | 19,0 ms | ✅ |
| `POST /products/prices` | < 300 ms | 8,2 ms | 5,9 ms | 22,8 ms | ✅ |

Carga nominal: 30 usuários virtuais por 10 min (rampa de 2 min, descida de 1 min), 13,2 req/s, mix ponderado com *think time*. O p99 de `POST /orders` foi avaliado pelo threshold e aprovado; o máximo de 252 ms já fica abaixo de 1.500 ms. A partir desta versão, o p99 também aparece nos resumos.

**Ressalva:** runner compartilhado não é ambiente de performance. O resultado mostra ausência de regressão e folga ampla em relação aos SLOs propostos, mas não certifica capacidade (ADR-QA-004; Q-06).

## Consistência entre execuções

| Suíte | Local (Windows/WSL2) | CI (Linux) | Conclusão |
|---|---|---|---|
| API | 3 execuções: 146 aprovadas e 44 falhas, conjunto idêntico | 129 + 17 aprovadas e 44 falhas | **Idêntico**: mesmas falhas, todas de defeito conhecido |
| Web | 3 execuções idênticas; gate 47/47 após o CT-WEB-058 | Gate 47/47; 6/6 defeitos conhecidos reproduzidos | **Idêntico**: H-15 também reproduzida no CI com volume controlado |
| Performance | Smoke aprovado (p95 de criação 112 ms) | Smoke e carga aprovados (p95 de criação 53 e 37 ms) | Mesmo resultado; o ambiente local é mais lento (latência do WSL2, RP-09) |

## Histórico de execuções da esteira

| Execução | Resultado | O que ensinou |
|---|---|---|
| [PR #21](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/actions/runs/40928767770) | Gates verdes | A auditoria dos artefatos mostrou o resumo do k6 não publicado (permissão), os relatórios da API misturados e o CT-WEB-051 não determinístico. Corrigidos no PR #22 |
| `develop` após o #21 | Cancelada | A regra de concorrência valida só a versão mais recente da branch (o merge do #22 substituiu a execução) |
| [PR #22](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/actions/runs/40931161100) e `develop` após o #22 | Falha no G2 | **`502 Bad Gateway` do Maven Central** na build do SUT (externo, transitório). Não é defeito. Mitigado com nova tentativa na build (PR #23) |
| [PR #23](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/actions/runs/40932249640) | Gates verdes | Correções confirmadas no Linux: os 3 artefatos publicados e os relatórios separados |
| **[`develop` após o #23](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/actions/runs/40932400876)** | **G0–G5 verdes; G7 bloqueou o deploy** | **Execução de referência** (acima) |
| [PR #29](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/actions/runs/40940052905) (segurança) | Gates verdes, incluindo o novo G6 | A auditoria do log mostrou que o CT-SEC-008 **não executava**: com `bash -e`, a falha esperada do Maven encerrava o passo antes do Playwright. Corrigido no PR #31 |
| [PR #31](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/actions/runs/40942668687) | Gates verdes | Correção confirmada: CT-SEC-008 executado e reprovado como esperado (#26) |
| [`develop` após o #31](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/actions/runs/40942830709) | **G0–G6 verdes; G7 bloqueou o deploy** (#4, #5, #14, #16) | Primeira execução completa com segurança (21 min 33 s). Resultados iguais aos da referência: API 129/129 (defeitos conhecidos: 44 falhas em 61), web 47/47 (6 defeitos conhecidos), G6 20/20 + 1/1 (4 defeitos conhecidos). Carga nominal: 10.353 requisições, 0% de erro, `POST /orders` com p95 de 26,9 ms e p99 de 64,6 ms |

## Como acessar as evidências

| Evidência | Onde |
|---|---|
| Resumo por gate (tabelas de testes, defeitos conhecidos, k6, decisão do G7) | Página da execução no GitHub Actions, seção *Summary* |
| Relatórios da API (JUnit XML, Allure com requisição e resposta de cada chamada) | Artefato `api-reports` → `npx allure-commandline serve tests/api/target/allure-results` |
| Relatório da web (HTML com trace, screenshot e vídeo das falhas; JUnit) | Artefato `web-reports` → `npx playwright show-report tests/web/playwright-report` |
| Resultados do k6 (texto e JSON) | Artefato `performance-results` |
| Defeitos | [Issues com label `defeito`](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/issues?q=label%3Adefeito) (#4 a #28) |

## Registros detalhados por suíte

| Registro | Conteúdo |
|---|---|
| [api-2026-10-29.md](api-2026-10-29.md) | Execução local da API: estabilidade, análise de falhas e confirmação de defeitos (H-01..H-12) |
| [web-2026-10-29.md](web-2026-10-29.md) | Execução local da web: prova de oráculo e defeitos da interface (H-13..H-18) |
| [performance-2026-10-29.md](performance-2026-10-29.md) | Smoke local e validação dos 8 cenários k6 |

Avaliação da versão e recomendação de release: [resumo de qualidade](../quality-summary.md).
