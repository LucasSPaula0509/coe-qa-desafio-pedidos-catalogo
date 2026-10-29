# Resumo de Qualidade da Versão — Gestão de Pedidos e Catálogo

| Item | Valor |
|---|---|
| Versão avaliada | SUT recebido para o desafio (commit `75f5172`, manifesto `sut.sha256`, 219 arquivos) |
| Período | 2026-10-29 |
| Base de avaliação | 183 casos de teste (34 regras + 4 requisitos não funcionais, cobertura de 100%), 167 automatizados em 3 suítes (API, web, performance) e executados localmente e na esteira de CI |
| Elaborado por | QA Sênior (autor), com apoio de IA (Claude Code), validado por evidências |
| Documentos de apoio | [Relatório de execução](test-execution/README.md) · [Defeitos (Issues)](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/issues?q=label%3Adefeito) · [Estratégia](test-strategy.md) · [Especificação e suspeitas](../specs/open-questions.md) |

## 1. Recomendação

> **Não liberar para produção nesta versão.** O núcleo do negócio está correto e confiável, mas **quatro defeitos de severidade alta** afetam o uso real: dois na integração com o catálogo e dois na interface. Há também dois impeditivos estruturais para produção: **não há autenticação** e a interface **exige HTTPS sem documentar isso**.
>
> **Liberação para homologação** pode ocorrer agora, com as ressalvas da seção 5.

Condições para liberar para produção:

