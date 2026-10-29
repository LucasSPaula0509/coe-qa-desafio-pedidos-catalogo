## Objetivo

<!-- O quê e por quê. Qual parte do desafio este PR entrega? -->

## Rastreabilidade

- Regras (RN): <!-- ex.: RN-10, RN-11 -->
- Casos de teste (CT): <!-- ex.: CT-ORD-020..026 -->
- Issues relacionadas: <!-- ex.: #12 -->

## Como validar

```bash
# comandos para reproduzir localmente
```

## Evidências

<!-- Resultado da execução (resumo, link do relatório/artefato do CI). -->

## Uso de IA

<!-- Ferramenta, objetivo do prompt, o que foi aceito/rejeitado e como a saída foi validada. Registrar também em docs/ai-usage.md. -->

## Checklist

- [ ] `sut/` não foi alterado (`scripts/verify-sut-integrity.sh` verde)
- [ ] O resultado esperado dos testes vem da especificação (RN), não do comportamento observado
- [ ] IDs `CT-*` no nome dos testes e matriz de rastreabilidade atualizada
- [ ] Sem `sleep`/espera fixa; testes independentes e idempotentes (setup/teardown próprios)
- [ ] Sem segredos ou dados pessoais reais no código e nos relatórios
- [ ] Documentação (README da suíte, estratégia/plano) atualizada quando aplicável
