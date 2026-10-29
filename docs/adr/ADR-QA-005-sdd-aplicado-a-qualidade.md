# ADR-QA-005: Spec-Driven Development (SDD) aplicado à qualidade

**Status:** Aceita

## Contexto

O desafio pede SDD aplicado às atividades de qualidade e avalia o uso eficaz de IA em todo o ciclo, com prompts, decisões e validações documentados. Quando a IA gera artefatos de teste diretamente a partir do código, a tendência é testar **o que o sistema faz**, e não **o que ele deveria fazer**. Isso esconde defeitos, porque o teste passa a refletir o comportamento atual.

## Decisão

Toda atividade de teste segue o fluxo **especificação → caso → automação → evidência**, e cada etapa referencia a anterior por ID:

```
sut/docs/ (base oficial)
   │  consolidação + revisão humana
   ▼
specs/business-rules.md        RN-xx: regra testável, com fonte citada
specs/open-questions.md        Q-xx: ambiguidades | H-xx: suspeitas de defeito
   │  técnicas de projeto de teste (ISTQB)
   ▼
docs/test-cases/*.md           CT-<ÁREA>-nnn → RN-xx
   │  automação
   ▼
tests/**                       @DisplayName("CT-ORD-001 …") / test('CT-WEB-001 …')
   │  execução
   ▼
relatórios + Issues            defeito → CT → RN → fonte
```

Regras:

1. **O resultado esperado vem da especificação, nunca do comportamento observado.** Se o sistema diverge, o teste falha e o caso vira defeito. O teste não é ajustado para passar.
2. **Toda RN cita a fonte** (`sut/docs/api.md`, ADR-0xx, PDF do desafio §2.2.2). Regras sem fonte não entram.
3. **Ambiguidades viram Q-xx**, com a interpretação adotada explícita. **Suspeitas levantadas por leitura de código viram H-xx** e só se tornam Issue depois de reproduzidas por teste.
4. **A IA participa de cada etapa com saída validada por humano.** Prompt, decisão e validação ficam registrados em `docs/ai-usage.md`.
5. A **matriz de rastreabilidade** (`docs/traceability-matrix.md`) fecha o ciclo: toda RN tem ao menos um caso, e todo caso automatizado tem o ID no nome do teste.

## Consequências

- **Positivas:**
  - Cobertura de requisitos mensurável (RN coberta/total).
  - Cada defeito tem uma trilha auditável até a documentação.
  - A IA acelera a especificação sem virar autoridade sobre o "esperado".
- **Negativas:**
  - Custo inicial maior de especificação antes de automatizar. Ele se paga ao evitar retrabalho e testes que validam o próprio bug.
  - Os IDs precisam ser mantidos em sincronia, o que é verificado na revisão de PR (critério do guia de governança).
