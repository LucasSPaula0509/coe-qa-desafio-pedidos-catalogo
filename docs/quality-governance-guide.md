# Guia de Governança de Qualidade — Squads

Objetivo: um padrão único de qualidade para squads com stacks diferentes (Java/Spring, Node/TypeScript, Angular, .NET). Os critérios são objetivos e, sempre que possível, verificados pela esteira, sem depender da memória do revisor. Este guia **complementa** o [guia de code review do SUT](../sut/docs/code-review-guide.md), que trata do código de produção. Aqui o foco é **teste, evidência e decisão de release**.

## Papéis

| Papel | Responsabilidade de qualidade |
|---|---|
| Squad (devs) | Testes de unidade e de componente; testes de contrato do que publica; corrigir o que quebra a esteira no mesmo dia |
| QA do squad | Casos derivados das regras (SDD), automação de API/E2E, análise de falhas, registro de defeitos com evidência |
| COE de QA | Este guia, as ferramentas-padrão (ADR-QA-002..004), os templates e as métricas consolidadas. Revisa as exceções |
| Client Face / PO | Prioridade dos defeitos, aceite de risco e **exceção formal** para liberar com defeito S1/S2 |

## Definição de pronto (qualidade)

Uma história só fica pronta quando:

1. A regra tem ID (`RN-xx`) e os casos têm IDs (`CT-xxx-nnn`) **antes** da automação (ADR-QA-005).
2. Todo caso P1/P2 está automatizado no nível mais baixo que o cobre (unidade → contrato/API → E2E) ou tem justificativa registrada.
3. A matriz de rastreabilidade não tem requisito sem caso nem caso sem requisito.
4. A esteira está verde, e os defeitos encontrados estão registrados como Issue.

## Critérios que BLOQUEIAM o PR

Os itens marcados com ⚙ são verificados pela esteira (gates G0–G6). Os demais ficam com o revisor.

- ⚙ Build, lint, formatação ou compilação de tipos vermelhos (Spotless, ESLint, Prettier, `tsc`). Merge só com esteira verde.
- ⚙ Rastreabilidade inconsistente: caso sem requisito, requisito sem caso ou matriz desatualizada (`traceability.mjs --check`).
- ⚙ Regressão de contrato: resposta fora do JSON Schema ou divergente do OpenAPI publicado.
- ⚙ Teste P1/P2 falhando que não está ligado a uma Issue de defeito.
- ⚙ Smoke de performance fora dos thresholds do SLO (NF-01).
- ⚙ Falha de segurança dirigida nova: injeção, XSS, *mass assignment*, vazamento de stack trace ou superfície administrativa exposta.
- ⚙ Alteração em código congelado (ex.: `sut/` com manifesto de integridade divergente).
- Comportamento novo ou corrigido sem teste automatizado, ou teste que **não falha** sem a mudança (sem prova de oráculo).
- Teste desativado (`@Disabled`, `skip`, `only`) ou asserção afrouxada para "deixar verde". Defeito confirmado vira `defeito-conhecido` com link da Issue e continua executando.
- Espera fixa (`sleep`, `waitForTimeout`) ou dependência de ordem, de dados acumulados ou de contagem absoluta do banco.
- Resultado esperado sem fonte: o oráculo precisa vir da regra, do contrato ou da documentação, nunca do comportamento atual do sistema.
- Segredo, token ou dado pessoal real no código, nos dados de teste ou nos artefatos (usar `example.com`, RFC 2606).
- Saída de IA (caso, dado, código) aceita sem validação registrada no log de uso de IA.
- Escopo misturado: teste, refatoração e ferramenta no mesmo PR devem ser divididos.

## Não bloqueiam (comentário `NIT:`)

- Nomes, extração de *helpers* e simplificações na automação.
- Caso P3 sem automação, com justificativa e item no backlog.
- Teste em quarentena por instabilidade, com dono e prazo de 1 ciclo (ver métricas).

## Defeitos: severidade e efeito no release

| Severidade | Exemplo | Efeito | Prazo de triagem |
|---|---|---|---|
| S1 — crítica | Perda ou corrupção de dados, indisponibilidade | Bloqueia merge e deploy | Mesmo dia |
| S2 — alta | Regra de negócio errada sem contorno | Bloqueia o **deploy** (G7) até a correção ou a exceção formal (`excecao-aprovada`) | 1 dia útil |
| S3 — média | Comportamento incorreto com contorno ou de borda | Não bloqueia; entra no resumo de qualidade | Próximo ciclo |
| S4 — baixa | Cosmético, texto, documentação | Não bloqueia; backlog | Backlog |

Antes do registro, todo defeito precisa estar reproduzido por um caso de teste. A Issue segue o template, com passos, esperado com fonte, obtido, `X-Request-Id` e evidência.

## Métricas e cadência

| Indicador | Meta | Ação se fora da meta |
|---|---|---|
| Esteira verde na `develop` | ≥ 95% das execuções | Correção tem prioridade sobre feature |
| Instabilidade (passou só na nova tentativa) | < 2% em 5 execuções | Quarentena com dono; Issue de manutenção |
| Duração da esteira de PR | ≤ 15 min | Paralelizar ou mover testes para o nível mais baixo |
| Casos P1/P2 automatizados | 100% (ou justificados) | Item obrigatório no próximo ciclo |
| Defeitos S1/S2 abertos sem exceção | 0 no release | Deploy bloqueado (G7) |
| Defeitos que escaparam para produção | Tendência de queda | Análise de causa raiz e caso novo para cada um |

**Cadência:** o resumo de qualidade (1 página, para público não técnico) acompanha todo release. As métricas consolidadas vão ao COE uma vez por mês. As exceções formais são revisadas a cada release, e as vencidas voltam a bloquear.

## Boas práticas

- **Autor:** PR pequeno (menos de 400 linhas fora de `docs/`), com os IDs cobertos e a evidência da execução (resumo JUnit/Playwright, *trace* ou `curl`).
- **Revisor:** separar `BLOCK:` de `NIT:`, conferir o oráculo contra a fonte e aprovar assim que os bloqueios forem resolvidos.
- **Falha na esteira:** classificar como erro do teste, do ambiente ou do sistema **antes** de reexecutar. Reexecução sem diagnóstico não conta como evidência.
