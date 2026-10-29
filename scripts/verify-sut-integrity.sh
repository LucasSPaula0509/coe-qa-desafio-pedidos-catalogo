#!/usr/bin/env bash
# Verifica que o sistema sob teste (sut/) não foi alterado.
#
# Compara os arquivos versionados em sut/ com o manifesto sut.sha256, gerado
# no recebimento do SUT (ADR-QA-001). Falha se algum arquivo foi modificado,
# removido ou adicionado. Usado localmente e como primeiro quality gate do CI.
#
# Uso: scripts/verify-sut-integrity.sh

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
manifest="${repo_root}/sut.sha256"

cd "${repo_root}"

if [[ ! -f "${manifest}" ]]; then
  echo "ERRO: manifesto não encontrado: ${manifest}" >&2
  exit 2
fi

# 1. Conteúdo: todo arquivo do manifesto existe e tem o hash esperado.
if ! sha256sum --check --quiet --strict "${manifest}"; then
  echo "ERRO: arquivos do SUT divergem do manifesto (alterados ou removidos)." >&2
  exit 1
fi

# 2. Inventário: nenhum arquivo foi adicionado ao SUT.
#    Usa os arquivos versionados (git ls-files) para ignorar artefatos de build
#    locais (target/, node_modules/, dist/) que não fazem parte do SUT.
expected="$(cut -c67- "${manifest}" | LC_ALL=C sort)"
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  actual="$(git ls-files sut | LC_ALL=C sort)"
else
  actual="$(find sut -type f | LC_ALL=C sort)"
fi

added="$(LC_ALL=C comm -13 <(echo "${expected}") <(echo "${actual}"))"
if [[ -n "${added}" ]]; then
  echo "ERRO: arquivos adicionados ao SUT:" >&2
  echo "${added}" >&2
  exit 1
fi

echo "OK: SUT íntegro — $(echo "${expected}" | wc -l | tr -d ' ') arquivos conferem com sut.sha256."
