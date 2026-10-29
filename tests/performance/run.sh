#!/usr/bin/env bash
# Executa um cenário k6 em container, na rede do Docker Compose do SUT (local e CI iguais).
#
# Uso: tests/performance/run.sh <cenário> [-e VAR=valor ...]
#   cenários: smoke | load | stress | spike | soak | items-scaling | dependency-degradation | volume
#   ex.:      tests/performance/run.sh load -e HOLD=3m
#
# Resultados: tests/performance/results/<cenário>-summary.{json,txt}. Código de saída ≠ 0 quando
# algum threshold é violado (quality gate).

set -euo pipefail

scenario="${1:?Informe o cenário (smoke, load, stress, spike, soak, items-scaling, dependency-degradation, volume)}"
shift

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
script="${here}/scenarios/${scenario}.js"
[[ -f "${script}" ]] || { echo "Cenário inexistente: ${scenario}" >&2; exit 2; }

mkdir -p "${here}/results"
image="${K6_IMAGE:-grafana/k6:0.54.0}"
network="${SUT_NETWORK:-sut_default}"

# No Git Bash (Windows), o Docker precisa do caminho no formato do Windows e sem conversão MSYS.
mount="${here}"
if command -v cygpath >/dev/null 2>&1; then
  mount="$(cygpath -w "${here}")"
  export MSYS_NO_PATHCONV=1
fi

docker run --rm \
  --network "${network}" \
  -v "${mount}:/scripts:ro" \
  -v "${mount}/results:/results" \
  "${image}" run "/scripts/scenarios/${scenario}.js" "$@"
