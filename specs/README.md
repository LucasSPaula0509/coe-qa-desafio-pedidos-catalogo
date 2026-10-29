# Especificação de Teste (SDD)

Esta pasta é a **fonte da verdade sobre o comportamento esperado** do sistema sob teste. Ela consolida a base de teste oficial (`sut/docs/` e o PDF do desafio) em regras testáveis. Os casos de teste e a automação derivam daqui (ADR-QA-005).

## Fluxo

```
Base oficial ──► Regras (RN) ──► Casos (CT) ──► Automação ──► Evidência / Defeito
sut/docs/, PDF   specs/          docs/test-cases/ tests/         relatórios, Issues
                 Q/H em aberto
```

| Etapa | Entrada | Saída | Critério de pronto |
|---|---|---|---|
| 1. Consolidação | `sut/docs/`, PDF §2.2 | [business-rules.md](business-rules.md) | Toda RN cita a fonte. Nenhuma RN vem só do código. |
| 2. Análise crítica | RNs + leitura do código | [open-questions.md](open-questions.md) | Ambiguidades (Q) com a interpretação adotada. Suspeitas (H) com referência de código e o CT que as confirma. |
| 3. Projeto de casos | RNs, Qs | [docs/test-cases/](../docs/test-cases/README.md) | Todo CT referencia ao menos uma RN e declara a técnica, a prioridade e o nível de automação. |
| 4. Rastreabilidade | CTs | [docs/traceability-matrix.md](../docs/traceability-matrix.md) | Toda RN tem ao menos um CT. A matriz é gerada por script, não à mão. |
| 5. Automação | CTs | `tests/**` | O ID do CT está no nome do teste. O esperado vem do CT, nunca do comportamento observado. |
| 6. Evidência | Execução | Relatórios, Issues | Todo defeito aponta para CT → RN → fonte. |

## Convenções de ID

| Prefixo | Significado | Onde |
|---|---|---|
| `RN-nn` | Regra testável (funcional) | [business-rules.md](business-rules.md) |
| `NF-nn` | Requisito não funcional testável (performance, segurança, acessibilidade) | [business-rules.md](business-rules.md) |
| `Q-nn` | Ambiguidade ou lacuna da documentação, com a interpretação adotada | [open-questions.md](open-questions.md) |
| `H-nn` | Suspeita de defeito ainda **não confirmada** | [open-questions.md](open-questions.md) |
| `CT-<ÁREA>-nnn` | Caso de teste | `docs/test-cases/` |

Os IDs `RF-xx` e `RNF-xx` pertencem ao SUT (`sut/docs/requirements.md`) e são citados como fonte. Eles não são reutilizados aqui.

## Abreviações de fonte

| Abreviação | Documento |
|---|---|
| PDF §x | Desafio técnico COE Leader QA (documento do candidato), seção x |
| api | `sut/docs/api.md` |
| arch | `sut/docs/architecture.md` |
| db | `sut/docs/database.md` |
| req | `sut/docs/requirements.md` |
| fe-arch | `sut/docs/frontend-architecture.md` |
| fe-int | `sut/docs/frontend-api-integration.md` |
| i18n | `sut/docs/i18n.md` |
| ADR-0nn | `sut/docs/adr/ADR-0nn-*.md` (ADRs do SUT) |
| seed | `sut/docker/mongo-init.js` e migração Flyway `V2__seed_discount_rule.sql` (massa inicial do ambiente) |

## Manutenção

Mudança na documentação do SUT → atualizar a RN → revisar os CTs afetados (a matriz mostra quais) → ajustar a automação. Uma RN alterada ou removida nunca é "corrigida" no teste para refletir o comportamento observado sem antes passar por uma Q registrada e decidida.
