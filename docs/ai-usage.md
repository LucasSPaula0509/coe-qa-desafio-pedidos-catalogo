# Uso de IA no Ciclo de Testes

**Ferramenta utilizada:** Claude Code (modelo Claude Opus 5.5), agente de IA integrado ao VS Code. Ele lê o repositório, executa comandos (git, build, testes) e produz artefatos, sempre com aprovação e revisão do candidato.

**Motivo da escolha:** o desafio pede IA em **todo** o ciclo de testes (estratégia, especificação, automação, execução e análise de defeitos), com evidências. Um agente com acesso ao repositório e ao terminal consegue:

1. Cruzar a documentação do SUT com o código, o que gera suspeitas de defeito fundamentadas, e não genéricas.
2. Executar os próprios artefatos (scripts, suítes) e iterar sobre o resultado real.
3. Manter o contexto do projeto inteiro entre as etapas.

Um copiloto de autocompletar cobriria só a etapa de automação. Um chat sem acesso ao repositório produziria casos de teste desconectados dos dados e dos contratos reais.

**Ferramenta complementar:** SAI APP 3.0 (diferencial avaliado). O uso é registrado nas etapas em que for aplicado.

**Link do chat/conversa com a IA:** `<INSERIR_LINK_DO_CHAT_AQUI>`

## Metodologia: Spec-Driven Development (SDD)

A IA não é autoridade sobre o resultado esperado (ADR-QA-005). O fluxo é **especificação → caso → automação → evidência**:

- A IA consolida a documentação do SUT em regras testáveis (RN), sempre citando a fonte.
- O candidato revisa as regras e decide as ambiguidades (Q-xx).
- Os casos de teste derivam das RN por técnicas formais de projeto de teste.
- A automação implementa os casos e não "descobre" o esperado a partir do comportamento.
- Suspeitas de defeito levantadas pela IA (H-xx) só viram Issue depois de reproduzidas por teste.

## Log de utilização por etapa

| Etapa | Prompt (objetivo) | Decisão humana | Validação aplicada |
|---|---|---|---|
| 1. Análise do desafio | "Analise o desafio (PDF anexo) de forma prática, mantendo os padrões do projeto e do documento" | Aprovação da estrutura de entrega (repositório único com SUT isolado) e da ordem de execução (estrutura → Parte 1 → … → Parte 5) | Leitura cruzada PDF × `sut/docs/` × código. Cada suspeita foi apontada para a linha de código e o documento correspondentes. |
| 2. Estrutura do repositório | Propor e montar o repositório com o SUT inalterado e GitFlow | Aceite de `sut/` + manifesto SHA-256 como prova de não alteração | O script de integridade foi testado em 3 cenários (íntegro, alterado, adicionado) e em checkout limpo via `git worktree`. A IA detectou `core.autocrlf=true` no Git do sistema, que alteraria os bytes do SUT, e isso foi mitigado com `.gitattributes`. |
| 3. ADRs de stack | Justificar as stacks de API, web e performance e o processo SDD, com alternativas | Stack aprovada: Java + RestAssured (API), Playwright (web), k6 (performance) | Coerência com o SUT conferida (Java 17, Spring Boot 3.3.4, Spotless, front Angular sem `data-testid`, ausência de SLA na documentação) |
| 4. Especificação (SDD) | "Consolide a base de teste (`sut/docs/` + PDF §2.2) em regras testáveis com fonte; registre ambiguidades e suspeitas de defeito com evidência de código" | Interpretações adotadas para as 11 questões (Q-01..Q-11), a validar com o Client Face; severidade preliminar das 8 suspeitas | Cada RN conferida na fonte citada. Cada H aponta para arquivo e linha do SUT e para o caso que a confirmará. Nenhuma H foi registrada como defeito antes de reprodução. |
| 5. Casos de teste | "Derive os casos das RN por técnica (partição, valor-limite, tabela de decisão), com prioridade por risco e nível de execução" | Pirâmide com foco em API; UI restrita a apresentação e jornadas; bordas de vigência no instante exato deixadas aos testes unitários do SUT (evita testes dependentes do relógio) | Os 13 conjuntos de valores monetários esperados foram **recalculados de forma independente** em aritmética inteira (centavos), e todos conferiram. Cobertura validada pelo gerador da matriz. |
| 6. Rastreabilidade | "Gere a matriz por script, validando IDs, referências cruzadas e cobertura" | Matriz gerada, nunca editada à mão; o modo `--check` vira gate no CI | O validador encontrou **3 inconsistências reais** na primeira versão dos casos (ver tabela abaixo), corrigidas antes do commit |
| 7. Estratégia e plano | "Elabore a estratégia (riscos, níveis, técnicas, gates, defeitos, métricas) e o plano (ISO/IEC/IEEE 29119-3)" | Política de defeitos conhecidos (teste não é desativado; S1/S2 bloqueiam deploy salvo exceção do Client Face); SLOs como premissa a homologar | Números do plano (179 casos, 81/72/26 por prioridade, 110 de API) conferidos com a matriz gerada |
| 8. Ambiente e smoke | "Instale Docker Desktop/WSL2 e GitHub CLI, crie o repositório e valide os critérios de entrada do plano" | Repositório **privado** (material marcado como confidencial); PRs empilhados sem merge, aguardando revisão | Critérios de entrada atendidos (health `UP`, massa conforme RN-22, regra 1000/10% ativa); smoke CT-ORD-002 e CT-CAT-001 aprovados com valores exatos. A observação revelou a divergência de contrato **H-09** (campos nulos omitidos), registrada como suspeita a confirmar pela suíte, e não como defeito direto |

