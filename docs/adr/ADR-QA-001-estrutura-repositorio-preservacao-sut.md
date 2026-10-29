# ADR-QA-001: Repositório único com o SUT isolado e verificado por hash

**Status:** Aceita

## Contexto

O desafio exige entregar, em um repositório GitHub, as suítes de teste (API, front-end, performance), a esteira de CI/CD e a documentação. A restrição central é que **o código do sistema sob teste (SUT) não pode ser alterado**: divergências são reportadas como defeitos, nunca corrigidas.

O SUT foi recebido como um repositório completo, com README, `docs/`, Docker Compose e CI próprios. Há três formas de organizar a entrega:

1. **Repositório separado só de testes**, baixando o SUT no CI (clone de URL externa ou artefato).
2. **Testes misturados na raiz do SUT** (o README e o `.github/` do SUT seriam substituídos ou editados).
3. **Repositório único com o SUT isolado em `sut/`** e as suítes em diretórios próprios.

## Decisão

Opção 3. O SUT fica em `sut/`, importado byte a byte em um commit isolado na `main` (`chore(sut): importa o sistema sob teste sem alterações`). A integridade é **provada, não declarada**:

- `sut.sha256` registra o SHA-256 dos 219 arquivos recebidos.
- `scripts/verify-sut-integrity.sh` falha se algum arquivo for alterado, removido ou adicionado. Ele roda localmente e como **primeiro gate** do CI.
- `.gitattributes` desativa a normalização de fim de linha em `sut/**` (`-text`). Sem isso, máquinas Windows com `core.autocrlf=true` alterariam os bytes.

As suítes ficam em `tests/<tipo>/`, e a documentação do projeto de qualidade em `docs/` e `specs/` na raiz. A documentação do SUT (`sut/docs/`) permanece intacta e é a **base de teste oficial**.

## Consequências

- **Positivas:**
  - Um único `git clone` reproduz SUT + testes + esteira, sem dependência de URL externa no CI.
  - O avaliador verifica a restrição "SUT não alterado" com um comando.
  - O CI original do SUT (`sut/.github/`) fica preservado e inativo: o GitHub só executa workflows da raiz. Assim não há conflito com a esteira de qualidade.
- **Negativas:**
  - Atualizar o SUT para uma nova versão exige regenerar o manifesto em um commit dedicado e rastreável (`chore(sut): atualiza SUT para <versão>`).
  - Os caminhos da documentação do SUT ganham o prefixo `sut/` (ex.: `sut/docs/api.md`).
