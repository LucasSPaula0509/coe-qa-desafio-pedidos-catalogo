# Guia de Contribuição

Convenções de trabalho do projeto de qualidade. Os critérios de bloqueio de PR estão no guia de governança de qualidade (entregável da Parte diferenciais).

## Regra de ouro

**O diretório `sut/` não é alterado.** Divergências entre o comportamento do sistema e a documentação viram Issue de defeito, nunca correção no SUT. O gate `scripts/verify-sut-integrity.sh` reprova qualquer PR que toque em `sut/` (ADR-QA-001).

## GitFlow

| Branch | Origem | Destino | Uso |
|---|---|---|---|
| `main` | — | — | Versões entregues. Só recebe merge de `release/*` ou `hotfix/*`. |
| `develop` | `main` | — | Integração contínua das features. |
| `feature/<escopo>` | `develop` | `develop` via PR | Uma parte ou incremento do desafio (ex.: `feature/parte-2-testes-api`). |
| `release/<versão>` | `develop` | `main` + `develop` | Fechamento de entrega: relatório, resumo de qualidade e tag. |
| `hotfix/<escopo>` | `main` | `main` + `develop` | Correção urgente em versão entregue. |

- Merge de PR com **merge commit** (`--no-ff`), para preservar a história das features.
- As branches `main` e `develop` são protegidas: PR obrigatório, esteira verde e ao menos uma aprovação.
- Tags semânticas na `main` (`v1.0.0`).

## Commits

[Conventional Commits](https://www.conventionalcommits.org/pt-br/), em português, no imperativo:

```
<tipo>(<escopo opcional>): <resumo em até 72 caracteres>

<corpo: o quê e por quê, não o como>
```

| Tipo | Uso |
|---|---|
| `test` | Casos de teste automatizados (novos ou alterados) |
| `docs` | Estratégia, plano, casos, specs, ADRs, README |
| `ci` | Workflows e quality gates |
| `build` | Dependências, scripts de build e ferramentas |
| `fix` | Correção na automação (nunca no SUT) |
| `refactor` | Melhoria interna da automação sem mudar cobertura |
| `chore` | Manutenção (ex.: `chore(sut): ...` para atualizar o SUT com novo manifesto) |

Commits de teste citam os IDs cobertos (ex.: `test(api): cobre desconto por regra ativa (CT-ORD-020..026)`).

## Pull requests

- Um PR por incremento coeso, idealmente com menos de 400 linhas alteradas fora de `docs/`.
- A descrição segue o template (`.github/pull_request_template.md`): objetivo, IDs cobertos, evidências de execução e uso de IA.
- O PR que adiciona automação também atualiza a matriz de rastreabilidade.

## Defeitos

Registrados como Issue pelo template **Defeito** (`.github/ISSUE_TEMPLATE/defect.yml`), com passos reproduzíveis, esperado vs. obtido citando a fonte da documentação, `X-Request-Id` e evidência. Antes de registrar, o defeito precisa estar **reproduzido por um caso de teste** (ADR-QA-005).