## Saídas da IA corrigidas ou rejeitadas

Evidência de que as saídas são validadas, e não aceitas por confiança:

| Etapa | Saída da IA | Problema identificado | Resolução |
|---|---|---|---|
| 1 | Suspeita "`size` > 100 e `page` < 0 normalizados silenciosamente deveriam retornar 400" | `sut/docs/adr/ADR-011-paginacao.md` documenta explicitamente que "valores fora da faixa [são] normalizados" | Suspeita **descartada como defeito** e convertida em caso de teste positivo da normalização (o esperado segue a ADR) |
| 2 | Manifesto SHA-256 gerado com o prefixo `*./` (modo binário do `sha256sum` no Windows) | O formato não seria verificável no runner Linux, porque o caminho não corresponde a `sut/` | Manifesto regenerado com caminhos normalizados (`sut/...`) e validado em checkout limpo |
| 6 | Casos de performance sem referência à Q-06 (ausência de SLA) | Detectado pelo validador: os SLOs são premissa, e sem o vínculo a questão ficaria sem rastreio | Q-06 adicionada à referência dos 8 casos CT-PRF |
| 6 | H-02 citava CT-TRV-005 como confirmação | Detectado pela checagem bidirecional: após renumeração, CT-TRV-005 passou a testar `X-Request-Id` em branco, não erro de framework | Referência corrigida para CT-TRV-006/007 (rota inexistente e método não suportado) |
| 6 | Q-08 (definição de 1 MB) não referenciada pelo caso de 413 | O caso CT-ORD-046 depende da interpretação da Q-08 | Q-08 adicionada à referência de CT-ORD-046 |

## Uso responsável

- **Nenhum artefato é aceito sem validação:** documentos são conferidos contra a fonte citada, e scripts e testes são executados antes do commit.
- **Dados fictícios:** os dados de cliente nos testes são sintéticos (nenhum CPF ou e-mail real). Nada sensível é enviado à ferramenta de IA.
- **Decisões de qualidade permanecem humanas:** escopo, priorização por risco, severidade de defeitos, limites de performance e critérios de bloqueio.
- **Rastreabilidade:** cada decisão relevante tem ADR, e cada etapa com IA tem registro nesta página e no template de PR.
