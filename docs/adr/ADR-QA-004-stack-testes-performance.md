# ADR-QA-004: Testes de performance e carga com k6

**Status:** Aceita

## Contexto

A Parte 4 exige testes automatizados de performance e carga, integrados à esteira como quality gate. O caminho crítico do sistema é `POST /api/v1/orders`, que faz uma chamada síncrona em lote ao catálogo (2 tentativas, timeout de leitura de 3 s) e uma transação no PostgreSQL. A documentação do SUT **não define SLAs**, então os limites precisam ser propostos como premissa e validados com os stakeholders.

## Decisão

**k6** (Grafana k6), com scripts em JavaScript versionados em `tests/performance/`:

- **Thresholds como gate:** limites de p95, p99 e taxa de erro declarados no script. O k6 termina com código diferente de zero quando um threshold é violado, e isso vira o gate no CI sem nenhuma lógica extra.
- **Perfis por objetivo:**
  - *smoke*: em todo PR.
  - *load*: carga nominal, na `develop`.
  - *stress* e *spike*: execução manual ou agendada.
  - *soak*: opcional.
- **Checks funcionais** em cada requisição (status, `total = subtotal − discount`), para que velocidade com resposta errada não passe como sucesso.
- **Tags por endpoint** (`name: POST /orders`) para métricas separadas por operação.
- Resumo exportado em JSON e HTML como artefato do CI.

## Alternativas consideradas

- **JMeter:** completo, mas os planos em XML são difíceis de revisar em PR e o gate exige plugins ou pós-processamento.
- **Gatling:** excelente, mas acrescentaria Scala/Kotlin ou a DSL Java a mais uma suíte, e o gate por asserções é menos direto que os thresholds do k6.
- **Locust:** introduziria Python só para essa suíte.

## Consequências

- **Positivas:**
  - Scripts legíveis em code review.
  - O gate é nativo (thresholds e exit code).
  - Binário único, fácil de rodar no GitHub Actions e localmente (`docker run grafana/k6`).
- **Negativas:**
  - Runner GitHub compartilhado não é ambiente de performance. Por isso, os resultados do CI servem para **detectar regressão relativa** (gate de smoke e load leve), não para certificar capacidade. Isso fica registrado na estratégia e nas premissas.
  - Os limites são propostos (não há SLA no SUT) e precisam ser homologados com o Client Face e os stakeholders.
