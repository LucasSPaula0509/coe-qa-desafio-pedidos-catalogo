# Uso de IA no Desenvolvimento

**Ferramenta utilizada:** CLI agêntica de AI Coding (opencode) conduzindo o ciclo completo — especificação, codificação, testes e depuração — com revisão e validação humanas em cada etapa.

**Motivo da escolha:** a vaga exige evidência de uso eficaz de IA ao longo de todo o ciclo de desenvolvimento. Uma CLI agêntica foi preferida a um copiloto de autocomplete porque o desafio pedia artefatos completos (dois serviços, testes, CI, documentação e ADRs), e o fluxo agêntico permite manter o contexto do projeto inteiro, executar builds/testes e iterar sobre os resultados — aproximando-se do fluxo real de trabalho de um COE Leader que orquestra IA em vez de apenas consumir sugestões.

**Link do chat/conversa com a IA:** `<INSERIR_LINK_DO_CHAT_AQUI>`

## Metodologia: Spec-Driven Development (SDD)

O desenvolvimento seguiu SDD: antes de qualquer código, a IA e o candidato produziram uma **especificação executável em 14 fases** (levantamento de requisitos, decisões de arquitetura, contratos de API, schema de dados, casos de teste e critérios de aceite), e a implementação foi conduzida fase a fase contra essa especificação. Cada fase só foi considerada concluída após validação objetiva (build + testes), e desvios da spec viraram ADRs.

## Log de utilização por etapa

| Etapa | Prompt (objetivo) | Decisão humana | Validação aplicada |
|---|---|---|---|
| 1. Levantamento de requisitos | "Analise o desafio e produza o levantamento com 21 seções" | Aprovação do escopo; definição da stack (Java + Spring Boot, Angular, PostgreSQL) sobrepõe o template Node do briefing | Revisão seção a seção contra o documento do desafio |
| 2. Arquitetura | Especificação da Clean Architecture, portas/adaptadores, integração em lote | Aceite do compromisso pragmático (@Transactional na aplicação) | ADRs 001-015 redigidos e revisados |
| 3. Contratos de API | Especificação dos endpoints, códigos de erro e envelope | Escolha do envelope `{error, requestId}` e semântica 422 para regras de negócio | Contratos conferidos na implementação e nos ITs |
| 4. Catalog Service | Implementação do serviço completo com testes | Revisão do modelo de vigência de preços | `mvn spotless:check test` — 14/14 testes verdes |
| 5. Order Service | Implementação do serviço completo com testes | Revisão da regra de desconto e do snapshot de preço | `mvn spotless:check test` — 25/25 testes verdes |
| 6. Depuração | Correção de erros de compilação/testes apontados pelo build (imports ausentes, stub de mock retornando null, API do Jackson) | Conferência de cada correção antes do próximo build | Builds reexecutados até BUILD SUCCESS |
| 7. Mensagens de erro | IA sugeriu códigos/mensagens e o mapeamento HTTP | Ajuste de semântica (404 x 422 x 5xx) e inclusão do `requestId` | Tabela de erros validada nos ITs de API |
| 8. Governança e dimensionamento | Redação do guia de code review e da estimativa | Revisão de realismo das premissas e dos critérios bloqueadores | Leitura crítica contra as práticas do desafio |

## Uso responsável e validação das saídas

- **Nada foi aceito sem validação executável:** todo código gerado por IA passou por compilação, testes automatizados e revisão humana; nenhum artefato foi mergeado "por confiança".
- **Erros da IA foram detectados pelo pipeline:** a IA cometeu erros típicos (imports ausentes, assinatura de API inexistente, mock não stubado) que foram capturados pelo build/testes e corrigidos no próprio ciclo — evidência de que o gate automatizado é indispensável quando IA participa do desenvolvimento.
- **Decisões de negócio permaneceram humanas:** stack, escopo, semântica de erros, regra de desconto e premissas da estimativa foram definidas/validadas pelo candidato.
- **Rastreabilidade:** cada decisão relevante virou um ADR; prompts e validações estão registrados na tabela acima.