1. Corrigir os quatro defeitos S2 (#4, #5, #14, #16) **ou** registrar exceção formal do Client Face (label `excecao-aprovada`, que libera o gate G7 da esteira).
2. Implantar sob HTTPS (ou corrigir #20) e incluir autenticação/autorização (decisão de escopo do SUT, ADR-013).
3. Homologar os SLOs de desempenho propostos (hoje são premissa do projeto de qualidade, Q-06).

## 2. O que funciona bem

O comportamento de maior risco financeiro foi testado com o máximo de rigor e **está correto**:

- **Valores do pedido:** preço do catálogo, subtotal, desconto a partir do valor mínimo (inclusive na fronteira exata), arredondamento "meio para cima" e total. Foram verificados com valores calculados de forma independente, inclusive no limite máximo (999 unidades de cada produto).
- **Pedidos inválidos são recusados por inteiro:** produto inexistente, inativo ou sem preço vigente nunca gera pedido parcial. Isso foi verificado diretamente no banco.
- **O preço registrado no pedido não muda** quando o catálogo é reajustado depois (snapshot).
- **Interface:** consulta de catálogo, criação de pedido, listagem, detalhe e troca de idioma em tempo de execução funcionam, nos três idiomas, e exibem exatamente o que a API calcula.
- **Desempenho:** na carga nominal da esteira (30 usuários simultâneos por 10 min, 10.321 requisições), **0% de erro e 100% de respostas corretas**. A criação de pedido ficou em 37 ms no p95, contra o limite proposto de 800 ms.

## 3. O que impede o release (severidade alta)

| Issue | Em linguagem de negócio | Impacto |
|---|---|---|
| [#4](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/issues/4) | Pedidos com **mais de 50 produtos diferentes** são recusados como se o catálogo estivesse fora do ar, embora o sistema prometa aceitar até 100 | Clientes com pedidos grandes não conseguem comprar e recebem uma mensagem enganosa |
| [#5](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/issues/5) | Quando o catálogo **demora ou cai no meio da resposta**, o sistema responde "erro interno" e **não faz a nova tentativa prevista** | Falhas temporárias do catálogo viram pedidos perdidos, e o monitoramento culpa o serviço errado |
| [#14](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/issues/14) | Depois de **qualquer atualização do serviço de pedidos**, a tela de Pedidos para de funcionar até alguém reiniciar o front-end. Num dos casos, as requisições foram para o serviço errado | Toda implantação de correção derruba a interface em silêncio |
| [#16](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/issues/16) | Com volume real de pedidos, a paginação **esconde as primeiras páginas** fora da tela | Os pedidos mais recentes depois da primeira página ficam inacessíveis pela interface |

## 4. Demais defeitos

| Severidade | Issue | Defeito |
|---|---|---|
| S3 — média | [#6](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/issues/6) | Erros do cliente (id inválido, parâmetro não numérico, método/rota inexistente) respondem "erro interno" (500) em vez de erro de entrada (4xx) |
| S3 | [#7](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/issues/7) | Quantidade `1.5` vira **1 unidade sem aviso**; texto e número são convertidos silenciosamente |
| S3 | [#8](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/issues/8) | O catálogo omite campos que o contrato documenta como `null` (`currentPrice`, `validTo`) |
| S3 | [#15](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/issues/15) | O diálogo "Descartar pedido?" aparece vazio, e confirmar não sai da tela |
| S3 | [#20](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/issues/20) | Servida por HTTP fora de `localhost`, a interface fica inoperante, sem mensagem |
| S4 — baixa | [#9](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/issues/9) | O lote do catálogo aceita IDs repetidos |
| S4 | [#10](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/issues/10) | A data de criação do pedido difere entre a criação e a consulta (precisão) |
| S4 | [#11](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/issues/11) | Requisição rejeitada pelo servidor web devolve HTML, sem o formato padrão de erro |
| S4 | [#12](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/issues/12) | Exemplos da documentação divergem da massa entregue |
| S4 | [#17](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/issues/17) | Região rolável do detalhe do pedido sem acesso por teclado (acessibilidade) |
| S4 | [#18](https://github.com/LucasSPaula0509/coe-qa-desafio-pedidos-catalogo/issues/18) | Formatação de valores em espanhol fora da norma do idioma |

**Distribuição:** 15 defeitos, sendo 4 S2, 5 S3 e 6 S4 (nenhum S1). Por componente: front-end 6, integração e `order-service` 3, `catalog-service` 3, os dois serviços 2, documentação 1. Por camada que detectou: suíte de API 9, suíte web 6. A automação de interface encontrou defeitos que nenhum teste de API alcançaria (#14, #15, #16, #20).

## 5. Riscos residuais e observações

| Risco / observação | Situação | Recomendação |
|---|---|---|
| **Sem autenticação**: qualquer pessoa com o identificador lê os dados pessoais de um pedido | Decisão de escopo do SUT (ADR-013) | Impeditivo para produção. OAuth2/JWT antes de expor a dados reais |
| **Segurança ainda não avaliada pela suíte própria** | Suspeitas H-04 (limite de 1 MB contornável), H-07 (`X-Request-Id` sem validação) e H-08 (cabeçalhos de segurança ausentes) **aguardam confirmação** | Executar a suíte de segurança (Parte diferenciais, gate G6) antes do release |
| **SLOs de desempenho são premissa**, sem homologação | Smoke e carga nominal dentro dos limites com folga ampla (p95 de criação 37 ms × 800 ms), em runner compartilhado | Homologar SLO e volume esperado com o Client Face (Q-06); certificar capacidade em ambiente dedicado |
| **Resiliência sem prazo total**: um catálogo que responde aos poucos pode segurar a criação do pedido indefinidamente | Comportamento conforme a documentação ("timeout de leitura"), não é defeito | Orçamento de tempo total para a chamada ao catálogo |
| Rótulos acessíveis da paginação ("Previous"/"Next") em inglês em todos os idiomas | Observação (vem do ng-bootstrap, sem tradução no SUT) | Fornecer as traduções do ng-bootstrap (`$localize`) |
| **12 questões de interpretação** da documentação (Q-01 a Q-12) com interpretação adotada pelo QA | Abertas | Validar com o PO/Client Face. Algumas mudam a classificação de defeitos (ex.: Q-04 → #9) |

## 6. Indicadores de qualidade

| Indicador | Valor |
|---|---|
| Cobertura de requisitos (regras com ≥ 1 caso) | **38/38 (100%)** |
| Casos automatizados | **167/183 (91%)**. Os 16 restantes são da suíte de segurança (diferenciais) |
| Estabilidade das suítes (local) | 3 execuções completas seguidas, com resultados idênticos, na API e na web. Nenhuma falha sem explicação |
| Instabilidade (*flakiness*) | 0 testes aprovados só na nova tentativa |
| Defeitos por severidade | S1: 0 · S2: 4 · S3: 5 · S4: 6 |
| Esteira de CI (execução de referência) | G0–G5 aprovados (API 129/129, web 47/47, smoke e carga dentro dos SLOs); **G7 bloqueou o deploy** pelos 4 defeitos S2, como a política determina ([relatório de execução](test-execution/README.md)) |
| Consistência local × CI | Mesmo conjunto de falhas nas duas plataformas; todas mapeadas para Issues |

## 7. Recomendações de evolução

**Para o sistema** (em ordem de valor e esforço):

1. **Integração com o catálogo:** dividir a consulta em lotes de até 50 IDs (#4) e classificar corretamente as falhas de IO, com nova tentativa (#5). Esse é o maior ganho de confiabilidade.
2. **Front-end:** `resolver` no nginx (#14), paginação com limite de links (#16), diálogo de confirmação (#15) e UUID com alternativa fora de HTTPS (#20).
3. **Contrato de erro:** tratar as exceções de entrada do framework como 4xx (#6), desligar conversões silenciosas de tipo (#7) e alinhar nulos e documentação (#8, #12).
4. **Segurança e operação:** autenticação, cabeçalhos de segurança no nginx, prazo total nas chamadas entre serviços e tradução dos componentes de terceiros.

**Para o processo de qualidade:**

1. **Shift-left no squad do SUT:** incorporar a suíte de API e os JSON Schemas ao CI do próprio SUT (contrato verificado antes do merge), e não só ao projeto de QA.
2. **Ativar proteção de branch** (checks G0–G5 obrigatórios) assim que o plano do repositório permitir.
3. **Ambiente de performance dedicado** para certificar capacidade: o runner do CI só detecta regressão.
4. **Fechar as 12 questões** com o PO: a especificação passa a ser a fonte única, sem interpretação do QA.
